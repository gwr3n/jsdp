/**
 * jsdp: A Java Stochastic Dynamic Programming Library
 * 
 * MIT License
 * 
 * Copyright (c) 2016 Roberto Rossi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jsdp.app.lotsizing;

import java.util.Collections;
import java.util.Enumeration;

import org.apache.logging.log4j.Logger;

import jsdp.sdp.Action;
import umontreal.ssj.probdist.Distribution;

import org.apache.logging.log4j.LogManager;

/**
 * A special purpose implementation of a backward recursion algorithm that exploits K-convexity 
 * to quickly process states.
 * 
 * @Deprecated
 * Sample-based approaches are more efficient.
 *  
 * @author Roberto Rossi
 *
 */
@Deprecated
public class sS_SequentialBackwardRecursion extends sS_BackwardRecursion {
   static final Logger logger = LogManager.getLogger(sS_BackwardRecursion.class.getName());

   /**
    * Creates an instance of a backward recursion algorithm that exploits K-convexity to quickly process states.
    * 
    * @param demand the distribution of random demand in each period, an array of {@code Distribution}.
    * @param fixedOrderingCost the fixed ordering cost.
    * @param proportionalOrderingCost the proportional (per unit) ordering cost.
    * @param holdingCost the proportional (per unit) holding cost; this is paid for each item brought from one period to the next.
    * @param penaltyCost the proportional (per unit) penalty cost; this is paid for each item short at the end of each period.
    */
   public sS_SequentialBackwardRecursion(Distribution[] demand,
                                         double fixedOrderingCost, 
                                         double proportionalOrderingCost, 
                                         double holdingCost,
                                         double penaltyCost){
      super(demand,
            fixedOrderingCost,
            proportionalOrderingCost,
            holdingCost,
            penaltyCost,
            sS_StateSpaceSampleIterator.SamplingScheme.NONE,
            Integer.MAX_VALUE);
   }

   @Override
   protected void recurse(int period){
      /**
       * Initially the algorithm proceeds as a classic backward recursion procedure,
       * by processing states sequentially in reverse order (largest to smalles).
       */
      for(int i = sS_State.getMaxIntState(); i >= sS_State.getMinIntState(); i--){
         sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period, i);
         sS_State state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
         Action bestAction = null;
         double bestCost = Double.MAX_VALUE;
         Enumeration<Action> actions = Collections.enumeration(state.getFeasibleActions()); 
         while(actions.hasMoreElements()){
            Action currentAction = actions.nextElement();
            double currentCost = this.getValueRepository().getExpectedValue(state, currentAction, this.getTransitionProbability());
            if(currentCost < bestCost){
               bestCost = currentCost;
               bestAction = currentAction;
            }
         }
         this.getValueRepository().setOptimalExpectedValue(state, bestCost);
         this.getValueRepository().setOptimalAction(state, bestAction);
         logger.trace("Period["+period+"]\tInventory: "+i+"\tOrder: "+sS_Action.actionToOrderQuantity(((sS_Action)bestAction).intAction)+"\tCost: "+bestCost);

         /**
          * As soon as we find a state for which it is optimal to place an order,
          * we exploit K-convexity to set the value of the optimal order quantity 
          * and associated expected total cost for all remaining states.
          */
         if(((sS_Action)bestAction).getIntAction() > 0){
            int initialAction = ((sS_Action)bestAction).getIntAction();
            while(--i >= sS_State.getMinIntState()){
               stateDescriptor = new sS_StateDescriptor(period, i);
               state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
               double orderingCostIncrement = sS_Action.actionToOrderQuantity(((sS_Action)bestAction).intAction+1-initialAction)*this.proportionalOrderingCost;
               this.getValueRepository().setOptimalExpectedValue(state, bestCost+orderingCostIncrement);
               bestAction = new sS_Action(state, ((sS_Action)bestAction).intAction+1); 
               this.getValueRepository().setOptimalAction(state, bestAction);
               logger.trace("Period["+period+"]\tInventory: "+i+"\tOrder: "+sS_Action.actionToOrderQuantity(((sS_Action)bestAction).intAction)+"\tCost: "+bestCost);
            }
         }
      }
   }
}

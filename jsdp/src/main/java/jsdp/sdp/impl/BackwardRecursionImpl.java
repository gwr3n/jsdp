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

package jsdp.sdp.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.BackwardRecursion;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.ValueRepository;
import umontreal.ssj.probdist.Distribution;

/**
 * A concrete implementation of a backward recursion procedure to compute (s,S) policy parameters.
 * 
 * @author Roberto Rossi
 *
 */
public class BackwardRecursionImpl extends BackwardRecursion{
   
   double fixedOrderingCost; 
   double proportionalOrderingCost; 
   double holdingCost;
   double penaltyCost;
   Distribution[] demand;
   
   /**
    * Creates an instance of the problem and initialises state space, transition probability and value repository.
    * 
    * @param demand the distribution of random demand in each period, an array of {@code Distribution}.
    * @param fixedOrderingCost the fixed ordering cost.
    * @param proportionalOrderingCost the proportional (per unit) ordering cost.
    * @param holdingCost the proportional (per unit) holding cost; this is paid for each item brought from one period to the next.
    * @param penaltyCost the proportional (per unit) penalty cost; this is paid for each item short at the end of each period.
    */
   public BackwardRecursionImpl(Distribution[] demand,
                        ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                        RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction,
                        Function<State, ArrayList<Action>> buildActionList,
                        Function<State, Action> idempotentAction,
                        SamplingScheme samplingScheme,
                        int maxSampleSize){
      super(OptimisationDirection.MIN);
      this.demand = demand;
      this.horizonLength = demand.length;
      
      this.stateSpace = new StateSpaceImpl[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new StateSpaceImpl(i, buildActionList, idempotentAction, samplingScheme, maxSampleSize);
      this.transitionProbability = new TransitionProbabilityImpl(
            demand,randomOutcomeFunction,(StateSpaceImpl[])this.getStateSpace(),StateImpl.getStepSize());
      this.valueRepository = new ValueRepository(immediateValueFunction);
   }
   
   @Override
   public TransitionProbabilityImpl getTransitionProbability(){
      return (TransitionProbabilityImpl) this.transitionProbability; 
   }
   
   public double[][] getOptimalPolicy(double initialInventory){
      double[][] optimalPolicy = new double[2][];
      double[] S = new double[demand.length];
      double[] s = new double[demand.length];
      for(int i = 0; i < demand.length; i++){
         if(i == 0) {
            StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(0, StateImpl.stateToIntState(initialInventory));
            s[i] = StateImpl.intStateToState(this.find_s(i).getInitialIntState());
            S[i] = ActionImpl.intActionToAction(this.getOptimalAction(stateDescriptor).getIntAction())+initialInventory;
         }
         else{
            s[i] = StateImpl.intStateToState(this.find_s(i).getInitialIntState());
            S[i] = StateImpl.intStateToState(this.find_S(i).getInitialIntState());
         }
      }
      optimalPolicy[0] = s;
      optimalPolicy[1] = S;
      return optimalPolicy;
   }
   
   public double getExpectedCost(double initialInventory){
      StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(0, StateImpl.stateToIntState(initialInventory));
      return getExpectedCost(stateDescriptor);
   }
   
   public double getExpectedCost(StateDescriptorImpl stateDescriptor){
      State state = ((StateSpaceImpl)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return getExpectedValue(state);
   }
   
   public ActionImpl getOptimalAction(StateDescriptorImpl stateDescriptor){
      State state = ((StateSpaceImpl)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return (ActionImpl) this.getValueRepository().getOptimalAction(state);
   }

   public StateImpl find_S(int period){
      StateImpl s = this.find_s(period);
      int i = ((ActionImpl)this.getValueRepository().getOptimalAction(s)).getIntAction()+s.getInitialIntState();
      StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(period, i);
      StateImpl state = (StateImpl) ((StateSpaceImpl)this.getStateSpace()[period]).getState(stateDescriptor);
      return state;
   }
   
   public StateImpl find_s(int period){
      Iterator<State> iterator = this.getStateSpace()[period].iterator();
      StateImpl state = null;
      do{
         state = (StateImpl) iterator.next();
         Action action = this.getValueRepository().getOptimalAction(state);
         if(action == null)
            continue;
         if(((ActionImpl)action).getIntAction() > 0){
            return state;
         }
      }while(iterator.hasNext());
      return state;
   }
}

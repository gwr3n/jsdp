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

package jsdp.app.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;

import jsdp.sdp.Action;
import jsdp.sdp.ActionIterator;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.impl.multidimensional.*;
import umontreal.ssj.probdistmulti.DiscreteDistributionIntMulti;
import umontreal.ssj.probdistmulti.SafeMultinomialDist;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.probdist.NormalDist;

/**
 *  We formulate a multi item stochastic lot sizing problem under multinomial demand  
 *  
 *  We use backward recursion and sampling to find optimal policies.
 *  
 * @author Roberto Rossi
 *
 */
public class MultiItemStochasticLotSizing {
   
   public static void main(String args[]){
      
      /*******************************************************************
       * Problem parameters
       */
      double fixedOrderingCost = 30; 
      double proportionalOrderingCost = 0; 
      double holdingCost = 1;
      double penaltyCost = 4;
      
      double[][] p = {{0.3,0.7},{0.3,0.7},{0.3,0.7},{0.3,0.7},{0.3,0.7}};
      int N[] = {5,5,5,5,5};
      
      int horizonLength = 5;

      
      // Random variables

      DiscreteDistributionIntMulti[] distributions = IntStream.iterate(0, i -> i + 1)
                                                              .limit(horizonLength)
                                                              .mapToObj(i -> new SafeMultinomialDist(N[i], p[i]))
                                                              .toArray(DiscreteDistributionIntMulti[]::new);
      
      double[] initialInventory = {0,0};
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      double stepSize[] = {1,1};       //Stepsize must be 1 for discrete distributions
      int minIntState[] = {-5,-5};
      int maxIntState[] = {20,20};
      StateImpl.setStateBoundaries(stepSize, minIntState, maxIntState);

      // Actions
      
      boolean sampleActions = true;
      int maxSampledAction = 500;
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         ActionIterator actionIterator;
         if(sampleActions)
            actionIterator = new ActionSampleIteratorImpl(state, maxSampledAction);
         else
            actionIterator = new ActionIteratorImpl(state);
         while(actionIterator.hasNext()){
            Action a = actionIterator.next();
            feasibleActions.add(a);
           
         }
         return feasibleActions;
      };
      
      Function<State, Action> idempotentAction = s -> new ActionImpl(s, new double[StateImpl.getStateDimension()]);
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         ActionImpl a = (ActionImpl)action;
         StateImpl fs = (StateImpl)finalState;
         double orderingCost = 
               Arrays.stream(a.getIntAction()).sum() > 0 ? (fixedOrderingCost + Arrays.stream(a.getIntAction()).sum()*proportionalOrderingCost) : 0;
         double holdingAndPenaltyCost =   
               holdingCost*Arrays.stream(fs.getInitialIntState()).map(s -> Math.max(s,0)).sum() + 
               penaltyCost*Arrays.stream(fs.getInitialIntState()).map(s -> Math.max(-s,0)).sum();
         return orderingCost+holdingAndPenaltyCost;
      };
      
      // Random Outcome Function
      
      RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction = (initialState, action, finalState) -> {
         double realizedDemand[] = new double[StateImpl.getStateDimension()];
         for(int i = 0; i < realizedDemand.length; i++){
            realizedDemand[i] = ((StateImpl)initialState).getInitialState()[i] +
                                ((ActionImpl)action).getAction()[i] -
                                ((StateImpl)finalState).getInitialState()[i];
         }
         //System.out.println(Arrays.toString(realizedDemand) + " " + action + " " + finalState);
         return realizedDemand;
      };
      
      /*******************************************************************
       * Solve
       */
      
      // Sampling scheme
      
      SamplingScheme samplingScheme = SamplingScheme.JENSENS_PARTITIONING;
      int maxSampleSize = 500;
      
      
      // Value Function Processing Method: backward recursion
      
      BackwardRecursionImpl recursion = new BackwardRecursionImpl(distributions,
                                                                  immediateValueFunction,
                                                                  randomOutcomeFunction,
                                                                  buildActionList,
                                                                  idempotentAction,
                                                                  samplingScheme,
                                                                  maxSampleSize);

      System.out.println("--------------Backward recursion--------------");
      StopWatch timer = new StopWatch();
      timer.start();
      recursion.runBackwardRecursion();
      timer.stop();
      System.out.println();
      double ETC = recursion.getExpectedCost(initialInventory);
      StateDescriptorImpl initialState = new StateDescriptorImpl(0, StateImpl.stateToIntState(initialInventory));
      double[] action = StateImpl.intStateToState(recursion.getOptimalAction(initialState).getIntAction());
      System.out.println("Expected total cost (assuming an initial inventory level "+Arrays.toString(initialInventory)+"): "+ETC);
      System.out.println("Optimal initial action: "+Arrays.toString(action));
      System.out.println("Time elapsed: "+timer);
      System.out.println();
      
   }
}
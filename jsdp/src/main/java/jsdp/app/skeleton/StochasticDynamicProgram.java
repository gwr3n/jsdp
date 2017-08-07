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

package jsdp.app.skeleton;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.IntStream;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.Recursion.OptimisationDirection;
import jsdp.sdp.impl.univariate.ActionImpl;
import jsdp.sdp.impl.univariate.BackwardRecursionImpl;
import jsdp.sdp.impl.univariate.SamplingScheme;
import jsdp.sdp.impl.univariate.StateDescriptorImpl;
import jsdp.sdp.impl.univariate.StateImpl;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;

/**
 * This is the generic skeleton of a stochastic dynamic program implemented via jsdp
 * 
 * @author Roberto Rossi
 *
 */

public class StochasticDynamicProgram {
   public static void main(String args[]){
      
      /*******************************************************************
       * Problem parameters
       */
      
      // Problem parameters
      
      @SuppressWarnings("unused")
      double param_1;
      @SuppressWarnings("unused")
      double param_2;
      
      double initialStateParameter = 0; // Replace 0 with the initial state of the system
      
      int randomVariables = 10;  //Number of random variables                                           
      
      double truncationQuantile = 0.95;
      
      // Random variables
      
      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(randomVariables)
                                              .mapToObj(i -> new PoissonDist(i)) // Replace PoissonDist(i) with the relevant distribution
                                              .toArray(Distribution[]::new);
      
      double[] supportLB = IntStream.iterate(0, i -> i + 1)
                                    .limit(distributions.length)
                                    .mapToDouble(i -> distributions[i].inverseF(1-truncationQuantile))
                                    .toArray();
      
      double[] supportUB = IntStream.iterate(0, i -> i + 1)
                                    .limit(distributions.length)
                                    .mapToDouble(i -> distributions[i].inverseF(truncationQuantile))
                                    .toArray();
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      double stepSize = 1;       //Stepsize must be 1 for discrete distributions
      double minState = -50;     //state lower bound in each period
      double maxState = 150;     //state upper bound in each period
      StateImpl.setStateBoundaries(stepSize, minState, maxState);
      
      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(double i = state.getInitialState(); 
             i <= StateImpl.getMaxState(); 
             i += StateImpl.getStepSize()){
            feasibleActions.add(new ActionImpl(state, 0 /* replace 0 with relevant code to compute feasible actions given current state */));
         }
         return feasibleActions;
      };
      
      Function<State, Action> idempotentAction = s -> new ActionImpl(s, 0.0);
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         @SuppressWarnings("unused")
         ActionImpl a = (ActionImpl)action;
         @SuppressWarnings("unused")
         StateImpl fs = (StateImpl)finalState;
         return 0.0 /* replace 0.0 with immediate cost computation */;
      };
      
      // Random Outcome Function
      
      RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction = (initialState, action, finalState) -> {
         double randomOutcome = 0.0 /* compute randomOutcome as a function of initialState, action, and finalState */;
         return randomOutcome;
      };
      
      /*******************************************************************
       * Solve
       */
      
      // Sampling scheme
      
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int maxSampleSize = 100;
      double reductionFactorPerStage = 1;
      
      
      // Value Function Processing Method: backward recursion
      
      // Note that to implement a forward recursion approach one cannot rely on 
      // package jsdp.sdp.impl and must necessarily develop dedicated concrete
      // implementations of abstract classes in jsdp.sdp
      
      double discountFactor = 1.0;
      int stateSpaceLowerBound = 10000000;
      float loadFactor = 0.8F;
      BackwardRecursionImpl recursion = new BackwardRecursionImpl(OptimisationDirection.MIN,
                                                                  distributions,
                                                                  supportLB,
                                                                  supportUB,
                                                                  immediateValueFunction,
                                                                  randomOutcomeFunction,
                                                                  buildActionList,
                                                                  idempotentAction,
                                                                  discountFactor,
                                                                  samplingScheme,
                                                                  maxSampleSize,
                                                                  reductionFactorPerStage,
                                                                  stateSpaceLowerBound,
                                                                  loadFactor,
                                                                  HashType.THASHMAP);

      
      System.out.println("--------------Backward recursion--------------");
      recursion.runBackwardRecursionMonitoring();
      System.out.println();
      double ETV = recursion.getExpectedCost(initialStateParameter);
      StateDescriptorImpl initialState = new StateDescriptorImpl(0, initialStateParameter);
      double action = recursion.getOptimalAction(initialState).getAction();
      long percent = recursion.getMonitoringInterfaceBackward().getPercentCPU();
      System.out.println("Expected total value (assuming initial state "+initialStateParameter+"): "+ETV);
      System.out.println("Optimal initial action: "+action);
      System.out.println("Time elapsed: "+recursion.getMonitoringInterfaceBackward().getTime());
      System.out.println("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      System.out.println();

   }
}

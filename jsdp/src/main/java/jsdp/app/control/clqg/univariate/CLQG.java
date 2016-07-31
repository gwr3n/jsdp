/*
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

package jsdp.app.control.clqg.univariate;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;

import com.sun.management.OperatingSystemMXBean;

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
import umontreal.ssj.probdist.NormalDist;

public class CLQG {
   
   /**
    *  We formulate the Constrained Linear Quadratic Gaussian (CLQG) 
    *  Control Problem as a stochastic dynamic programming problem. 
    *  
    *  We use backward recursion and sampling to find optimal policies.
    *  
    * @author Roberto Rossi
    *
    */
   
   public static void main(String args[]){
      
      /*******************************************************************
       * Problem parameters
       */
      
      int T = 20;                         // Horizon length
      double G = 1;                       // Input transition
      double Phi = 1;                     // State transition
      double R = 1;                       // Input cost
      double Q = 1;                       // State cost
      double Ulb = -1;                    // Action constraint
      double Uub = 20;                    // Action constraint
      double noiseStd = 5;                // Standard deviation of the noise
      
      double[] noiseStdArray = new double[T];
      Arrays.fill(noiseStdArray, noiseStd);
      double truncationQuantile = 0.975;
            
      // Random variables

      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(noiseStdArray.length)
                                              .mapToObj(i -> new NormalDist(0, noiseStdArray[i]))
                                              
                                              .toArray(Distribution[]::new);
      double[] supportLB = IntStream.iterate(0, i -> i + 1)
                                    .limit(T)
                                    .mapToDouble(i -> NormalDist.inverseF(0, noiseStdArray[i], 1-truncationQuantile))
                                    .toArray();
      
      double[] supportUB = IntStream.iterate(0, i -> i + 1)
                                    .limit(T)
                                    .mapToDouble(i -> NormalDist.inverseF(0, noiseStdArray[i], truncationQuantile))
                                    .toArray();
      
      double initialX = 0;                  // Initial state
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      double stepSize = 0.5;       //Stepsize must be 1 for discrete distributions
      double minState = -25;
      double maxState = 100;
      StateImpl.setStateBoundaries(stepSize, minState, maxState);
      
      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = (Function<State, ArrayList<Action>> & Serializable) s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         double maxAction = Math.min(Uub, (StateImpl.getMaxState() - Phi*state.getInitialState())/G);
         double minAction = Math.max(Ulb, (StateImpl.getMinState() - Phi*state.getInitialState())/G);
         for(double actionPointer = minAction; actionPointer <= maxAction; actionPointer += StateImpl.getStepSize()){
            feasibleActions.add(new ActionImpl(state, actionPointer));
         }
         return feasibleActions;
      };
      
      Function<State, Action> idempotentAction = (Function<State, Action> & Serializable) s -> new ActionImpl(s, 0.0);
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         ActionImpl a = (ActionImpl)action;
         StateImpl fs = (StateImpl)finalState;
         double inputCost = Math.pow(a.getAction(),2)*R;
         double stateCost = Math.pow(fs.getInitialState(),2)*Q;
         return inputCost+stateCost;
      };
      
      // Random Outcome Function
      
      RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction = (initialState, action, finalState) -> {
         double realizedNoise =  ((StateImpl)finalState).getInitialState() -
                                 ((StateImpl)initialState).getInitialState()*Phi -
                                 ((ActionImpl)action).getAction()*G;
         return realizedNoise;
      };
      
      /*******************************************************************
       * Solve
       */
      
      // Sampling scheme
      
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int maxSampleSize = 20;
      
      
      // Value Function Processing Method: backward recursion
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
                                                                  stateSpaceLowerBound,
                                                                  loadFactor,
                                                                  HashType.HASHTABLE);

      
      System.out.println("--------------Backward recursion--------------");
      StopWatch timer = new StopWatch();
      OperatingSystemMXBean osMBean;
      try {
         osMBean = ManagementFactory.newPlatformMXBeanProxy(
               ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
         long nanoBefore = System.nanoTime();
         long cpuBefore = osMBean.getProcessCpuTime();
         
         timer.start();
         recursion.runBackwardRecursion();
         timer.stop();
         
         long cpuAfter = osMBean.getProcessCpuTime();
         long nanoAfter = System.nanoTime();
         
         long percent;
         if (nanoAfter > nanoBefore)
          percent = ((cpuAfter-cpuBefore)*100L)/
            (nanoAfter-nanoBefore);
         else percent = 0;

         System.out.println("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      System.out.println();
      double ETC = recursion.getExpectedCost(initialX);
      StateDescriptorImpl initialState = new StateDescriptorImpl(0, initialX);
      double action = recursion.getOptimalAction(initialState).getAction();
      System.out.println("Expected total cost (assuming an initial state "+initialX+"): "+ETC);
      System.out.println("Optimal initial action: "+action);
      System.out.println("Time elapsed: "+timer);
      System.out.println();
   }
}

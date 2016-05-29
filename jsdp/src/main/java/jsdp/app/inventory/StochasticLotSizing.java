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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;

import jsdp.app.inventory.simulation.SimulatePolicies;
import jsdp.sdp.Action;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;
import jsdp.sdp.impl.*;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.probdist.NormalDist;

/**
 *  We formulate the stochastic lot sizing problem as defined in  
 *  
 *  Herbert E. Scarf. Optimality of (s, S) policies in the dynamic inventory problem. In K. J. Arrow, S. Karlin, 
 *  and P. Suppes, editors, Mathematical Methods in the Social Sciences, pages 196–202. Stanford University
 *  Press, Stanford, CA, 1960.
 *  
 *  as a stochastic dynamic programming problem. 
 *  
 *  We use backward recursion and sampling to find optimal policies.
 *  
 * @author Roberto Rossi
 *
 */
public class StochasticLotSizing {
   
   public static void main(String args[]){
      
      boolean simulate = true;
      
      /*******************************************************************
       * Problem parameters
       */
      double fixedOrderingCost = 100; 
      double proportionalOrderingCost = 0; 
      double holdingCost = 1;
      double penaltyCost = 4;
      
      double[] meanDemand = {20,30,20,40,10,30,30,40,10,30,60};
      double coefficientOfVariation = 0.4;
      
      // Random variables

      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(meanDemand.length)
                                              .mapToObj(i -> new NormalDist(meanDemand[i],meanDemand[i]*coefficientOfVariation))
                                              //.mapToObj(i -> new PoissonDist(meanDemand[i]))
                                              .toArray(Distribution[]::new);
      
      double initialInventory = 0;
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      double stepSize = 0.8;       //Stepsize must be 1 for discrete distributions
      int minIntState = -100;
      int maxIntState = 500;
      StateImpl.setStateBoundaries(stepSize, minIntState, maxIntState);

      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(double i = state.getInitialState(); i <= StateImpl.getMaxState(); i+= StateImpl.getStepSize()){
            feasibleActions.add(new ActionImpl(state, i - state.getInitialState()));
         }
         return feasibleActions;
      };
      
      Function<State, Action> idempotentAction = s -> new ActionImpl(s, 0);
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         ActionImpl a = (ActionImpl)action;
         StateImpl fs = (StateImpl)finalState;
         double orderingCost = 
               a.getIntAction() > 0 ? (fixedOrderingCost + a.getAction()*proportionalOrderingCost) : 0;
         double holdingAndPenaltyCost =   
               holdingCost*Math.max(fs.getInitialState(),0) + penaltyCost*Math.max(-fs.getInitialState(),0);
         return orderingCost+holdingAndPenaltyCost;
      };
      
      // Random Outcome Function
      
      ImmediateValueFunction<State, Action, Double> randomOutcomeFunction = (initialState, action, finalState) -> {
         double realizedDemand = ((StateImpl)initialState).getInitialState() +
                                 ((ActionImpl)action).getAction() -
                                 ((StateImpl)finalState).getInitialState();
         return realizedDemand;
      };
      
      /*******************************************************************
       * Solve
       */
      
      // Sampling scheme
      
      SamplingScheme samplingScheme = SamplingScheme.JENSENS_PARTITIONING;
      int maxSampleSize = 40;
      
      
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
      double action = StateImpl.intStateToState(recursion.getOptimalAction(initialState).getIntAction());
      System.out.println("Expected total cost (assuming an initial inventory level "+initialInventory+"): "+ETC);
      System.out.println("Optimal initial action: "+action);
      System.out.println("Time elapsed: "+timer);
      
      /*******************************************************************
       * Simulate
       */
      
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.00",otherSymbols);
      
      if(!simulate) 
         return;
      
      double[][] optimalPolicy = recursion.getOptimalPolicy(initialInventory);
      double[] s = optimalPolicy[0];
      double[] S = optimalPolicy[1];      
      for(int i = 0; i < distributions.length; i++){
         System.out.println("S["+(i+1)+"]:"+df.format(S[i])+"\ts["+(i+1)+"]:"+df.format(s[i]));
      }

      /**
       * Simulation confidence level and error threshold
       */
      double confidence = 0.95;
      double errorTolerance = 0.001;
      
      double[] results = SimulatePolicies.simulate_sS(distributions, 
                                                      fixedOrderingCost, 
                                                      holdingCost, 
                                                      penaltyCost, 
                                                      proportionalOrderingCost, 
                                                      initialInventory, 
                                                      S, s, 
                                                      confidence, 
                                                      errorTolerance);
      System.out.println();
      System.out.println("Simulated cost: "+ df.format(results[0])+
                         " Confidence interval=("+df.format(results[0]-results[1])+","+
                         df.format(results[0]+results[1])+")@"+
                         df.format(confidence*100)+"% confidence");
      System.out.println();
   }
}

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

package jsdp.app.inventory.univariate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.sun.management.OperatingSystemMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory; 

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.Recursion.OptimisationDirection;
import jsdp.sdp.impl.univariate.*;
import jsdp.app.inventory.univariate.simulation.SimulatePolicies;
import jsdp.app.inventory.univariate.simulation.sS_Policy;

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
 *  as a stochastic dynamic programming problem. We use backward recursion and sampling to find optimal policies.
 *  
 *  Run with VM arguments -d64 -Xms512m -Xmx4g
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
      double fixedOrderingCost = 20; 
      double proportionalOrderingCost = 0; 
      double holdingCost = 1;
      double penaltyCost = 8;
      
      double[] meanDemand = {15, 16, 15, 14, 11, 7, 6, 3};
      double coefficientOfVariation = 0.1;
      double truncationQuantile = 0.99;
      
      // Random variables

      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(meanDemand.length)
                                              .mapToObj(i -> new NormalDist(meanDemand[i],meanDemand[i]*coefficientOfVariation))
                                              //.mapToObj(i -> new PoissonDist(meanDemand[i]))
                                              .toArray(Distribution[]::new);
      double[] supportLB = IntStream.iterate(0, i -> i + 1)
                                    .limit(meanDemand.length)
                                    .mapToDouble(i -> NormalDist.inverseF(meanDemand[i],meanDemand[i]*coefficientOfVariation, 1-truncationQuantile))
                                    .toArray();
      double[] supportUB = IntStream.iterate(0, i -> i + 1)
                                    .limit(meanDemand.length)
                                    .mapToDouble(i -> NormalDist.inverseF(meanDemand[i],meanDemand[i]*coefficientOfVariation, truncationQuantile))
                                    .toArray();
      
      double initialInventory = 0;
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      double stepSize = 1;       //Stepsize must be 1 for discrete distributions
      double minState = -200;
      double maxState = 200;
      StateImpl.setStateBoundaries(stepSize, minState, maxState);

      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(double i = state.getInitialState(); 
             i <= StateImpl.getMaxState(); 
             i += StateImpl.getStepSize()){
            feasibleActions.add(new ActionImpl(state, i - state.getInitialState()));
         }
         return feasibleActions;
      };
      
      Function<State, Action> idempotentAction = s -> new ActionImpl(s, 0.0);
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         ActionImpl a = (ActionImpl)action;
         StateImpl fs = (StateImpl)finalState;
         double orderingCost = 
               a.getAction() > 0 ? (fixedOrderingCost + a.getAction()*proportionalOrderingCost) : 0;
         double holdingAndPenaltyCost =   
               holdingCost*Math.max(fs.getInitialState(),0) + penaltyCost*Math.max(-fs.getInitialState(),0);
         return orderingCost+holdingAndPenaltyCost;
      };
      
      // Random Outcome Function
      
      RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction = (initialState, action, finalState) -> {
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
      int maxSampleSize = 100;
      double reductionFactorPerStage = 1;
      
      
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
                                                                  reductionFactorPerStage,
                                                                  stateSpaceLowerBound,
                                                                  loadFactor,
                                                                  HashType.THASHMAP);

      
      System.out.println("--------------Backward recursion--------------");
      StopWatch timer = new StopWatch();
      OperatingSystemMXBean osMBean;
      try {
         osMBean = ManagementFactory.newPlatformMXBeanProxy(
               ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
         long nanoBefore = System.nanoTime();
         long cpuBefore = osMBean.getProcessCpuTime();
         
         timer.start();
         recursion.runBackwardRecursionMonitoring();
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
      double ETC = recursion.getExpectedCost(initialInventory);
      StateDescriptorImpl initialState = new StateDescriptorImpl(0, initialInventory);
      double action = recursion.getOptimalAction(initialState).getAction();
      System.out.println("Expected total cost (assuming an initial inventory level "+initialInventory+"): "+ETC);
      System.out.println("Optimal initial action: "+action);
      System.out.println("Time elapsed: "+timer);
      System.out.println();
      
      /*******************************************************************
       * Charting
       */
      System.out.println("--------------Charting--------------");
      int targetPeriod = 0;                                 
      plotOptimalPolicyAction(targetPeriod, recursion);     //Plot optimal policy action
      BackwardRecursionImpl recursionPlot = new BackwardRecursionImpl(OptimisationDirection.MIN,
                                                                      distributions,
                                                                      supportLB,
                                                                      supportUB,
                                                                      immediateValueFunction,
                                                                      randomOutcomeFunction,
                                                                      buildActionList,
                                                                      idempotentAction,
                                                                      discountFactor,
                                                                      targetPeriod > 0 ? SamplingScheme.NONE : samplingScheme,
                                                                      maxSampleSize,
                                                                      reductionFactorPerStage,
                                                                      HashType.THASHMAP);
      plotOptimalPolicyCost(targetPeriod, recursionPlot);   //Plot optimal policy cost 
      System.out.println();
      
      /*******************************************************************
       * Simulation
       */
      System.out.println("--------------Simulation--------------");
      double confidence = 0.95;           //Simulation confidence level 
      double errorTolerance = 0.001;      //Simulation error threshold
      
      if(simulate) 
         simulate(distributions, 
               fixedOrderingCost, 
               holdingCost, 
               penaltyCost, 
               proportionalOrderingCost, 
               initialInventory, 
               recursion, 
               confidence, 
               errorTolerance);
   }
   
   static void plotOptimalPolicyCost(int targetPeriod, BackwardRecursionImpl recursion){
      recursion.runBackwardRecursionMonitoring(targetPeriod);
      XYSeries series = new XYSeries("Optimal policy");
      for(double i = StateImpl.getMinState(); i <= StateImpl.getMaxState(); i += StateImpl.getStepSize()){
         StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(targetPeriod, i);
         series.add(i,recursion.getExpectedCost(stateDescriptor));
      }
      XYDataset xyDataset = new XYSeriesCollection(series);
      JFreeChart chart = ChartFactory.createXYLineChart("Optimal policy policy - period "+targetPeriod+" expected total cost", "Opening inventory level", "Expected total cost",
            xyDataset, PlotOrientation.VERTICAL, false, true, false);
      ChartFrame frame = new ChartFrame("Optimal policy",chart);
      frame.setVisible(true);
      frame.setSize(500,400);
   }
   
   static void plotOptimalPolicyAction(int targetPeriod, BackwardRecursionImpl recursion){
      XYSeries series = new XYSeries("Optimal policy");
      recursion.getStateSpace()[targetPeriod].entrySet()
                                  .forEach(s ->{
                                     double state = ((StateImpl)s.getValue()).getInitialState();
                                     StateDescriptorImpl descriptor = new StateDescriptorImpl(targetPeriod, state);
                                     double optimalAction = recursion.getOptimalAction(descriptor).getAction();
                                     series.add(state, optimalAction);
                                  });
      
      XYDataset xyDataset = new XYSeriesCollection(series);
      JFreeChart chart = ChartFactory.createXYLineChart("Optimal policy - period "+targetPeriod+" order quantity", "Opening inventory level", "Order quantity",
            xyDataset, PlotOrientation.VERTICAL, false, true, false);
      ChartFrame frame = new ChartFrame("Optimal policy",chart);
      frame.setVisible(true);
      frame.setSize(500,400);
   }
   
   static void simulate(Distribution[] distributions,
         double fixedOrderingCost,
         double holdingCost,
         double penaltyCost,
         double proportionalOrderingCost,
         double initialInventory,
         BackwardRecursionImpl recursion,
         double confidence,
         double errorTolerance){
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.00",otherSymbols);
      
      sS_Policy policy = new sS_Policy(recursion, distributions.length);
      double[][] optimalPolicy = policy.getOptimalPolicy(initialInventory);
      double[] s = optimalPolicy[0];
      double[] S = optimalPolicy[1];      
      for(int i = 0; i < distributions.length; i++){
         System.out.println("S["+(i+1)+"]:"+df.format(S[i])+"\ts["+(i+1)+"]:"+df.format(s[i]));
      }
      
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

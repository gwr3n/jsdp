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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import jsdp.app.inventory.simulation.SimulatePolicies;
import jsdp.sdp.Action;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;
import jsdp.sdp.impl.*;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.probdist.NormalDist;

/**
 *  We formulate the capacitated stochastic lot sizing problem as a stochastic dynamic programming problem. 
 *  
 * @author Roberto Rossi
 *
 */
public class CapacitatedStochasticLotSizing {
   
   public static void main(String args[]){
      
      boolean simulate = true;
      
      /*******************************************************************
       * Problem parameters
       */
      double fixedOrderingCost = 100; 
      double proportionalOrderingCost = 0; 
      double holdingCost = 1;
      double penaltyCost = 4;
      double maxOrderQuantity = 30;
      
      double[] meanDemand = {20,30,20,40,10,20,30,20,40,10,5};
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
      
      double stepSize = 1;       //Stepsize must be 1 for discrete distributions
      int minIntState = -100;
      int maxIntState = 150;
      StateImpl.setStateBoundaries(stepSize, minIntState, maxIntState);

      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(double i = state.getInitialState(); i <= StateImpl.getMaxState() && i <= state.getInitialState() + maxOrderQuantity; i+= StateImpl.getStepSize()){
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
      
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int maxSampleSize = 20;
      
      
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
       * Charting
       */   
      int targetPeriod = 0;
      plotOptimalPolicyAction(targetPeriod, recursion);     //Plot optimal policy action
      plotOptimalPolicyCost(targetPeriod, recursion);       //Plot optimal policy cost      
      
      /*******************************************************************
       * Simulation
       */
      double confidence = 0.95;           //Simulation confidence level 
      double errorTolerance = 0.001;      //Simulation error threshold
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
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
      XYSeries series = new XYSeries("Optimal policy");
      for(double i = 0; i <= StateImpl.getMaxState(); i += StateImpl.getStepSize()){
         StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(targetPeriod, StateImpl.stateToIntState(i));
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
                                     StateDescriptorImpl descriptor = new StateDescriptorImpl(targetPeriod, StateImpl.stateToIntState(state));
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
      
      double[] results = SimulatePolicies.simulateStochaticLotSizing(distributions, 
                                                      fixedOrderingCost, 
                                                      holdingCost, 
                                                      penaltyCost, 
                                                      proportionalOrderingCost, 
                                                      initialInventory, 
                                                      recursion, 
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

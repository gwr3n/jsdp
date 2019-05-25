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
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.Recursion.OptimisationDirection;
import jsdp.sdp.impl.univariate.*;
import jsdp.app.inventory.univariate.simulation.SimulatePolicies;
import jsdp.app.inventory.univariate.simulation.skSk_Policy;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.probdist.NormalDist;

/**
 *  We formulate the capacitated stochastic lot sizing problem as a stochastic dynamic programming problem. 
 *  
 * @author Roberto Rossi
 *
 */
@SuppressWarnings("unused")
public class CapacitatedStochasticLotSizing {
   
   public static void main(String args[]){
      /*Random rnd = new Random();
      rnd.setSeed(1234);
      int counter = 0;
      while(true) {
         System.out.println("Instance #: "+ counter++);
         randomInstance(rnd);
      }*/
      sampleInstance();
   }
   
   public static void randomInstance(Random rnd){
      
      boolean simulate = true;
      
      /*******************************************************************
       * Problem parameters
       */
      
      double fixedOrderingCost = rnd.nextInt(500) + 50; 
      double proportionalOrderingCost = 0; 
      double holdingCost = 1;
      double penaltyCost = rnd.nextInt(5)+1;
      double maxOrderQuantity = 50+rnd.nextInt(150);
      
      double[] meanDemand = IntStream.iterate(0, i -> i + 1)
                                     .limit(8)
                                     .mapToDouble(i -> rnd.nextInt(100)).toArray();
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+proportionalOrderingCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxOrderQuantity+"\n"+
            "Demand: "+ Arrays.toString(meanDemand));
      
      //double coefficientOfVariation = 0.15;
      //double[] stdDemand = {1,1,1,1,1,1,1,1};
      double truncationQuantile = 0.999999;
      
      // Random variables

      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(meanDemand.length)
                                              //.mapToObj(i -> new NormalDist(meanDemand[i],stdDemand[i]))
                                              //.mapToObj(i -> new NormalDist(meanDemand[i],meanDemand[i]*coefficientOfVariation))
                                              .mapToObj(i -> new PoissonDist(meanDemand[i]))
                                              .toArray(Distribution[]::new);
      double[] supportLB = IntStream.iterate(0, i -> i + 1)
                                    .limit(meanDemand.length)
                                    //.mapToDouble(i -> distributions[i].inverseF(1-truncationQuantile))
                                    .mapToDouble(i -> 0)
                                    .toArray();
      double[] supportUB = IntStream.iterate(0, i -> i + 1)
                                    .limit(meanDemand.length)
                                    .mapToDouble(i -> distributions[i].inverseF(truncationQuantile))
                                    .toArray();      
      double initialInventory = 0;
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      double stepSize = 1;       //Stepsize must be 1 for discrete distributions
      double minState = -2000;
      double minStateCheck = -500;
      double maxState = 1000;
      double maxStateCheck = 500;
      StateImpl.setStateBoundaries(stepSize, minState, maxState);

      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(double i = state.getInitialState(); 
             i <= StateImpl.getMaxState() && i <= state.getInitialState() + maxOrderQuantity; 
             i += StateImpl.getStepSize()){
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
      
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int maxSampleSize = 200;
      double reductionFactorPerStage = 1;
      
      
      // Value Function Processing Method: backward recursion
      double discountFactor = 1.0;
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
                                                                  HashType.MAPDB_HEAP_SHARDED);

      System.out.println("--------------Backward recursion--------------");
      recursion.runBackwardRecursionMonitoring();
      System.out.println();
      double ETC = recursion.getExpectedCost(initialInventory);
      StateDescriptorImpl initialState = new StateDescriptorImpl(0, initialInventory);
      double action = recursion.getOptimalAction(initialState).getAction();
      long percent = recursion.getMonitoringInterfaceBackward().getPercentCPU();
      System.out.println("Expected total cost (assuming an initial inventory level "+initialInventory+"): "+ETC);
      System.out.println("Optimal initial action: "+action);
      System.out.println("Time elapsed: "+recursion.getMonitoringInterfaceBackward().getTime());
      System.out.println("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      System.out.println();
      
      
      /*******************************************************************
       * NoInitialOrder
       */
      
      BackwardRecursionImpl recursionNoInitialOrder = new BackwardRecursionImpl(OptimisationDirection.MIN,
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
                                                                                HashType.MAPDB_HEAP_SHARDED);
      int targetPeriod = 0;
      recursionNoInitialOrder.runBackwardRecursion(targetPeriod);
      
      /*******************************************************************
       * KConvexity
       */
      
      if(testKBConvexity(0, recursionNoInitialOrder, minStateCheck, maxStateCheck, fixedOrderingCost, maxOrderQuantity))
         System.out.println("The function is (K,B) convex");
      else
         System.err.println("The function is not (K,B) convex");
      
      System.out.println();
      
      /*******************************************************************
       * OrderUpToCapacity
       */
      
      if(testOrderUpToCapacityiii(0, distributions.length, recursionNoInitialOrder, recursion, minStateCheck, maxStateCheck, fixedOrderingCost, maxOrderQuantity))
         System.out.println("OrderUpToCapacity ok");
      else {
         System.err.println("OrderUpToCapacity violated");
         throw new NullPointerException("OrderUpToCapacity violated");
      }
      
      System.out.println();
      
      /*******************************************************************
       * Simulation
       */
      System.out.println("--------------Simulate SDP policy--------------");
      double confidence = 0.95;            //Simulation confidence level 
      double errorTolerance = 0.0001;      //Simulation error threshold
      
      double simulatedETC = Double.NaN; 
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
         simulatedETC = simulate(distributions, 
                  fixedOrderingCost, 
                  holdingCost, 
                  penaltyCost, 
                  proportionalOrderingCost, 
                  maxOrderQuantity,
                  initialInventory, 
                  recursion, 
                  confidence, 
                  errorTolerance);
      else{
         if(!simulate) System.out.println("Simulation disabled.");
         if(samplingScheme != SamplingScheme.NONE) System.out.println("Cannot simulate a sampled solution, please disable sampling: set samplingScheme == SamplingScheme.NONE.");
      }
      
      /*******************************************************************
       * Simulate (sk,Sk) policy
       */
      System.out.println("--------------Simulate (sk,Sk) policy--------------");
      System.out.println("S[t][k], where t is the time period and k is the (sk,Sk) index.");
      System.out.println();
      confidence = 0.95;            //Simulation confidence level 
      errorTolerance = 0.0001;      //Simulation error threshold
      
      int thresholdNumberLimit = Integer.MAX_VALUE; //Number of thresholds used by the (sk,Sk) policy in each period
      
      if(simulate && samplingScheme == SamplingScheme.NONE) { 
         double policyCost = simulateskSk(distributions, 
                      fixedOrderingCost, 
                      holdingCost, 
                      penaltyCost, 
                      proportionalOrderingCost, 
                      maxOrderQuantity,
                      initialInventory, 
                      recursion, 
                      confidence, 
                      errorTolerance,
                      thresholdNumberLimit,
                      simulatedETC);
      
      if(100*(policyCost-simulatedETC)/simulatedETC != 0)
         throw new NullPointerException("Optimality gap not 0!");
      }else{
         if(!simulate) System.out.println("Simulation disabled.");
         if(samplingScheme != SamplingScheme.NONE) System.out.println("Cannot simulate a sampled solution, please disable sampling: set samplingScheme == SamplingScheme.NONE.");
      }
   }
   
   public static void sampleInstance(){
      
      boolean simulate = true;
      
      /*******************************************************************
       * Problem parameters
       */
      double fixedOrderingCost = 427.0; 
      double proportionalOrderingCost = 0; 
      double holdingCost = 1;
      double penaltyCost = 6;
      double maxOrderQuantity = 166.0;
      
      double[] meanDemand = {2,13,92,13,94,79,80,26};
      //double coefficientOfVariation = 0.15;
      //double[] stdDemand = {1,1,1,1,1,1,1,1};
      double truncationQuantile = 0.999999;
      
      // Random variables

      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(meanDemand.length)
                                              //.mapToObj(i -> new NormalDist(meanDemand[i],stdDemand[i]))
                                              //.mapToObj(i -> new NormalDist(meanDemand[i],meanDemand[i]*coefficientOfVariation))
                                              .mapToObj(i -> new PoissonDist(meanDemand[i]))
                                              .toArray(Distribution[]::new);
      double[] supportLB = IntStream.iterate(0, i -> i + 1)
                                    .limit(meanDemand.length)
                                    //.mapToDouble(i -> distributions[i].inverseF(1-truncationQuantile))
                                    .mapToDouble(i -> 0)
                                    .toArray();
      double[] supportUB = IntStream.iterate(0, i -> i + 1)
                                    .limit(meanDemand.length)
                                    .mapToDouble(i -> distributions[i].inverseF(truncationQuantile))
                                    .toArray();      
      double initialInventory = 0;
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      double stepSize = 1;       //Stepsize must be 1 for discrete distributions
      double minState = -2000;
      double minStateCheck = -200;
      double maxState = 1000;
      double maxStateCheck = 500;
      StateImpl.setStateBoundaries(stepSize, minState, maxState);

      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(double i = state.getInitialState(); 
             i <= StateImpl.getMaxState() && i <= state.getInitialState() + maxOrderQuantity; 
             i += StateImpl.getStepSize()){
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
      
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int maxSampleSize = 200;
      double reductionFactorPerStage = 1;
      
      
      // Value Function Processing Method: backward recursion
      double discountFactor = 1.0;
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
                                                                  HashType.MAPDB_HEAP_SHARDED);

      System.out.println("--------------Backward recursion--------------");
      recursion.runBackwardRecursionMonitoring();
      System.out.println();
      double ETC = recursion.getExpectedCost(initialInventory);
      StateDescriptorImpl initialState = new StateDescriptorImpl(0, initialInventory);
      double action = recursion.getOptimalAction(initialState).getAction();
      long percent = recursion.getMonitoringInterfaceBackward().getPercentCPU();
      System.out.println("Expected total cost (assuming an initial inventory level "+initialInventory+"): "+ETC);
      System.out.println("Optimal initial action: "+action);
      System.out.println("Time elapsed: "+recursion.getMonitoringInterfaceBackward().getTime());
      System.out.println("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      System.out.println();
      
      /*******************************************************************
       * Charting
       */   
      System.out.println("--------------Charting--------------");
      int targetPeriod = 0;                                                                           //If targetPeriod > 0 then no sampling!
      for(int i = 0; i < meanDemand.length; i++) {
         System.out.println("--------------Period "+i+"--------------");
         plotOptimalPolicyAction(i, recursion, StateImpl.getMinState(), StateImpl.getMaxState());     //Plot optimal policy action
      }
      BackwardRecursionImpl recursionNoInitialOrder = new BackwardRecursionImpl(OptimisationDirection.MIN,
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
                                                                      HashType.MAPDB_HEAP_SHARDED);
      plotOptimalPolicyCost(targetPeriod, recursionNoInitialOrder, minState, maxState);   //Plot optimal policy cost      
      System.out.println();
      
      /*******************************************************************
       * KBConvexity
       */
      
      if(testKBConvexity(0, recursionNoInitialOrder, minStateCheck, maxStateCheck, fixedOrderingCost, maxOrderQuantity))
         System.out.println("The function is (K,B) convex");
      else
         System.err.println("The function is not (K,B) convex");
      
      System.out.println();
      
      /*******************************************************************
       * OrderUpToCapacity
       */
      
      for(int t = 0; t < meanDemand.length; t++) {
         if(testOrderUpToCapacityiii(t, distributions.length, recursionNoInitialOrder, recursion, minStateCheck, maxStateCheck, fixedOrderingCost, maxOrderQuantity))
            System.out.println("OrderUpToCapacity ok");
         else {
            System.err.println("OrderUpToCapacity violated");
         }
      }
      
      System.out.println();
      
      /*******************************************************************
       * Simulation
       */
      System.out.println("--------------Simulate SDP policy--------------");
      double confidence = 0.95;            //Simulation confidence level 
      double errorTolerance = 0.0001;      //Simulation error threshold
      
      double simulatedETC = Double.NaN; 
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
         simulatedETC = simulate(distributions, 
                  fixedOrderingCost, 
                  holdingCost, 
                  penaltyCost, 
                  proportionalOrderingCost, 
                  maxOrderQuantity,
                  initialInventory, 
                  recursion, 
                  confidence, 
                  errorTolerance);
      else{
         if(!simulate) System.out.println("Simulation disabled.");
         if(samplingScheme != SamplingScheme.NONE) System.out.println("Cannot simulate a sampled solution, please disable sampling: set samplingScheme == SamplingScheme.NONE.");
      }
      
      /*******************************************************************
       * Simulate (sk,Sk) policy
       */
      System.out.println("--------------Simulate (sk,Sk) policy--------------");
      System.out.println("S[t][k], where t is the time period and k is the (sk,Sk) index.");
      System.out.println();
      confidence = 0.95;            //Simulation confidence level 
      errorTolerance = 0.0001;      //Simulation error threshold
      
      int thresholdNumberLimit = Integer.MAX_VALUE; //Number of thresholds used by the (sk,Sk) policy in each period
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
         simulateskSk(distributions, 
                      fixedOrderingCost, 
                      holdingCost, 
                      penaltyCost, 
                      proportionalOrderingCost, 
                      maxOrderQuantity,
                      initialInventory, 
                      recursion, 
                      confidence, 
                      errorTolerance,
                      thresholdNumberLimit,
                      simulatedETC);
      else{
         if(!simulate) System.out.println("Simulation disabled.");
         if(samplingScheme != SamplingScheme.NONE) System.out.println("Cannot simulate a sampled solution, please disable sampling: set samplingScheme == SamplingScheme.NONE.");
      }
   }
   
   static boolean testOrderUpToCapacityShiaoxiang(int targetPeriod, int periods, BackwardRecursionImpl recursion, double minState, double maxState, double fixedOrderingCost, double maxOrderQuantity) {
            
      boolean flag = true;
      for(double x = maxState; x >= minState; x -= StateImpl.getStepSize()) {
         for(double y = x; y >= minState; y -= StateImpl.getStepSize()) {

            StateDescriptorImpl stateDescriptorx = new StateDescriptorImpl(targetPeriod, x);
            double gx = recursion.getExpectedCost(stateDescriptorx);

            StateDescriptorImpl stateDescriptorxa = new StateDescriptorImpl(targetPeriod, x+maxOrderQuantity);
            double gxa = recursion.getExpectedCost(stateDescriptorxa);

            StateDescriptorImpl stateDescriptorxd = new StateDescriptorImpl(targetPeriod, x+StateImpl.getStepSize());
            double gxd = recursion.getExpectedCost(stateDescriptorxd)-recursion.getExpectedCost(stateDescriptorx); 

            StateDescriptorImpl stateDescriptory = new StateDescriptorImpl(targetPeriod, y-maxOrderQuantity);
            double gy = recursion.getExpectedCost(stateDescriptory);

            StateDescriptorImpl stateDescriptorya = new StateDescriptorImpl(targetPeriod, y);
            double gya = recursion.getExpectedCost(stateDescriptorya);

            StateDescriptorImpl stateDescriptoryd = new StateDescriptorImpl(targetPeriod, y+StateImpl.getStepSize());
            double gyd = recursion.getExpectedCost(stateDescriptoryd)-recursion.getExpectedCost(stateDescriptory); 

            double delta = 0.0000000001;
            
            if((fixedOrderingCost + gya - gy)/maxOrderQuantity >= (fixedOrderingCost + gxa - gx)/maxOrderQuantity + delta){
               System.out.println("K: "+fixedOrderingCost);
               System.out.println("x: "+x);
               System.out.println("y: "+y);
               System.out.println("gx: "+gx);
               System.out.println("gy: "+gy);
               System.out.println("gxa: "+gxa);
               System.out.println("gya: "+gya);
               System.out.println("gxd: "+gxd);
               System.out.println("gyd: "+gyd);
               System.out.println("Discrepancy: "+(fixedOrderingCost + gya - gy)/maxOrderQuantity+">"+(fixedOrderingCost + gxa - gx)/maxOrderQuantity);
               flag = false;
            }
         }
      }
      return flag;
   }
   
   static boolean testOrderUpToCapacity(int targetPeriod, int periods, BackwardRecursionImpl recursionNoOrder, BackwardRecursionImpl recursionOrder, double minState, double maxState, double fixedOrderingCost, double maxOrderQuantity) {

      skSk_Policy policy = new skSk_Policy(recursionOrder, periods);
      double[][][] optimalPolicy = policy.getOptimalPolicy(0, Integer.MAX_VALUE, maxOrderQuantity);
      double S = optimalPolicy[1][0][optimalPolicy[1][0].length-1];

      boolean flag = true;
      for(double x = S-maxOrderQuantity; x >= minState; x -= StateImpl.getStepSize()) {

         StateDescriptorImpl stateDescriptorx = new StateDescriptorImpl(targetPeriod, x);
         double gx = recursionNoOrder.getExpectedCost(stateDescriptorx);

         StateDescriptorImpl stateDescriptorxa = new StateDescriptorImpl(targetPeriod, x+maxOrderQuantity);
         double gxa = recursionNoOrder.getExpectedCost(stateDescriptorxa);

         StateDescriptorImpl stateDescriptorxd = new StateDescriptorImpl(targetPeriod, x+StateImpl.getStepSize());
         double gxd = recursionNoOrder.getExpectedCost(stateDescriptorxd)-recursionNoOrder.getExpectedCost(stateDescriptorx); 

         double delta = 0.0000000001;

         if((fixedOrderingCost + gxa - gx)/maxOrderQuantity + delta > 0){
            System.out.println("K: "+fixedOrderingCost);
            System.out.println("x: "+x);
            System.out.println("gx: "+gx);
            System.out.println("gxa: "+gxa);
            System.out.println("gxd: "+gxd);
            System.out.println("Discrepancy: "+(fixedOrderingCost + gxa - gx)/maxOrderQuantity+" > "+0);
            flag = false;
         }

      }
      return flag;
   }
   
   static boolean testOrderUpToCapacityiii(int targetPeriod, int periods, BackwardRecursionImpl recursionNoOrder, BackwardRecursionImpl recursionOrder, double minState, double maxState, double fixedOrderingCost, double maxOrderQuantity) {
      
      skSk_Policy policy = new skSk_Policy(recursionOrder, periods);
      double[][][] optimalPolicy = policy.getOptimalPolicy(0, Integer.MAX_VALUE, maxOrderQuantity);
      double Q = maxOrderQuantity;
      for(int i = 0; i < optimalPolicy[1][targetPeriod].length; i++) {
         double S = optimalPolicy[1][targetPeriod][i];
         double s = optimalPolicy[0][targetPeriod][i];
         Q = Math.min(Q, S-s);
      }
      
      boolean flag = true;
      for(double x = maxState-maxOrderQuantity; x >= minState; x -= StateImpl.getStepSize()) {
         for(double a = 0; a <= Q; a += StateImpl.getStepSize()) {
            for(double y = x + a - maxOrderQuantity; y >= minState; y -= StateImpl.getStepSize()) {
               
               StateDescriptorImpl stateDescriptorx = new StateDescriptorImpl(targetPeriod, x);
               double gx = recursionNoOrder.getExpectedCost(stateDescriptorx);

               StateDescriptorImpl stateDescriptorxa = new StateDescriptorImpl(targetPeriod, x+a);
               double gxa = recursionNoOrder.getExpectedCost(stateDescriptorxa);

               //StateDescriptorImpl stateDescriptorxd = new StateDescriptorImpl(targetPeriod, x+StateImpl.getStepSize());
               //double gxd = recursionNoOrder.getExpectedCost(stateDescriptorxd)-recursionNoOrder.getExpectedCost(stateDescriptorx); 

               StateDescriptorImpl stateDescriptory = new StateDescriptorImpl(targetPeriod, y);
               double gy = recursionNoOrder.getExpectedCost(stateDescriptory);

               StateDescriptorImpl stateDescriptorya = new StateDescriptorImpl(targetPeriod, y+maxOrderQuantity);
               double gya = recursionNoOrder.getExpectedCost(stateDescriptorya);

               //StateDescriptorImpl stateDescriptoryd = new StateDescriptorImpl(targetPeriod, y+StateImpl.getStepSize());
               //double gyd = recursionNoOrder.getExpectedCost(stateDescriptoryd)-recursionNoOrder.getExpectedCost(stateDescriptory); 

               double delta = 0.0000000001;

               if((fixedOrderingCost + gya - gy)/maxOrderQuantity >= (fixedOrderingCost + gxa - gx)/a + delta){
                  System.out.println("K: "+fixedOrderingCost);
                  System.out.println("x: "+x);
                  System.out.println("y: "+y);
                  System.out.println("a: "+a);
                  System.out.println("gx: "+gx);
                  System.out.println("gy: "+gy);
                  System.out.println("gxa: "+gxa);
                  System.out.println("gya: "+gya);
                  System.out.println("Discrepancy: "+(fixedOrderingCost + gya - gy)/maxOrderQuantity+">"+(fixedOrderingCost + gxa - gx)/a);
                  flag = false;
               }
            }
         }
      }
      return flag;
   }
   
   static boolean testKBConvexity(int targetPeriod, BackwardRecursionImpl recursion, double minState, double maxState, double fixedOrderingCost, double maxOrderQuantity) {
      //recursion.runBackwardRecursion(targetPeriod); // Not strictly needed because it has been already called by the plot function, saves time.

      for(double x = maxState-maxOrderQuantity; x >= minState; x -= StateImpl.getStepSize()) {
         for(double a = StateImpl.getStepSize(); a <= maxOrderQuantity; a += StateImpl.getStepSize()) {

            StateDescriptorImpl stateDescriptorx = new StateDescriptorImpl(targetPeriod, x);
            double gx = recursion.getExpectedCost(stateDescriptorx);

            StateDescriptorImpl stateDescriptorxa = new StateDescriptorImpl(targetPeriod, x+a);
            double gxa = recursion.getExpectedCost(stateDescriptorxa);

            StateDescriptorImpl stateDescriptorxd = new StateDescriptorImpl(targetPeriod, x-StateImpl.getStepSize());
            double gxd = recursion.getExpectedCost(stateDescriptorx)-recursion.getExpectedCost(stateDescriptorxd); 

            if(fixedOrderingCost + gxa - gx - a*gxd < 0) {
               System.out.println("K: "+fixedOrderingCost);
               System.out.println("x: "+x);
               System.out.println("a: "+a);
               System.out.println("gx: "+gx);
               System.out.println("gxa: "+gxa);
               System.out.println("gxd: "+gxd);
               System.out.println("Discrepancy: "+(fixedOrderingCost + gxa - gx - a*gxd));
               return false;
            }
         }
      }
      
      return true;
   }
   
   static void plotOptimalPolicyCost(int targetPeriod, BackwardRecursionImpl recursion, double minState, double maxState){
      recursion.runBackwardRecursion(targetPeriod);
      XYSeries series = new XYSeries("Optimal policy");
      for(double i = minState; i <= maxState; i += StateImpl.getStepSize()){
         StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(targetPeriod, i);
         series.add(i,recursion.getExpectedCost(stateDescriptor));
         System.out.println("("+i+","+(recursion.getExpectedCost(stateDescriptor)/*-200*/)+")");
      }
      XYDataset xyDataset = new XYSeriesCollection(series);
      JFreeChart chart = ChartFactory.createXYLineChart("Optimal policy policy - period "+targetPeriod+" expected total cost", "Opening inventory level", "Expected total cost",
            xyDataset, PlotOrientation.VERTICAL, false, true, false);
      ChartFrame frame = new ChartFrame("Optimal policy",chart);
      frame.setVisible(true);
      frame.setSize(500,400);
   }
   
   static void plotOptimalPolicyAction(int targetPeriod, BackwardRecursionImpl recursion, double minState, double maxState){
      XYSeries series = new XYSeries("Optimal policy");
      for(double i = minState; i <= maxState; i += StateImpl.getStepSize()){
         StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(targetPeriod, i);
         series.add(i,recursion.getOptimalAction(stateDescriptor).getAction());
         //System.out.println("("+i+","+recursion.getOptimalAction(stateDescriptor).getAction()+")");
      }
      
      XYDataset xyDataset = new XYSeriesCollection(series);
      JFreeChart chart = ChartFactory.createXYLineChart("Optimal policy - period "+targetPeriod+" order quantity", "Opening inventory level", "Order quantity",
            xyDataset, PlotOrientation.VERTICAL, false, true, false);
      ChartFrame frame = new ChartFrame("Optimal policy",chart);
      frame.setVisible(true);
      frame.setSize(500,400);
   }
   
   static double simulate(Distribution[] distributions,
                        double fixedOrderingCost,
                        double holdingCost,
                        double penaltyCost,
                        double proportionalOrderingCost,
                        double maxOrderQuantity,                        
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
      
      return results[0];
   }
   
   static double simulateskSk(Distribution[] distributions,
         double fixedOrderingCost,
         double holdingCost,
         double penaltyCost,
         double proportionalOrderingCost,
         double maxOrderQuantity,                        
         double initialInventory,                        
         BackwardRecursionImpl recursion,
         double confidence,
         double errorTolerance,
         int thresholdNumberLimit,
         double optimalPolicyCost){


      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.00",otherSymbols);

      skSk_Policy policy = new skSk_Policy(recursion, distributions.length);
      double[][][] optimalPolicy = policy.getOptimalPolicy(initialInventory, thresholdNumberLimit, maxOrderQuantity);
      double[][] s = optimalPolicy[0];
      double[][] S = optimalPolicy[1];
      for(int i = 0; i < distributions.length; i++){
         for(int k = 0; k < s[i].length; k++){
            System.out.println("S["+(i+1)+"]["+(k+1)+"]:"+df.format(S[i][k])+"\ts["+(i+1)+"]["+(k+1)+"]:"+df.format(s[i][k]));
         }
         System.out.println();
      }

      double[] results = SimulatePolicies.simulate_skSk(distributions, 
            fixedOrderingCost, 
            holdingCost, 
            penaltyCost, 
            proportionalOrderingCost,
            maxOrderQuantity,
            initialInventory, 
            S,
            s,
            confidence, 
            errorTolerance);
      System.out.println();
      System.out.println("Simulated cost: "+ df.format(results[0])+
            " Confidence interval=("+df.format(results[0]-results[1])+","+
            df.format(results[0]+results[1])+")@"+
            df.format(confidence*100)+"% confidence");
      DecimalFormat df3 = new DecimalFormat("#.000",otherSymbols);
      System.out.println("Optimality gap: "+df3.format(100*(results[0]-optimalPolicyCost)/optimalPolicyCost)+"%");
      System.out.println();
      
      return results[0];
   }
}

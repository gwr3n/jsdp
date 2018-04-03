package jsdp.app.inventory.univariate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import jsdp.app.inventory.univariate.simulation.skSk_Policy;
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

public class CapacitatedStochasticLotSizingBatch {
   
   public static void main(String args[]){
      runBatch("results.csv");
   }
   
   public static void writeToFile(String fileName, String str){
      File results = new File(fileName);
      try {
         FileOutputStream fos = new FileOutputStream(results, true);
         OutputStreamWriter osw = new OutputStreamWriter(fos);
         osw.write(str+"\n");
         osw.close();
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   
   public static void runBatch(String fileName){
      double[] fixedOrderingCost = {250,500,1000};
      double[] proportionalOrderingCost = {2,5,10};
      double holdingCost = 1;
      double[] penaltyCost = {5,10,15};
      double[] maxOrderQuantity = {1,2,3}; //Max order quantity in m*avgDemand
      double[][] meanDemand = {
            {30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30}, 
            {46 ,49 ,50 ,50 ,49 ,46 ,42 ,38 ,35 ,33 ,30 ,28 ,26 ,23 ,21 ,18 ,14 ,11 ,8 ,6}, 
            {7 ,9 ,11 ,13 ,17 ,22 ,24 ,26 ,32 ,34 ,36 ,41 ,44 ,47 ,48 ,50 ,50 ,49 ,47 ,44},
            {47 ,30 ,13 ,6 ,13 ,30 ,47 ,54 ,47 ,30 ,13 ,6 ,13 ,30 ,47 ,30 ,15 ,8 ,11 ,30}, 
            {36 ,30 ,24 ,21 ,24 ,30 ,36 ,39 ,36 ,30 ,24 ,21 ,24 ,30 ,36 ,31 ,24 ,21 ,26 ,33},
            {63 ,27 ,10 ,24 ,1 ,23 ,33 ,35 ,67 ,7 ,14 ,41 ,4 ,63 ,26 ,45 ,53 ,25 ,10 ,50},
            {5 ,15 ,46 ,140 ,80 ,147 ,134 ,74 ,84 ,109 ,47 ,88 ,66 ,28 ,32 ,89 ,162 ,36 ,32 ,50},
            {14 ,24 ,71 ,118 ,49 ,86 ,152 ,117 ,226 ,208 ,78 ,59 ,96 ,33 ,57 ,116 ,18 ,135 ,128 ,180},
            {13 ,35 ,79 ,43 ,44 ,59 ,22 ,55 ,61 ,34 ,50 ,95 ,36 ,145 ,160 ,104 ,151 ,86 ,123 ,64},
            {15 ,56 ,19 ,84 ,136 ,67 ,67 ,155 ,87 ,164 ,194 ,67 ,65 ,132 ,35 ,131 ,133 ,36 ,173 ,152}
      };
      String[] demandPattern = {"STA", "LC1", "LC2", "SIN1", "SIN2", "RAND", "EMP1", "EMP2", "EMP3", "EMP4"};
      
      writeToFile(fileName,  "Fixed ordering cost, Proportional ordering cost, Penalty cost, Max order quantity, Expected Demand, ETC sdp, ETC sim sdp, ETC sim skSk, ETC sim skSk Two, ETC sim skSk One, Max number of levels");
      
      for(double oc : fixedOrderingCost) {
         for(double u : proportionalOrderingCost) {
            for(double p : penaltyCost) {
               for(double m : maxOrderQuantity) {
                  for(int d = 0; d < meanDemand.length; d++) {
                     double totalDemand = Arrays.stream(meanDemand[d]).average().getAsDouble();
                     double[] result = runInstance(oc, u, holdingCost, p, Math.round(m*totalDemand), meanDemand[d]);
                     writeToFile(fileName, oc + "," + u + "," + p + "," + Math.round(m*totalDemand) + "," + demandPattern[d] + "," + result[0] +","+ result[1] +","+ result[2] +","+result[3]+","+result[4]+","+result[5]);
                  }
               }
            }
         }
      }
   }
   
   public static double[] runInstance(
         double fixedOrderingCost,
         double proportionalOrderingCost,
         double holdingCost,
         double penaltyCost,
         double maxOrderQuantity,
         double[] meanDemand){
      
      boolean simulate = true;
      
      double truncationQuantile = 0.9999;
      
      // Random variables

      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(meanDemand.length)
                                              //.mapToObj(i -> new NormalDist(meanDemand[i],stdDemand[i]))
                                              //.mapToObj(i -> new NormalDist(meanDemand[i],meanDemand[i]*coefficientOfVariation))
                                              .mapToObj(i -> new PoissonDist(meanDemand[i]))
                                              .toArray(Distribution[]::new);
      double[] supportLB = IntStream.iterate(0, i -> i + 1)
                                    .limit(meanDemand.length)
                                    .mapToDouble(i -> distributions[i].inverseF(1-truncationQuantile))
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
      double minState = -300;
      double maxState = 500;
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
       * Simulation
       */
      System.out.println("--------------Simulate SDP policy--------------");
      double confidence = 0.95;            //Simulation confidence level 
      double errorTolerance = 0.0001;      //Simulation error threshold
      
      double simulatedETC = Double.NaN; 
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
         simulatedETC = CapacitatedStochasticLotSizing.simulate(distributions, 
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
      
      skSk_Policy policy = new skSk_Policy(recursion, distributions.length);
      double[][][] optimalPolicy = policy.getOptimalPolicy(initialInventory, Integer.MAX_VALUE);
      long maxNumberOfLevels = Arrays.stream(optimalPolicy[0]).mapToLong(a -> Arrays.stream(a).count()).max().getAsLong();
      
      System.out.println("--------------Simulate (sk,Sk) policy--------------");
      System.out.println("S[t][k], where t is the time period and k is the (sk,Sk) index.");
      System.out.println();
      confidence = 0.95;            //Simulation confidence level 
      errorTolerance = 0.0001;      //Simulation error threshold
      
      int thresholdNumberLimit = Integer.MAX_VALUE; //Number of thresholds used by the (sk,Sk) policy in each period
      double simulatedskSkETCGen = Double.NaN;
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
         simulatedskSkETCGen = CapacitatedStochasticLotSizing.simulateskSk(distributions, 
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
      
      thresholdNumberLimit = 2; //Number of thresholds used by the (sk,Sk) policy in each period
      double simulatedskSkETCTwo = Double.NaN;
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
          simulatedskSkETCTwo = CapacitatedStochasticLotSizing.simulateskSk(distributions, 
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
      
      thresholdNumberLimit = 1; //Number of thresholds used by the (sk,Sk) policy in each period
      double simulatedskSkETCOne = Double.NaN;
      
      if(simulate && samplingScheme == SamplingScheme.NONE) 
          simulatedskSkETCOne = CapacitatedStochasticLotSizing.simulateskSk(distributions, 
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
      
      double[] etcOut = new double[6];
      etcOut[0] = ETC;
      etcOut[1] = simulatedETC;
      etcOut[2] = simulatedskSkETCGen;
      etcOut[3] = simulatedskSkETCTwo;
      etcOut[4] = simulatedskSkETCOne;
      etcOut[5] = maxNumberOfLevels;
      return etcOut;
   }
}

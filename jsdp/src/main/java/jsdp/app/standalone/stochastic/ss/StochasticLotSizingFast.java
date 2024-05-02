package jsdp.app.standalone.stochastic.ss;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import jsdp.utilities.sampling.SampleFactory;
import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.stat.Tally;

public class StochasticLotSizingFast {
   
   private static double computeImmediateCost(
         int inventory, 
         int quantity, 
         int demand,
         double holdingCost, 
         double penaltyCost, 
         double fixedOrderingCost, 
         double unitCost) {
      return ((quantity > 0) ? fixedOrderingCost + unitCost * quantity : 0) +
            holdingCost*Math.max(0, inventory + quantity - demand) 
            + penaltyCost *Math.max(0, demand - inventory - quantity);
   }
   
   private static final double error_tolerance = 1.0E-10; // Prevents double rounding errors
   
   private static double getOptimalCost(double[] expectedTotalCosts) {
      double min = expectedTotalCosts[0];
      for(int a = 1; a < expectedTotalCosts.length; a++) {
         if(expectedTotalCosts[a] < min-error_tolerance) {
            min = expectedTotalCosts[a];
         }
      }
      return min;
   }
   
   private static int getOptimalAction(double[] expectedTotalCosts) {
      double min = expectedTotalCosts[0];
      int action = 0;
      for(int a = 1; a < expectedTotalCosts.length; a++) {
         if(expectedTotalCosts[a] < min-error_tolerance) {
            min = expectedTotalCosts[a];
            action = a;
         }
      }
      return action;
   }
   
   public static Solution sdp(Instance instance) {
      
      double demandProbabilities [][] = InstancePortfolio.computeDemandProbability(instance);
      
      int optimalAction[][] = new int [instance.getStages()][instance.stateSpaceSize()];
      double Gn[][] = new double [instance.getStages()][instance.stateSpaceSize()];
      double Cn[][] = new double [instance.getStages()][instance.stateSpaceSize()];
      
      /** Compute Expected Cost **/
      
      for(int t = instance.getStages()-1; t >= 0; t--) {                               // Time
         double totalCost[][] = new double [instance.stateSpaceSize()][];
         boolean found_s = false;
         for(int i = instance.stateSpaceSize()-1; i >= 0; i--) {                       // Inventory
            totalCost[i] = new double [instance.stateSpaceSize()-i];
            if (found_s && t > 0) {
               Cn[t][i] = Cn[t][i+1]+instance.unitCost;
               optimalAction[t][i] = optimalAction[t][i+1]+1;
               continue;
            }
            for(int a = 0; a < totalCost[i].length; a++) {                             // Actions
               double totalProbabilityMass = 0;
               for(int d = 0; d < demandProbabilities[t].length; d++) {                // Demand
                  double immediateCost = 0;
                  double futureCost = 0;
                  if((instance.inventory(i) + a - d <= instance.maxInventory) && (instance.inventory(i) + a - d >= instance.minInventory)) {
                     immediateCost = demandProbabilities[t][d]*
                           computeImmediateCost(instance.inventory(i), a, d, instance.holdingCost, instance.penaltyCost, instance.fixedOrderingCost, instance.unitCost);
                     futureCost = demandProbabilities[t][d]*( (t==instance.getStages()-1) ? 0 : instance.discountFactor*Cn[t+1][i+a-d] );
                     totalProbabilityMass += demandProbabilities[t][d];
                  }
                  totalCost[i][a] += immediateCost + futureCost;
               }
               totalCost[i][a]/=totalProbabilityMass;
            }
            Gn[t][i] = totalCost[i][0];
            Cn[t][i] = getOptimalCost(totalCost[i]);
            optimalAction[t][i] = getOptimalAction(totalCost[i]);
            if(optimalAction[t][i] > 0) found_s = true; // K-convexity  
         }
      }
      return new Solution(optimalAction, Gn, Cn);
   }
   
   public static void printSolution(Instance instance, Solution solution, int safeMin) {
      int[] S = solution.find_S(instance, safeMin);
      int t = 0;
      System.out.println("Expected total cost at Sm: "+solution.Cn[t][S[t]-instance.minInventory]);
      System.out.println("Expected total cost with zero initial inventory: "+(solution.Cn[t][-instance.minInventory]));
   }
   
   public static void printPolicy(Instance instance, Solution solution, int safeMin) {
      int[] S = solution.find_S(instance, safeMin);
      int[] s = solution.find_s(instance, safeMin);
      System.out.println("Policy");
      System.out.println("s\tS");
      for(int t =0; t < instance.getStages(); t++) {
         System.out.println("--- Period "+t+" ---");
         System.out.println(s[t]+"\t"+S[t]);
      }
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
   
   private static double[][] getDemandPatters(){
      double[][] meanDemand = {
            {30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30 ,30},         //STA 
            {46 ,49 ,50 ,50 ,49 ,46 ,42 ,38 ,35 ,33 ,30 ,28 ,26 ,23 ,21 ,18 ,14 ,11 ,8 ,6},           //LC1
            {7 ,9 ,11 ,13 ,17 ,22 ,24 ,26 ,32 ,34 ,36 ,41 ,44 ,47 ,48 ,50 ,50 ,49 ,47 ,44},           //LC2
            {47 ,30 ,13 ,6 ,13 ,30 ,47 ,54 ,47 ,30 ,13 ,6 ,13 ,30 ,47 ,30 ,15 ,8 ,11 ,30},            //SIN1
            {36 ,30 ,24 ,21 ,24 ,30 ,36 ,39 ,36 ,30 ,24 ,21 ,24 ,30 ,36 ,31 ,24 ,21 ,26 ,33},         //SIN2
            {63 ,27 ,10 ,24 ,1 ,23 ,33 ,35 ,67 ,7 ,14 ,41 ,4 ,63 ,26 ,45 ,53 ,25 ,10 ,50},            //RAND
            {5 ,15 ,46 ,140 ,80 ,147 ,134 ,74 ,84 ,109 ,47 ,88 ,66 ,28 ,32 ,89 ,162 ,36 ,32 ,50},     //EMP1
            {14 ,24 ,71 ,118 ,49 ,86 ,152 ,117 ,226 ,208 ,78 ,59 ,96 ,33 ,57 ,116 ,18 ,135 ,128 ,180},//EMP2
            {13 ,35 ,79 ,43 ,44 ,59 ,22 ,55 ,61 ,34 ,50 ,95 ,36 ,145 ,160 ,104 ,151 ,86 ,123 ,64},    //EMP3
            {15 ,56 ,19 ,84 ,136 ,67 ,67 ,155 ,87 ,164 ,194 ,67 ,65 ,132 ,35 ,131 ,133 ,36 ,173 ,152} //EMP4
      };

      
      return meanDemand;
   }
   
   public static void runBatchPoisson(String fileName){
      double tail = 0.0001;
      int minInventory = -1000;
      int maxInventory = 1000;
      
      double[] fixedOrderingCost = {250,500,1000};
      double[] proportionalOrderingCost = {2,5,10};
      double holdingCost = 1;
      double[] penaltyCost = {5,10,15};
      double[] maxOrderQuantity = {2,3,4}; //Max order quantity in m*avgDemand
      double[][] meanDemand = getDemandPatters();
      String[] demandPattern = {"STA", "LC1", "LC2", "SIN1", "SIN2", "RAND", "EMP1", "EMP2", "EMP3", "EMP4"};
      
      writeToFile(fileName,  "Fixed ordering cost, Proportional ordering cost, Capacity, Expected Demand, ETC SDP, ETC sim");
      
      int instances = fixedOrderingCost.length*proportionalOrderingCost.length*penaltyCost.length*maxOrderQuantity.length*demandPattern.length;
      int count = 0;
      for(double oc : fixedOrderingCost) {
         for(double u : proportionalOrderingCost) {
            for(double p : penaltyCost) {
               for(int d = 0; d < meanDemand.length; d++) {
                  
                  /* Skip instances 
                  if(count < 561) {
                     count++;
                     continue;
                  }*/
                  
                  Distribution[] demand = Arrays.stream(meanDemand[d]).mapToObj(k -> new PoissonDist(k)).toArray(Distribution[]::new);
                  
                  Instance instance = new Instance(oc, u, holdingCost, p, demand, tail, minInventory, maxInventory);
                  
                  int initialInventory = 0;
                  
                  double[] result = solveInstance(instance, initialInventory);
                  writeToFile(fileName, oc + "," + u + "," + p + "," + demandPattern[d] + "," + result[0] +","+ result[1]);
                  System.out.println((++count)+"/"+instances);
               }
            }
         }
      }
   }
   
   public static void tabulateBatchPoisson(String fileName){
      double tail = 0.0001;
      int minInventory = -1000;
      int maxInventory = 1000;
      int safeMin = -100;
      int safeMax = 1000;
      
      int periods = 10;
      double[] fixedOrderingCost = {250};
      double[] proportionalOrderingCost = {0};
      double holdingCost = 1;
      double[] penaltyCost = {5};
      
      long seed = 4321;
      Random rnd = new Random(seed);
      double[][] meanDemand = new double[100][];
      for(int i = 0; i < meanDemand.length; i++) {
         meanDemand[i] = rnd.doubles(0, 200).limit(periods).toArray();
      }
      
      int instances = fixedOrderingCost.length*proportionalOrderingCost.length*penaltyCost.length*meanDemand.length;
      int count = 0;
      for(double oc : fixedOrderingCost) {
         for(double u : proportionalOrderingCost) {
            for(double p : penaltyCost) {
               for(int d = 0; d < meanDemand.length; d++) {
                  
                  Distribution[] demand = Arrays.stream(meanDemand[d]).mapToObj(k -> new PoissonDist(k)).toArray(Distribution[]::new);
                  
                  Instance instance = new Instance(oc, u, holdingCost, p, demand, tail, minInventory, maxInventory);
                  
                  int initialInventory = 0;
                  
                  boolean compact = false;
                  String result = tabulateInstance(instance, initialInventory, safeMin, safeMax, compact);
                  writeToFile(fileName, result);
                  System.out.println((++count)+"/"+instances);
               }
            }
         }
      }
   }
   
   public static double[] solveInstance(Instance instance, int initialInventory) {
      
      //System.out.println("***************** Print instance ***************");
      //System.out.println(instance.toString());
      //System.out.println("************************************************");
      //System.out.println();
      
      Solution solution = sdp(instance);
      
      double sdpETC = solution.Cn[0][initialInventory-instance.minInventory];
      
      //System.out.println("***************** Print policy *****************");
      //printPolicy(instance, solution, safeMin);
      //System.out.println("************************************************");
      //System.out.println();
      
      double confidence = 0.95;
      double error = 0.0001;
      int[] S = solution.find_S(instance, instance.minInventory);
      int[] s = solution.find_s(instance, instance.minInventory);
      boolean verifyOptimal = true;
      double simulatedsSETC = simulate_sS(instance, solution, initialInventory, S, s, confidence, error, verifyOptimal)[0];
      
      double[] etcOut = new double[2];
      etcOut[0] = sdpETC;
      etcOut[1] = simulatedsSETC;
      return etcOut;
   }
   
   public static String tabulateInstance(Instance instance, int initialInventory, int safeMin, int safeMax, boolean compact) {
      
      String out = ""+instance.fixedOrderingCost+","+instance.unitCost+","+instance.penaltyCost+",";
      for(int i = 0; i < instance.demand.length; i++) {
         out += instance.demand[i].getMean();
         if(i < instance.demand.length-1) out += ",";
      }
      out += "\n";
      
      Solution solution = sdp(instance);
      
      if(!compact) { // tabulate the functional equation
         out += "";
         for(int i = Math.max(0,safeMin-instance.minInventory); i < Math.min(instance.stateSpaceSize(),safeMax-instance.minInventory); i++) {
            out += solution.Gn[0][i] + 
                  ((i == Math.min(instance.stateSpaceSize(),safeMax-instance.minInventory) - 1) ? "" : ", ");
         }
         out += "";
      } else { // tabulate (s,S) policy parameters
         out += "";
         int[] S = solution.find_S(instance, safeMin);
         int[] s = solution.find_s(instance, safeMin);
         double cost = solution.Gn[0][S[0]-instance.minInventory];
         out += S[0] + "," + s[0] + "," + cost;
         out += "";
      }
      
      return out;
   }

   public static void solveSampleInstance(Instances problemInstance, long seed) {
      
      /** Random instances **/
      Random rnd = new Random();
      rnd.setSeed(seed);
      
      Instance instance; 
      switch(problemInstance) {
         case SAMPLE_POISSON:
         default:
            instance = InstancePortfolio.generateSamplePoissonInstance();
            break;
      }
      
      Solution solution = sdp(instance);
      
      System.out.println();
      printSolution(instance, solution, instance.minInventory);
      
      System.out.println();
      printPolicy(instance, solution, instance.minInventory);
      
      System.out.println("***************** Simulate *****************");
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.000",otherSymbols);
      int initialInventory = 0;
      double optimalPolicyCost = solution.Cn[0][initialInventory-instance.minInventory];
      double confidence = 0.95;
      double error = 0.0001;
      double[] results = null;
      try {
         boolean verifyOptimal = true;
         int[] S = solution.find_S(instance, instance.minInventory);
         int[] s = solution.find_s(instance, instance.minInventory);
         results = simulate_sS(instance, solution, initialInventory, S, s, confidence, error, verifyOptimal);
         System.out.println(
               "Optimal policy cost: "+ df.format(optimalPolicyCost)+
               "\nSimulated cost: "+ df.format(results[0])+
               "\nConfidence interval=("+df.format(results[0]-results[1])+","+
               df.format(results[0]+results[1])+")@"+
               df.format(confidence*100)+"% confidence");
         System.out.println("Optimality gap: "+df.format(100*(results[0]-optimalPolicyCost)/optimalPolicyCost)+"%");
      }catch(Exception e) {
         System.out.println("This instance cannot be simulated.");
      }
      System.out.println("*******************************************");
      System.out.println();
   }

   /**
    * Simulation of an (s,S) policy
    */
   public static double[] simulate_sS(
         Instance instance,
         Solution solution,
         int initialStock,
         int[] S, 
         int[] s, 
         double confidence,
         double error,
         boolean verifyOptimal){
      Distribution[] demand = instance.demand;
      double orderCost = instance.fixedOrderingCost;
      double holdingCost = instance.holdingCost;
      double penaltyCost = instance.penaltyCost;
      double unitCost = instance.unitCost;
      double discountFactor = instance.discountFactor;
      
      Tally costTally = new Tally();
      Tally[] stockPTally = new Tally[demand.length];
      Tally[] stockNTally = new Tally[demand.length];
      for(int i = 0; i < demand.length; i++) {
         stockPTally[i] = new Tally();
         stockNTally[i] = new Tally();
      }
      
      int minRuns = 1000;
      int maxRuns = 1000000;
      
      SampleFactory.resetStartStream();
      
      double[] centerAndRadius = new double[2];
      for(int i = 0; i < minRuns || (centerAndRadius[1]>=centerAndRadius[0]*error && i < maxRuns); i++){
         double[] demandRealizations = SampleFactory.getNextSample(demand);
         
         double replicationCost = 0;
         double inventory = initialStock;
         for(int t = 0; t < demand.length; t++){
            if(inventory <= s[t]){
               replicationCost += orderCost;
               replicationCost += Math.max(0, S[t]-inventory)*unitCost;
               inventory = S[t]-demandRealizations[t];
               replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
            }else{
               inventory = inventory-demandRealizations[t];
               replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
            }
            replicationCost *= discountFactor;
            stockPTally[t].add(Math.max(inventory, 0));
            stockNTally[t].add(Math.max(-inventory, 0));
         }
         costTally.add(replicationCost);
         if(i >= minRuns) costTally.confidenceIntervalNormal(confidence, centerAndRadius);
      }
      return centerAndRadius;
   }

   public static void main(String[] args) {
      
      long seed = 4321;
      Instances instance = Instances.SAMPLE_POISSON;
      solveSampleInstance(instance, seed);
      
      //runBatchPoisson("results_poisson.csv");
      //tabulateBatchPoisson("results_poisson.csv");
   }
}

enum Instances {
   SAMPLE_POISSON
}

class InstancePortfolio{
   
   static double[][] computeDemandProbability(Instance instance) {
      double[][] demandProbability = new double [instance.demand.length][];
      for(int t = 0; t < instance.demand.length; t++) {
         if(instance.demand[t] instanceof ContinuousDistribution) {
            demandProbability[t] = tabulateProbabilityContinuous((ContinuousDistribution)instance.demand[t], instance.tail);
         }else if(instance.demand[t] instanceof DiscreteDistributionInt) {
            demandProbability[t] = tabulateProbabilityDiscrete((DiscreteDistributionInt)instance.demand[t], instance.tail);
         }else
            throw new NullPointerException("Distribution not recognized.");
      }
      return demandProbability;
   }

   private static double[] tabulateProbabilityContinuous(ContinuousDistribution dist, double tail) {
      // Note that minDemand is assumed to be 0;
      int maxDemand = (int)Math.round(dist.inverseF(1-tail));
      double[] demandProbabilities = new double[maxDemand + 1];
      for(int i = 0; i <= maxDemand; i++) {
         demandProbabilities [i] = (dist.cdf(i+0.5)-dist.cdf(i-0.5))/(dist.cdf(maxDemand+0.5)-dist.cdf(-0.5));
      }
      assert(Arrays.stream(demandProbabilities).sum() == 1);
      return demandProbabilities;
   }
   
   private static double[] tabulateProbabilityDiscrete(DiscreteDistributionInt dist, double tail) {
      // Note that minDemand is assumed to be 0;
      int maxDemand = dist.inverseFInt(1-tail);
      double[] demandProbabilities = new double[maxDemand + 1];
      for(int i = 0; i <= maxDemand; i++) {
         demandProbabilities [i] = dist.prob(i)/dist.cdf(maxDemand);
      }
      assert(Arrays.stream(demandProbabilities).sum() == 1);
      return demandProbabilities;
   }
   
   public static Instance generateSamplePoissonInstance() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -1000;
      int maxInventory = 1000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 100;
      double unitCost = 1;
      double holdingCost = 1;
      double penaltyCost = 10;
      double[] meanDemand = {20, 40, 60, 40};
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
   
      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            tail,
            minInventory,
            maxInventory
            );
      
      System.out.println(instance.toString());
      
      return instance;
   }
}

class Instance {
   /*** Problem instance ***/
   public double fixedOrderingCost;
   public double unitCost;
   public double holdingCost;
   public double penaltyCost;
   public double discountFactor = 1;
   public Distribution[] demand;
   
   /** SDP boundary conditions **/
   public double tail;
   public int maxDemand;
   public int minInventory;
   public int maxInventory;
   
   public Instance(
         double fixedOrderingCost,
         double unitCost,
         double holdingCost,
         double penaltyCost,
         double discountFactor,
         Distribution[] demand,
         double tail,
         int minInventory,
         int maxInventory) {
      this(fixedOrderingCost, unitCost, holdingCost, penaltyCost, demand, tail, minInventory, maxInventory);
      this.discountFactor = discountFactor;
   }
   
   public Instance(
         double fixedOrderingCost,
         double unitCost,
         double holdingCost,
         double penaltyCost,
         Distribution[] demand,
         double tail,
         int minInventory,
         int maxInventory) {
      this.fixedOrderingCost = fixedOrderingCost;
      this.unitCost = unitCost;
      this.holdingCost = holdingCost;
      this.penaltyCost = penaltyCost;
      this.demand = demand;
      this.tail = tail;
      this.minInventory = minInventory;
      this.maxInventory = maxInventory;
   }
   
   public int getStages() {
      return this.demand.length;
   }
   
   public int inventory(int index) {
      return index + this.minInventory;
   }
   
   public int index(int inventory) {
      return inventory - this.minInventory;
   }
   
   public int stateSpaceSize() {
      return this.maxInventory - this.minInventory + 1;
   }
   
   @Override
   public String toString() {
      return 
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Demand: "+ Arrays.toString(demand);
   }
}

class Solution {
   
   /** Explain meaning of indexes **/
   public int optimalAction[][];
   public double Gn[][];
   public double Cn[][];
   
   public Solution(int optimalAction[][], double Gn[][], double[][] Cn) {
      this.optimalAction = optimalAction;
      this.Gn = Gn;
      this.Cn = Cn;
   }
   
   
   public int[] find_S(Instance instance, int safeMin) {
      int[] S = new int[instance.getStages()];
      for(int t = 0; t < instance.getStages(); t++) {
         for(int i = safeMin - instance.minInventory; i < this.optimalAction[t].length; i++) {
            if(this.optimalAction[t][i] == 0) {
               S[t] = i-1 + this.optimalAction[t][i-1] + instance.minInventory;
               break;
            }
         }
      }
      return S;
   }
   
   public int[] find_s(Instance instance, int safeMin) {
      int[] s = new int[instance.getStages()];
      for(int t = 0; t < instance.getStages(); t++) {
         for(int i = safeMin - instance.minInventory; i < this.optimalAction[t].length; i++) {
            if(this.optimalAction[t][i] == 0) {
               s[t] = i-1 + instance.minInventory;
               break;
            }
         }
      }
      return s;
   }
}

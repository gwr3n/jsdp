package jsdp.app.standalone.stochastic.ss;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.DoubleStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jsdp.utilities.sampling.SampleFactory;
import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;
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
   
   public static double[] simulate_sS(
         Instance instance,
         Solution solution,
         int initialStock,
         int[] S, 
         int[] s, 
         double confidence,
         double error){
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
   
   /**
    * Solve an instance and returns optimal and simulated ETC
    */
   public static double[] solveInstance(Instance instance, int initialInventory, long seed) {
      
      Random rnd = new Random();
      rnd.setSeed(seed);
      
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
      double simulatedsSETC = simulate_sS(instance, solution, initialInventory, S, s, confidence, error)[0];
      
      double[] etcOut = new double[2];
      etcOut[0] = sdpETC;
      etcOut[1] = simulatedsSETC;
      return etcOut;
   }
   
   /**
    * Solve an instance and prints the solution
    */
   public static void solveSampleInstance(Instances problemInstance, long seed) {
      
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
         int[] S = solution.find_S(instance, instance.minInventory);
         int[] s = solution.find_s(instance, instance.minInventory);
         results = simulate_sS(instance, solution, initialInventory, S, s, confidence, error);
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

   public static String tabulateInstanceCSV(Instance instance, int initialInventory, int safeMin, int safeMax, boolean compact) {
      
      String out = ""+instance.fixedOrderingCost+","+instance.holdingCost+","+instance.unitCost+","+instance.penaltyCost+",";
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
   
   public static String tabulateInstanceJSON(Instance instance, int initialInventory, int safeMin, int safeMax) {
      Solution solution = sdp(instance);      
      return Gn.getJSON(new Gn(instance, solution, safeMin, safeMax));
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

   public static void tabulateBatchPoisson(String fileName, Storage store, boolean parallel){
      double tail = 0.0001;
      int minInventory = -500;
      int maxInventory = 500;
      int safeMin = -20;
      int safeMax = 400;
      
      int periods = 25;
      double[] fixedOrderingCost = DoubleStream.iterate(0, n -> n + 25).limit(10).toArray();
      double[] proportionalOrderingCost = {0, 1};
      double holdingCost = 1;
      double[] penaltyCost = DoubleStream.iterate(2, n -> n + 2).limit(10).toArray();
      
      long seed = 4321;
      Random rnd = new Random(seed);
      double[][] meanDemand = new double[10000][];
      for(int i = 0; i < meanDemand.length; i++) {
         double level = rnd.nextInt(100);
         double scale = rnd.nextDouble()*30; 
         double[] epsilon = rnd.doubles(0, 1).limit(periods).toArray(); 
         double [] Xt = new double[periods];
         Xt[0] = level + NormalDist.inverseF01(epsilon[0])*scale;
         for(int k = 1; k < periods; k++)
            Xt[k] = Xt[k-1] + NormalDist.inverseF01(epsilon[k])*scale;
         meanDemand[i] = Arrays.stream(Xt)
                               .map(k -> Math.max(k,0.1))
                               .map(k -> Math.min(k,100)).toArray();
      }
      
      int batchSize = 16;
      ExecutorService executor = Executors.newFixedThreadPool(batchSize);
      
      int instances = fixedOrderingCost.length*proportionalOrderingCost.length*penaltyCost.length*meanDemand.length;
      int count = 0;
      if(store == Storage.JSON) writeToFile(fileName, "["); 
      for(double oc : fixedOrderingCost) {
         for(double u : proportionalOrderingCost) {
            for(double p : penaltyCost) {
               if(parallel) {
                  /**
                   * Parallel
                   */
                  for (int d = 0; d < meanDemand.length; d += batchSize) {
                     List<String> results = new ArrayList<>();
                     int end = Math.min(d + batchSize, meanDemand.length);
                     
                     for (int i = d; i < end; i++) {
                        final int index = i;
                        executor.submit(() -> {
                            Distribution[] demand = Arrays.stream(meanDemand[index]).mapToObj(k -> new PoissonDist(k)).toArray(Distribution[]::new);
                            Instance instance = new Instance(oc, u, holdingCost, p, demand, tail, minInventory, maxInventory);
                            int initialInventory = 0;
   
                            String result;
                            switch (store) {
                                case CSV: {
                                    boolean compact = false;
                                    result = tabulateInstanceCSV(instance, initialInventory, safeMin, safeMax, compact);
                                }
                                break;
                                case JSON:
                                default: {
                                    result = tabulateInstanceJSON(instance, initialInventory, safeMin, safeMax);
                                    result += ",";
                                }
                            }
                            synchronized (results) {
                                results.add(result);
                            }                         
                        });
                    }
                     
                    // Wait for all tasks to complete
                    executor.shutdown();
                    try {
                        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                     
                    count += results.size();
                    System.out.println(count + "/" + instances);
                    
                    if(store == Storage.JSON && count == instances) {
                       int lastIndex = results.size() - 1;
                       String lastString = results.get(lastIndex);
                       lastString = lastString.substring(0, lastString.length() - 1);
                       results.set(lastIndex, lastString);
                    }
                     
                    for (String result : results) {
                       writeToFile(fileName, result);
                    }
                    
                    // Reinitialize the executor for the next batch
                    executor = Executors.newFixedThreadPool(batchSize);
                  }
               }else {
                  /**
                   * Sequential
                   */
                  for(int d = 0; d < meanDemand.length; d++) {
                     
                     Distribution[] demand = Arrays.stream(meanDemand[d]).mapToObj(k -> new PoissonDist(k)).toArray(Distribution[]::new);
                     
                     Instance instance = new Instance(oc, u, holdingCost, p, demand, tail, minInventory, maxInventory);
                     
                     int initialInventory = 0;
                     
                     switch(store) {
                        case CSV: {
                           boolean compact = false;
                           String result = tabulateInstanceCSV(instance, initialInventory, safeMin, safeMax, compact);
                           writeToFile(fileName, result);
                        }
                        break;
                        case JSON: 
                        default: {
                           String result = tabulateInstanceJSON(instance, initialInventory, safeMin, safeMax);
                           writeToFile(fileName, result + ((count == instances - 1) ? "" : ","));
                        }
                     }
                           
                     System.out.println((++count)+"/"+instances);
                  }
               }
            }
         }
      }
      if(store == Storage.JSON) writeToFile(fileName, "]"); 
   }
   
   public static void main(String[] args) {
      
      //long seed = 4321;
      //Instances instance = Instances.SAMPLE_POISSON;
      //solveSampleInstance(instance, seed);
      
      boolean parallel = true;
      tabulateBatchPoisson("batch_poisson.json", Storage.JSON, parallel);
   }
}

enum Storage {
   CSV, JSON
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

@SuppressWarnings("unused")
class Gn {
   private double K;
   private double h;
   private double v;
   private double p;
   private double[] d;
   private double[] Gn;
   private double s;
   private double S;
   
   public Gn(Instance instance, Solution solution, int safeMin, int safeMax) {
      this.K = instance.fixedOrderingCost;
      this.h = instance.holdingCost;
      this.v = instance.unitCost;
      this.p = instance.penaltyCost;
      this.d = Arrays.stream(instance.demand).mapToDouble(d -> d.getMean()).toArray();
      this.Gn = Arrays.copyOfRange(solution.Gn[0], safeMin - instance.minInventory, safeMax - instance.minInventory);
      this.s = solution.find_s(instance, safeMin)[0];
      this.S = solution.find_S(instance, safeMin)[0];
   }
   
   public static String getJSON(Object object) {
      GsonBuilder gsonBuilder = new GsonBuilder();
      Gson gson = gsonBuilder.create();
      return gson.toJson(object);
   }
}




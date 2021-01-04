package jsdp.app.standalone.stochastic.lateraltransshipment;

import java.util.Arrays;

import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;

public class LateralTransshipment {
   
   static double[] tabulateProbabilityContinuous(ContinuousDistribution dist, double tail) {
      // Note that minDemand is assumed to be 0;
      int maxDemand = (int)Math.round(dist.inverseF(1-tail));
      double[] demandProbabilities = new double[maxDemand + 1];
      for(int i = 0; i <= maxDemand; i++) {
         demandProbabilities [i] = (dist.cdf(i+0.5)-dist.cdf(i-0.5))/(dist.cdf(maxDemand+0.5)-dist.cdf(-0.5));
      }
      assert(Arrays.stream(demandProbabilities).sum() == 1);
      return demandProbabilities;
   }
   
   static double[] tabulateProbabilityDiscrete(DiscreteDistributionInt dist, double tail) {
      // Note that minDemand is assumed to be 0;
      int maxDemand = dist.inverseFInt(1-tail);
      double[] demandProbabilities = new double[maxDemand + 1];
      for(int i = 0; i <= maxDemand; i++) {
         demandProbabilities [i] = dist.prob(i)/dist.cdf(maxDemand);
      }
      assert(Arrays.stream(demandProbabilities).sum() == 1);
      return demandProbabilities;
   }
   
   static double[][][] computeDemandProbability(Instance instance) {
      double[][][] demandProbability = new double [2][instance.getStages()][];
      for(int t = 0; t < instance.getStages(); t++) {
         if(instance.demandA[t] instanceof ContinuousDistribution) {
            demandProbability[0][t] = tabulateProbabilityContinuous((ContinuousDistribution)instance.demandA[t], instance.tail);
         }else if(instance.demandA[t] instanceof DiscreteDistributionInt) {
            demandProbability[0][t] = tabulateProbabilityDiscrete((DiscreteDistributionInt)instance.demandA[t], instance.tail);
         }else
            throw new NullPointerException("Distribution not recognized.");
         
         if(instance.demandB[t] instanceof ContinuousDistribution) {
            demandProbability[1][t] = tabulateProbabilityContinuous((ContinuousDistribution)instance.demandB[t], instance.tail);
         }else if(instance.demandB[t] instanceof DiscreteDistributionInt) {
            demandProbability[1][t] = tabulateProbabilityDiscrete((DiscreteDistributionInt)instance.demandB[t], instance.tail);
         }else
            throw new NullPointerException("Distribution not recognized.");
      }
      return demandProbability;
   }
   
   static double computeImmediateEndOfPeriodCost(
         int iA,
         int iB,
         int QA,
         int QB,
         int demandA,
         int demandB,
         double hA,
         double hB,
         double pA,
         double pB) {
      double costA = 
            hA*Math.max(0, iA + QA - demandA) +
            pA*Math.max(0, demandA - iA - QA);
      double costB = 
            hB*Math.max(0, iB + QB - demandB) +
            pB*Math.max(0, demandB - iB - QB);
      return costA + costB;
   }
   
   static double getOptimalCost(double[][] expectedTotalCosts) {
      double min = expectedTotalCosts[0][0];
      for(int a = 0; a < expectedTotalCosts.length; a++) {
         for(int b = 0; b < expectedTotalCosts.length; b++) {
            if(expectedTotalCosts[a][b] < min) {
               min = expectedTotalCosts[a][b];
            }
         }
      }
      return min;
   }
   
   static int[] getOptimalAction(double[][] expectedTotalCosts) {
      double min = expectedTotalCosts[0][0];
      int[] action = new int[2];
      for(int a = 0; a < expectedTotalCosts.length; a++) {
         for(int b = 0; b < expectedTotalCosts.length; b++) {
            if(expectedTotalCosts[a][b] < min) {
               min = expectedTotalCosts[a][b];
               action = new int[]{a,b};
            }
         }
      }
      return action;
   }
   
   static double getOptimalCost(double[] expectedTotalCosts) {
      double min = expectedTotalCosts[0];
      for(int a = 1; a < expectedTotalCosts.length; a++) {
         if(expectedTotalCosts[a] < min) {
            min = expectedTotalCosts[a];
         }
      }
      return min;
   }
   
   static int getOptimalAction(double[] expectedTotalCosts) {
      double min = expectedTotalCosts[0];
      int action = 0;
      for(int a = 1; a < expectedTotalCosts.length; a++) {
         if(expectedTotalCosts[a] < min) {
            min = expectedTotalCosts[a];
            action = a;
         }
      }
      return action;
   }
   
   public static Solution solveInstance(Instance instance) {
      
      double demandProbabilities [][][] = computeDemandProbability(instance);
      
      int optimalActionOrder[][][][] = new int [instance.getStages()][instance.stateSpaceSize()][instance.stateSpaceSize()][];
      double GnOrder[][][] = new double [instance.getStages()][instance.stateSpaceSize()][instance.stateSpaceSize()];
      double CnOrder[][][] = new double [instance.getStages()][instance.stateSpaceSize()][instance.stateSpaceSize()];
      
      int optimalActionTransshipment[][][] = new int [instance.getStages()][instance.stateSpaceSize()][instance.stateSpaceSize()];
      double GnTransshipment[][][] = new double [instance.getStages()][instance.stateSpaceSize()][instance.stateSpaceSize()];
      double CnTransshipment[][][] = new double [instance.getStages()][instance.stateSpaceSize()][instance.stateSpaceSize()];
      
      /** Compute Expected Cost **/
      
      for(int t = instance.getStages()-1; t >= 0; t--) {                               // Time
         
         // Orders last
         double totalCostO[][][][] = new double [instance.stateSpaceSize()][instance.stateSpaceSize()][instance.stateSpaceSize()+1][instance.stateSpaceSize()+1];
         for(int iA = 0; iA < instance.stateSpaceSize(); iA++) {                          // Inventory A
            for(int iB = 0; iB < instance.stateSpaceSize(); iB++) {                       // Inventory B
               for(int QA = 0; QA <= instance.stateSpaceSize(); QA++) {                   // Order A
                  for(int QB = 0; QB <= instance.stateSpaceSize(); QB++) {                // Order B
                     totalCostO[iA][iB][QA][QB] += (QA > 0) ? instance.KA + instance.vA * QA : 0;
                     totalCostO[iA][iB][QA][QB] += (QB > 0) ? instance.KB + instance.vB * QB : 0;
                     double totalProbabilityMass = 0;
                     for(int dA = 0; dA < demandProbabilities[0][t].length; dA++) {       // Demand A
                        for(int dB = 0; dB < demandProbabilities[1][t].length; dB++) {    // Demand B
                           double immediateCost = 0;
                           double futureCost = 0;
                           if(
                                 (instance.inventory(iA) + QA - dA <= instance.maxInventory) && (instance.inventory(iA) + QA - dA >= instance.minInventory) &&
                                 (instance.inventory(iB) + QB - dB <= instance.maxInventory) && (instance.inventory(iB) + QB - dB >= instance.minInventory)) {
                              immediateCost = demandProbabilities[0][t][dA]*demandProbabilities[1][t][dB]*
                                    computeImmediateEndOfPeriodCost(instance.inventory(iA),instance.inventory(iB), QA, QB, dA, dB, instance.hA, instance.hB, instance.pA, instance.pB);
                              futureCost = demandProbabilities[0][t][dA]*demandProbabilities[0][t][dB]*( (t==instance.getStages()-1) ? 0 : CnTransshipment[t+1][iA+QA-dA][iB+QB-dB]);
                              totalProbabilityMass += demandProbabilities[0][t][dA]*demandProbabilities[1][t][dB];
                           }
                           totalCostO[iA][iB][QA][QB] += immediateCost + futureCost;
                        }
                     }
                     totalCostO[iA][iB][QA][QB]/=totalProbabilityMass;
                  }
               }
               GnOrder[t][iA][iB] = totalCostO[iA][iB][0][0];
               CnOrder[t][iA][iB] = getOptimalCost(totalCostO[iA][iB]);
               optimalActionOrder[t][iA][iB] = getOptimalAction(totalCostO[iA][iB]);
            }
         }
         
         // Then transshipment
         double totalCostT[][][] = new double [instance.stateSpaceSize()][instance.stateSpaceSize()][instance.stateSpaceSize()+1];
         for(int iA = 0; iA < instance.stateSpaceSize(); iA++) {                          // Inventory A
            for(int iB = 0; iB < instance.stateSpaceSize(); iB++) {                       // Inventory B
               for(int T = 0; T <= instance.stateSpaceSize(); T++) {                      // Transship
                  if(instance.inventory(T) == 0) {
                     GnTransshipment[t][iA][iB] = GnOrder[t][iA][iB];
                     totalCostT[iA][iB][T] = CnOrder[t][iA][iB];
                  } else {
                     totalCostT[iA][iB][T] += instance.R + instance.u * Math.abs(instance.inventory(T));
                     double futureCost = 0;
                     if(instance.inventory(T) > 0 && instance.inventory(T) > instance.inventory(iA))
                        totalCostT[iA][iB][T] = Double.MAX_VALUE;
                     else if(instance.inventory(T) < 0 && -instance.inventory(T) > instance.inventory(iB))
                        totalCostT[iA][iB][T] = Double.MAX_VALUE;
                     else if(
                           (instance.inventory(iA) - instance.inventory(T) <= instance.maxInventory) && (instance.inventory(iA) - instance.inventory(T) >= instance.minInventory) &&
                           (instance.inventory(iB) + instance.inventory(T) <= instance.maxInventory) && (instance.inventory(iB) + instance.inventory(T) >= instance.minInventory)
                           ) {
                        futureCost = CnOrder[t][instance.index(instance.inventory(iA) - instance.inventory(T))][instance.index(instance.inventory(iB) + instance.inventory(T))];
                        totalCostT[iA][iB][T] += futureCost;
                     }else {
                        totalCostT[iA][iB][T] = Double.MAX_VALUE;
                     }           
                  }
               }
               CnTransshipment[t][iA][iB] = getOptimalCost(totalCostT[iA][iB]);
               optimalActionTransshipment[t][iA][iB] = instance.inventory(getOptimalAction(totalCostT[iA][iB]));
            }
         }
      }
      return new Solution(optimalActionOrder, optimalActionTransshipment, GnTransshipment, GnOrder, CnTransshipment, CnOrder);
   }
   
   public static void printSolution(Instance instance, Solution solution) {
      int t = 0;
      System.out.println("Expected total cost with zero initial inventory: "+(solution.CnTransshipment[t][-instance.minInventory][-instance.minInventory]));
      
      System.out.print("\t");
      for(int j = 0; j < instance.stateSpaceSize(); j++) {
         System.out.print(instance.inventory(j) + "\t");
      }
      System.out.println();
      for(int i = 0; i < instance.stateSpaceSize(); i++) {
         System.out.print(instance.inventory(i) + "\t");
         for(int j = 0; j < instance.stateSpaceSize(); j++) {
            System.out.print(solution.GnTransshipment[0][i][j] + "\t");
         }
         System.out.println();
      }
      
      System.out.println();
      
      System.out.print("\t");
      for(int j = 0; j < instance.stateSpaceSize(); j++) {
         System.out.print(instance.inventory(j) + "\t");
      }
      System.out.println();
      for(int i = 0; i < instance.stateSpaceSize(); i++) {
         System.out.print(instance.inventory(i) + "\t");
         for(int j = 0; j < instance.stateSpaceSize(); j++) {
            System.out.print(solution.optimalActionTransshipment[0][i][j] + "\t");
         }
         System.out.println();
      }
      
      System.out.println();
      
      System.out.print("\t");
      for(int j = 0; j < instance.stateSpaceSize(); j++) {
         System.out.print(instance.inventory(j) + "\t");
      }
      System.out.println();
      for(int i = 0; i < instance.stateSpaceSize(); i++) {
         System.out.print(instance.inventory(i) + "\t");
         for(int j = 0; j < instance.stateSpaceSize(); j++) {
            System.out.print(solution.optimalActionOrder[0][i][j][0] + "\t");
         }
         System.out.println();
      }
      
      System.out.println();
      
      System.out.print("\t");
      for(int j = 0; j < instance.stateSpaceSize(); j++) {
         System.out.print(instance.inventory(j) + "\t");
      }
      System.out.println();
      for(int i = 0; i < instance.stateSpaceSize(); i++) {
         System.out.print(instance.inventory(i) + "\t");
         for(int j = 0; j < instance.stateSpaceSize(); j++) {
            System.out.print(solution.optimalActionOrder[0][i][j][1] + "\t");
         }
         System.out.println();
      }
   }
   
   public static void solveSampleInstance(Instances problemInstance) {      
      Instance instance; 
      switch(problemInstance) {
      case SAMPLE_POISSON:
      default:
         instance = InstancePortfolio.generateSamplePoissonInstance();
         break;
      }
      
      Solution solution = solveInstance(instance);
      
      System.out.println();
      printSolution(instance, solution);
   }
   
   public static void main(String[] args) {
      Instances instance = Instances.SAMPLE_POISSON;
      solveSampleInstance(instance);
   }
}

enum Instances {
   SAMPLE_POISSON
}

class InstancePortfolio{
   public static Instance generateSamplePoissonInstance() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -20;
      int maxInventory = 30;
      
      /*** Problem instance ***/
      double K = 20;
      double v = 1;  
      double h = 0.25;
      double p = 5;
      
      double KA = K;
      double vA = v;
      double KB = K;
      double vB = v;
      double R = 0;
      double u = 0.5;
      double hA = h;
      double hB = h;
      double pA = p;
      double pB = p;
      
      double[] meanDemandA = {4,6,8,6};
      Distribution[] demandA = Arrays.stream(meanDemandA).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      double[] meanDemandB = {4,6,8,6};
      Distribution[] demandB = Arrays.stream(meanDemandB).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      
      System.out.println(
            "Fixed ordering: "+K+"\n"+
            "Proportional ordering: "+v+"\n"+
            "Fixed transshipment: "+R+"\n"+
            "Proportional transshipment: "+u+"\n"+      
            "Holding cost: "+h+"\n"+
            "Penalty cost: "+p+"\n"+
            "Demand: "+ Arrays.toString(meanDemandA)+"\n"+
            "Demand: "+ Arrays.toString(meanDemandB));
   
   
      Instance instance = new Instance(
            KA,
            vA,
            KB,
            vB,
            R,
            u,
            hA,
            hB,
            pA,
            pB,
            demandA,
            demandB,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
}

class Instance {
   /*** Problem instance ***/
   public double KA; // fixed ordering cost installation A
   public double vA; // unit ordering cost installation A
   public double KB; // fixed ordering cost installation B
   public double vB; // unit ordering cost installation B
   public double R; // fixed transshipment cost
   public double u; // unit transshipment cost
   public double hA; // holding cost installation A
   public double hB; // holding cost installation B
   public double pA; // penalty cost installation A
   public double pB; // penalty cost installation B
   public Distribution[] demandA;
   public Distribution[] demandB;
   
   /** SDP boundary conditions **/
   public double tail;
   public int maxDemand;
   public int minInventory;
   public int maxInventory;
   
   public Instance(
         double KA,
         double vA,
         double KB,
         double vB,
         double R,
         double u,
         double hA,
         double hB,
         double pA,
         double pB,
         Distribution[] demandA,
         Distribution[] demandB,
         double tail,
         int minInventory,
         int maxInventory) {
      this.KA = KA;
      this.vA = vA;
      this.KB = KB;
      this.vB = vB;
      this.R = R;
      this.u = u;
      this.hA = hA;
      this.hB = hB;
      this.pA = pA;
      this.pB = pB;
      this.demandA = demandA;
      this.demandB = demandB;
      this.tail = tail;
      this.minInventory = minInventory;
      this.maxInventory = maxInventory;
   }
   
   public int getStages() {
      assert(this.demandA.length == this.demandB.length);
      return this.demandA.length;
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
}

class Solution {
   
   /** Explain meaning of indexes **/
   public int optimalActionOrder[][][][]; // t, iA, iB, QA, QB
   public int optimalActionTransshipment[][][]; // t, iA, iB, T
   public double GnOrder[][][]; // t, iA, iB, cost after orders
   public double GnTransshipment[][][]; // t, iA, iB, cost after orders
   public double CnOrder[][][]; // t, iA, iB, cost after transshipment
   public double CnTransshipment[][][]; // t, iA, iB, cost after transshipment
   
   public Solution(
         int optimalActionOrder[][][][],
         int optimalActionTransshipment[][][], 
         double GnTransshipment[][][], 
         double GnOrder[][][], 
         double[][][] CnTransshipment,
         double[][][] CnOrder) {
      this.optimalActionOrder = optimalActionOrder;
      this.optimalActionTransshipment = optimalActionTransshipment;
      this.GnTransshipment = GnTransshipment;
      this.GnOrder = GnOrder;
      this.CnTransshipment = CnTransshipment;
      this.CnOrder = CnOrder;
   }
}
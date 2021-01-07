package jsdp.app.standalone.stochastic.capacitated;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;

import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.GammaDist;

import umontreal.ssj.stat.Tally;

import jsdp.utilities.sampling.SampleFactory;

public class CapacitatedStochasticLotSizingFast {

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
   
   static double[] tabulateProbabilityRandom(double maxDemand) {
      double[] demandProbabilities = new double[(int)Math.round(maxDemand) + 1];
      Random rnd = new Random();
      double quantiles = 1000;
      for(int i = 0; i < quantiles; i++) {
         demandProbabilities[rnd.nextInt((int)Math.round(maxDemand) + 1)] += 1/quantiles;
      }
      assert(Arrays.stream(demandProbabilities).sum() == 1);
      return demandProbabilities;
   }
   
   static double[] tabulateProbabilitySparseRandom(double maxDemand, double capacity) {
      double[] demandProbabilities = new double[(int)Math.round(maxDemand) + 1];
      Random rnd = new Random();
      int C = (int)Math.round(capacity);
      int D = (int)Math.round(maxDemand);
      demandProbabilities[(D - C > 0 ? rnd.nextInt(D - C) : 0) + C] += rnd.nextDouble();
      demandProbabilities[rnd.nextInt(C + 1)] += (1 - Arrays.stream(demandProbabilities).sum())*rnd.nextDouble();
      demandProbabilities[rnd.nextInt(C + 1)] += (1 - Arrays.stream(demandProbabilities).sum())*rnd.nextDouble();
      demandProbabilities[(D - C > 0 ? rnd.nextInt(D - C) : 0) + C] += (1 - Arrays.stream(demandProbabilities).sum())*rnd.nextDouble();
      demandProbabilities[(D - C > 0 ? rnd.nextInt(D - C) : 0) + C] += 1 - Arrays.stream(demandProbabilities).sum();
      assert(Arrays.stream(demandProbabilities).sum() == 1);
      String out = "";
      for(int i = 0; i < demandProbabilities.length; i++) {
         if(demandProbabilities[i] > 0) out += i+"/"+demandProbabilities[i]+",";
      }
      System.out.println(out);
      return demandProbabilities;
   }
   
   static double[] tabulateProbabilityShiaoxiang1996() {
      return new double[] {0.00001,0.00001,0.00001,0.00001,0.00001,0.00001,0.94994,0.05};
   }
   
   static double[] tabulateProbabilityGallego2000() {
      return new double[] {0.00001,0.15,0.00001,0.00001,0.00001,0.00001,0.69995,0.15};
   }
   
   static double[] tabulateProbabilityNoOrderOrder_1(int t) {
      String distribution = "107/0.28804, 215/0.40838, 260/0.30358,\n"
            + "53/0.27762, 174/0.00283, 199/0.00202, 245/0.71753,\n"
            + "4/0.10204, 54/0.00752, 116/0.04826, 145/0.84218,\n"
            + "59/0.29474, 131/0.31910, 136/0.38616,\n"
            + "64/0.13203, 281/0.75405, 284/0.11392,\n"
            + "137/0.75736, 153/0.02654, 197/0.21610,\n"
            + "114/0.60165, 127/0.29033, 174/0.08904, 275/0.01898,\n"
            + "209/0.76446, 258/0.11859, 276/0.11695,\n"
            + "41/0.14676, 135/0.28086, 174/0.13442, 263/0.43796,\n"
            + "88/0.09797, 131/0.76541, 233/0.01452, 287/0.12210,\n"
            + "201/0.07041, 209/0.84693, 281/0.08266,\n"
            + "50/0.01981, 132/0.04819, 289/0.93200,";
      String[] periodDemand = distribution.split("\n");
      String[] demandDistribution = periodDemand[t].split(",");
      String[][] demandDistributionArray = Arrays.stream(demandDistribution).map(s -> s.split("/")).toArray(String[][]::new);
      double[][] demandDistributionDoubleArray = Arrays.stream(demandDistributionArray).limit(demandDistributionArray.length).map(s -> new double[]{Double.parseDouble(s[0]), Double.parseDouble(s[1])}).toArray(double[][]::new);
      double maxDemand = demandDistributionDoubleArray[demandDistributionDoubleArray.length - 1][0];
      double[] res = new double[(int)Math.round(maxDemand+1)];
      for(int i = 0; i < demandDistributionDoubleArray.length; i++) {
         res[(int)Math.round(demandDistributionDoubleArray[i][0])] = demandDistributionDoubleArray[i][1];
      }
      assert(Arrays.stream(res).sum() == 1);
      return res;
   }
   
   static double[] tabulateProbabilityNoOrderOrder_2(int t) {
      String distribution = "45/0.03671, 77/0.13654, 123/0.78596, 268/0.04079,\n"
            + "70/0.71222, 111/0.25824, 158/0.02954,\n"
            + "112/0.28034, 216/0.68764, 221/0.03202,\n"
            + "50/0.04864, 228/0.59748, 246/0.35388,\n"
            + "22/0.17562, 34/0.27540, 49/0.42421, 137/0.12477,\n"
            + "13/0.66304, 226/0.23036, 230/0.10660,\n"
            + "102/0.20559, 152/0.33795, 291/0.45646,\n"
            + "27/0.00900, 112/0.00469, 153/0.04761, 293/0.93870,\n"
            + "9/0.04179, 91/0.87259, 105/0.00064, 227/0.08498,\n"
            + "31/0.00462, 32/0.05346, 196/0.93892, 292/0.00300,\n"
            + "113/0.19172, 129/0.46507, 134/0.34321,\n"
            + "6/0.02876, 8/0.07738, 66/0.04847, 246/0.84539,";
      String[] periodDemand = distribution.split("\n");
      String[] demandDistribution = periodDemand[t].split(",");
      String[][] demandDistributionArray = Arrays.stream(demandDistribution).map(s -> s.split("/")).toArray(String[][]::new);
      double[][] demandDistributionDoubleArray = Arrays.stream(demandDistributionArray).limit(demandDistributionArray.length).map(s -> new double[]{Double.parseDouble(s[0]), Double.parseDouble(s[1])}).toArray(double[][]::new);
      double maxDemand = demandDistributionDoubleArray[demandDistributionDoubleArray.length - 1][0];
      double[] res = new double[(int)Math.round(maxDemand+1)];
      for(int i = 0; i < demandDistributionDoubleArray.length; i++) {
         res[(int)Math.round(demandDistributionDoubleArray[i][0])] = demandDistributionDoubleArray[i][1];
      }
      assert(Arrays.stream(res).sum() == 1);
      return res;
   }
   
   static double[][] computeDemandProbability(Instance instance) {
      double[][] demandProbability = new double [instance.demand.length][];
      for(int t = 0; t < instance.demand.length; t++) {
         if(instance.demand[t] instanceof RandomDist) {
            demandProbability[t] = tabulateProbabilityRandom(((RandomDist)instance.demand[t]).maxDemand);
         }else if(instance.demand[t] instanceof SparseRandomDist) {
            demandProbability[t] = tabulateProbabilitySparseRandom(((SparseRandomDist)instance.demand[t]).maxDemand, instance.maxQuantity);
         }else if(instance.demand[t] instanceof Shiaoxiang1996Dist) {
            demandProbability[t] = tabulateProbabilityShiaoxiang1996();
         }else if(instance.demand[t] instanceof Gallego2000Dist) {
            demandProbability[t] = tabulateProbabilityGallego2000();
         }else if(instance.demand[t] instanceof NoOrderOrder1Dist) {
            demandProbability[t] = tabulateProbabilityNoOrderOrder_1(t);
         }else if(instance.demand[t] instanceof NoOrderOrder2Dist) {
            demandProbability[t] = tabulateProbabilityNoOrderOrder_2(t);
         }else if(instance.demand[t] instanceof ContinuousDistribution) {
            demandProbability[t] = tabulateProbabilityContinuous((ContinuousDistribution)instance.demand[t], instance.tail);
         }else if(instance.demand[t] instanceof DiscreteDistributionInt) {
            demandProbability[t] = tabulateProbabilityDiscrete((DiscreteDistributionInt)instance.demand[t], instance.tail);
         }else
            throw new NullPointerException("Distribution not recognized.");
      }
      return demandProbability;
   }

   static double computeImmediateCost(
         int inventory, 
         int quantity, 
         int demand,
         double holdingCost, 
         double penaltyCost, 
         double fixedOrderingCost, 
         double unitCost) {
      return holdingCost*Math.max(0, inventory + quantity - demand) 
            + penaltyCost *Math.max(0, demand - inventory - quantity);
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
   
   enum FUNCTION {
      Gn, Cn
   }
   
   public static Solution solveInstance(Instance instance) {
      
      double demandProbabilities [][] = computeDemandProbability(instance);
      
      int optimalAction[][] = new int [instance.getStages()][instance.stateSpaceSize()];
      double Gn[][] = new double [instance.getStages()][instance.stateSpaceSize()];
      double Cn[][] = new double [instance.getStages()][instance.stateSpaceSize()];
      
      /** Compute Expected Cost **/
      
      for(int t = instance.getStages()-1; t >= 0; t--) {                               // Time
         double totalCost[][] = new double [instance.stateSpaceSize()][instance.maxQuantity+1];
         for(int i = 0; i < instance.stateSpaceSize(); i++) {                          // Inventory
            for(int a = 0; a <= instance.maxQuantity; a++) {                           // Actions
               totalCost[i][a] += (a > 0) ? instance.fixedOrderingCost + instance.unitCost * a : 0;
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
         }
      }
      return new Solution(optimalAction, Gn, Cn, instance.maxQuantity);
   }
   
   public static void printSolution(Instance instance, Solution solution, int safeMin) {
      int[] S = solution.find_S(instance, safeMin);
      int t = 0;
      System.out.println("Expected total cost at Sm: "+solution.Cn[t][S[t]-instance.minInventory]);
      System.out.println("Expected total cost with zero initial inventory: "+(solution.Cn[t][-instance.minInventory]));
      
   }
   
   public static void printPolicy(Instance instance, Solution solution, int safeMin) {
      int[][] Sk = solution.find_Sk(instance, safeMin);
      int[][] sk = solution.find_sk(instance, safeMin);
      System.out.println("Policy");
      System.out.println("sk\tSk");
      for(int t =0; t < instance.getStages(); t++) {
         System.out.println("--- Period "+t+" ---");
         for(int k = 0; k < Sk[t].length; k++) {
            System.out.println(sk[t][k]+"\t"+Sk[t][k]);
         }
      }
   }
   
   public static void plotCostFunction(Instance instance, Solution solution, int min, int max, boolean plot, FUNCTION f, int period) {
      double[][] optimalCost;
      switch(f) {
      case Gn:
         optimalCost = solution.Gn;
         break;
      case Cn:
      default:
         optimalCost = solution.Cn;
      }
      
      int t = period;
      
      /** Plot the expected optimal cost **/
      XYSeries series = new XYSeries(f == FUNCTION.Gn ? "Gn" : "Cn");
      //XYSeries seriesRQCE = new XYSeries("Right QCE");
      //XYSeries seriesLQCE = new XYSeries("Left QCE");
      String cost = "cost"+(f == FUNCTION.Gn ? "Gn" : "Cn")+" = {";
      //String lqce = "lqce"+(f == FUNCTION.Gn ? "Gn" : "Cn")+" = {";
      String order_up_to = "order_up_to = {";
      String action = "action = {";
      for(int i = Math.max(0,min-instance.minInventory); i < Math.min(instance.stateSpaceSize(),max-instance.minInventory); i++) {
         series.add(i+instance.minInventory,optimalCost[t][i]);
         //seriesRQCE.add(i+instance.minInventory, f == FUNCTION.Gn ? solution.rightQuasiconvexEnvelopeGn[t][i] : solution.rightQuasiconvexEnvelopeCn[t][i]);
         //seriesLQCE.add(i+instance.minInventory, f == FUNCTION.Gn ? solution.leftQuasiconvexEnvelopeGn[t][i] : solution.leftQuasiconvexEnvelopeCn[t][i]);
         cost += "{" + (i+instance.minInventory) + ", " + (optimalCost[t][i]) + "}" + 
               ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
         //lqce += "{" + (i+instance.minInventory) + ", " + (f == FUNCTION.Gn ? solution.leftQuasiconvexEnvelopeGn[t][i] : solution.leftQuasiconvexEnvelopeCn[t][i]) + "}" + 
         //      ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
         order_up_to += "{" + (i+instance.minInventory) + ", " + (i+instance.minInventory+solution.optimalAction[t][i]) + "}" + 
               ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
         action += "{" + (i+instance.minInventory) + ", " + (solution.optimalAction[t][i]) + "}" + 
               ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
      }
      cost += "};";
      //lqce += "};";
      order_up_to += "};";
      action += "};";
      System.out.println(cost);
      //System.out.println(lqce);
      System.out.println(order_up_to);
      System.out.println(action);
      
      /** Plot the expected optimal cost **/
      if(plot) {
         XYSeriesCollection xyDataset = new XYSeriesCollection();
         xyDataset.addSeries(series);
         //xyDataset.addSeries(seriesRQCE);
        // xyDataset.addSeries(seriesLQCE);
         JFreeChart chart = ChartFactory.createXYLineChart((f == FUNCTION.Gn ? "Gn" : "Cn"), "Opening inventory level", "Expected total cost",
               xyDataset, PlotOrientation.VERTICAL, false, true, false);
         ChartFrame frame = new ChartFrame((f == FUNCTION.Gn ? "Gn" : "Cn"),chart);
         frame.setVisible(true);
         frame.setSize(500,400);
      }
   }
   
   public static void plotCnMinusGn(Instance instance, Solution solution, int min, int max, boolean plot) {
      
      /** Plot the expected optimal cost **/
      XYSeries series = new XYSeries("Cn-Gn");
      XYSeries series1 = new XYSeries("K+min_y Gn(y)-Gn(x)");
      String cost = "costCnMinusGn = {";
      for(int i = Math.max(0,min-instance.minInventory); i < Math.min(instance.stateSpaceSize(),max-instance.minInventory); i++) {
         // Cn-Gn
         series.add(i+instance.minInventory,solution.Cn[0][i]-solution.Gn[0][i]);
         
         @SuppressWarnings("unused")
         double step1 = instance.fixedOrderingCost+Arrays.stream(solution.Cn[0],i,Math.min(i+instance.maxQuantity+1, solution.Cn[0].length)).min().getAsDouble()-solution.Cn[0][i];
         
         double step2 = Double.MAX_VALUE;
         for(int y = i; y < Math.min(i+instance.maxQuantity+1, solution.Cn[0].length); y++) {
            double Vky = Math.min(solution.Gn[0][y], instance.fixedOrderingCost+Arrays.stream(solution.Gn[0],y,Math.min(y+instance.maxQuantity+1, solution.Gn[0].length)).min().getAsDouble());
            double Vki = Math.min(solution.Gn[0][i], instance.fixedOrderingCost+Arrays.stream(solution.Gn[0],i,Math.min(i+instance.maxQuantity+1, solution.Gn[0].length)).min().getAsDouble());
            
            double temp = instance.fixedOrderingCost + Vky - Vki;
            step2 = Math.min(step2, temp);
         }
         
         double step3 = Double.MAX_VALUE;
         for(int y = i; y < Math.min(i+instance.maxQuantity+1, solution.Cn[0].length); y++) {
            double Vky = solution.Gn[0][y] + Math.min(0, instance.fixedOrderingCost+Arrays.stream(solution.Gn[0],y,Math.min(y+instance.maxQuantity+1, solution.Gn[0].length)).min().getAsDouble()-solution.Gn[0][y]);
            double Vki = solution.Gn[0][i] + Math.min(0, instance.fixedOrderingCost+Arrays.stream(solution.Gn[0],i,Math.min(i+instance.maxQuantity+1, solution.Gn[0].length)).min().getAsDouble()-solution.Gn[0][i]);
            
            double temp = instance.fixedOrderingCost + Vky - Vki;
            step3 = Math.min(step3, temp);
         }
         
         double step4 = Double.MAX_VALUE;
         int inventoryLevel = Integer.MIN_VALUE; // Switch verbose check of proof step
         for(int y = i; y < Math.min(i+instance.maxQuantity+1, solution.Cn[0].length); y++) {
            double Vky = Math.min(0, instance.fixedOrderingCost+Arrays.stream(solution.Gn[0],y,Math.min(y+instance.maxQuantity+1, solution.Gn[0].length)).min().getAsDouble()-solution.Gn[0][y]);
            double Vki = Math.min(0, instance.fixedOrderingCost+Arrays.stream(solution.Gn[0],i,Math.min(i+instance.maxQuantity+1, solution.Gn[0].length)).min().getAsDouble()-solution.Gn[0][i]);
            double temp = instance.fixedOrderingCost + solution.Gn[0][y] - solution.Gn[0][i] - Vki + Vky;
            if(i+instance.minInventory == inventoryLevel) {
               System.out.println(y+instance.minInventory + " " +temp);
               System.out.println("K+Gn(y)-Gn(x) " +(instance.fixedOrderingCost + solution.Gn[0][y] - solution.Gn[0][i]));
               System.out.println("Vky " +Vky);
               System.out.println("Vki " +Vki);
               System.out.println();
            }
            step4 = Math.min(step4, temp);
         }
         
         series1.add(i+instance.minInventory,step4);
         cost += "{" + (i+instance.minInventory) + ", " + (solution.Cn[0][i]-solution.Gn[0][i]) + "}" + 
               ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
      }
      cost += "};";
      System.out.println(cost);
      
      /** Plot the expected optimal cost **/
      if(plot) {
         XYSeriesCollection xyDataset = new XYSeriesCollection();
         xyDataset.addSeries(series);
         xyDataset.addSeries(series1);
         JFreeChart chart = ChartFactory.createXYLineChart("Cn-Gn", "Opening inventory level", "Expected total cost",
               xyDataset, PlotOrientation.VERTICAL, false, true, false);
         ChartFrame frame = new ChartFrame("Cn-Gn",chart);
         frame.setVisible(true);
         frame.setSize(500,400);
      }
   }
   
   /************** TESTS ****************/
   
   /**
    * Test a tighter version of CKi than the one presented in 
    * 
    * Gallego G, Scheller-Wolf A (2000) Capacitated inventory problems with fixed order costs: 
    * Some optimal policy structure. European Journal of Operational Research 126(3):603-613.
    * 
    * K+g(x+a)-g(x)-a(g(y+b)-g(y))/b>=0, where y<=x+a, 0<a<=B, 0<b<=a.
    */
   public static boolean testKBConvexity(Instance instance, Solution solution, int min, int max, FUNCTION f) {
      boolean flag = true;
      
      double[][] optimalCost;
      switch(f) {
      case Gn:
         optimalCost = solution.Gn;
         break;
      case Cn:
      default:
         optimalCost = solution.Cn;
      }
      
      int t = 0;
      for(int x = max-instance.maxQuantity; x >= min; x--) {
         for(int y = x; y >= min + instance.maxQuantity; y--) {
            for(int a = 1; a <= instance.maxQuantity; a++) {
               for(int b = 1; b <= instance.maxQuantity; b++) { 
                  double gx = optimalCost[t][x - instance.minInventory];
                  double gxa = optimalCost[t][x + a - instance.minInventory];
                  double gy = optimalCost[t][y - instance.minInventory];
                  double gyb = optimalCost[t][y - b - instance.minInventory];
                  double delta = 0.0000000001;
      
                  if(instance.fixedOrderingCost + gxa - gx - a*(gy-gyb)/b + delta < 0) {
                     System.out.println("K: "+instance.fixedOrderingCost);
                     System.out.println("B: "+instance.maxQuantity);
                     System.out.println("x: "+x);
                     System.out.println("a: "+a);
                     System.out.println("y: "+y);
                     System.out.println("b: "+b);
                     System.out.println("gx: "+gx);
                     System.out.println("gxa: "+gxa);
                     System.out.println("gy: "+gy);
                     System.out.println("gyb: "+gyb);
                     System.out.println("Discrepancy: "+(instance.fixedOrderingCost + gxa - gx - a*(gy-gyb)/b));
                     flag = false;
                  }
               }
            }
         }
      }
      return flag;
   }
   
   /**
    * Test a visibility version of CKi from 
    * 
    * Gallego G, Scheller-Wolf A (2000) Capacitated inventory problems with fixed order costs: 
    * Some optimal policy structure. European Journal of Operational Research 126(3):603-613.
    */
   public static boolean testKBConvexity_visibility(Instance instance, Solution solution, int min, int max, FUNCTION f) {
      boolean flag = true;
      
      double[][] optimalCost;
      switch(f) {
      case Gn:
         optimalCost = solution.Gn;
         break;
      case Cn:
      default:
         optimalCost = solution.Cn;
      }
      
      double K = instance.fixedOrderingCost;
      int B = instance.maxQuantity;
      int t = 0;
      for(int x2 = max; x2 >= min+B; x2--) {
         double gx2 = optimalCost[t][x2 - instance.minInventory];
         double delta = 0.0000000001;
         
         for(int x1 = x2; x1 >= Math.max(min, x2 - B); x1--) {
            double gx1 = optimalCost[t][x1 - instance.minInventory];
            
            for(int a = 1; a < Math.min(B, x2-x1); a++) {
               double gx12 = optimalCost[t][x2 - a - instance.minInventory];
               
               double lambdaC = 1.0*B/(x2-x1);
               double lambda = 1.0*a/(x2-x1);
               
               if(gx12 > lambda*gx1 + lambdaC*(gx2 + K) + delta){
                  System.out.println("t: "+t);
                  System.out.println("K: "+instance.fixedOrderingCost);
                  System.out.println("B: "+instance.maxQuantity);
                  System.out.println("x1: "+x1);
                  System.out.println("x2: "+x2);
                  System.out.println("a: "+a);
                  System.out.println("lambda: "+lambda);
                  System.out.println("lambdaC: "+lambdaC);
                  System.out.println("Discrepancy: "+
                        gx12 + " > " + (lambda*gx1 + lambdaC*(gx2 + K))
                        );
                  System.out.println();
                  flag = false;
               }
               
            }
         }
      }
      
      return flag;
      
   }
   
   /**
    * Test CKii from 
    * 
    * Shaoxiang C (2004) The infinite horizon periodic review problem with setup costs and capacity constraints: 
    * A partial characterization of the optimal policy. Operations Research 52(3):409-421.
    */
   public static boolean testKBConvexity_ii_Shiaoxiang(Instance instance, Solution solution, int min, int max, FUNCTION f) {
      boolean flag = true;
      
      double[][] optimalCost;
      switch(f) {
      case Gn:
         optimalCost = solution.Gn;
         break;
      case Cn:
      default:
         optimalCost = solution.Cn;
      }
      
      int t = 0;
      for(int x = max; x >= min + instance.maxQuantity; x--) {
         for(int y = x; y >= min + instance.maxQuantity; y--) {
            for(int a = 1; a <= Math.min(max-x,instance.maxQuantity); a++) {
               double gx = optimalCost[t][x - instance.minInventory];
               double gxa = optimalCost[t][x + a - instance.minInventory];
               double gy = optimalCost[t][y - instance.maxQuantity - instance.minInventory];
               double gyC = optimalCost[t][y - instance.minInventory];
               double delta = 0.0000000001;

               if((instance.fixedOrderingCost + gyC - gy)/instance.maxQuantity > (instance.fixedOrderingCost + gxa - gx)/a + delta){
                  System.out.println("t: "+t);
                  System.out.println("K: "+instance.fixedOrderingCost);
                  System.out.println("B: "+instance.maxQuantity);
                  System.out.println("x: "+x);
                  System.out.println("a: "+a);
                  System.out.println("y: "+y);
                  System.out.println("gx: "+gx);
                  System.out.println("gy: "+gy);
                  System.out.println("gxa: "+gxa);
                  System.out.println("gyC: "+gyC);

                  System.out.println("Discrepancy: "+(instance.fixedOrderingCost + gyC - gy)/instance.maxQuantity+">"+(instance.fixedOrderingCost + gxa - gx)/a);
                  flag = false;
               }
            }
         }
      }
      return flag;
   }
   
   /**
    * Test a visibility version of CKii from 
    * 
    * Shaoxiang C (2004) The infinite horizon periodic review problem with setup costs and capacity constraints: 
    * A partial characterization of the optimal policy. Operations Research 52(3):409-421.
    */
   public static boolean testKBConvexity_ii_Shiaoxiang_visibility(Instance instance, Solution solution, int min, int max, FUNCTION f) {
      boolean flag = true;
      
      double[][] optimalCost;
      switch(f) {
      case Gn:
         optimalCost = solution.Gn;
         break;
      case Cn:
      default:
         optimalCost = solution.Cn;
      }
      
      double K = instance.fixedOrderingCost;
      int B = instance.maxQuantity;
      int t = 0;
      for(int x2 = max; x2 >= min+2*B; x2--) {
         double gx2 = optimalCost[t][x2 - instance.minInventory];
         double delta = 0.000001;
         
         for(int x1 = x2 - B - 1; x1 >= Math.max(min, x2 - 2*B); x1--) {
            double gx1 = optimalCost[t][x1 - instance.minInventory];
            double gx1PlusB = optimalCost[t][x1 + B - instance.minInventory];
            
            for(int a = 1; a <= Math.min(B, x2-x1-B); a++) {
               double gx2Minusa = optimalCost[t][x2 - a - instance.minInventory];
               
               double lambdaC = 1.0*B/(x2-x1);
               double lambda = 1.0*a/(x2-x1);
               
               
               if(lambda*gx1 + lambdaC*(gx2 + K) + delta < lambda*(gx1PlusB + K) + lambdaC*gx2Minusa){
                  System.out.println("t: "+t);
                  System.out.println("K: "+instance.fixedOrderingCost);
                  System.out.println("B: "+instance.maxQuantity);
                  System.out.println("x1: "+x1);
                  System.out.println("x2: "+x2);
                  System.out.println("a: "+a);
                  System.out.println("lambda: "+lambda);
                  System.out.println("lambdaC: "+lambdaC);
                  System.out.println("Discrepancy: "+
                        (lambda*gx1 + lambdaC*(gx2 + K)) + " < " + (lambda*(gx1PlusB + K) + lambdaC*gx2Minusa)
                        );
                  System.out.println();
                  flag = false;
               }
               
            }
         }
      }
      
      return flag;
      
   }
   
   /**
    * Test CKiii, this is a new unpublished property
    * 
    * K+Gn(y)-Cn(y-B)<=K+Gn(x+a)-Cn(x), where y<=x and a>0.
    */
   public static boolean testKBConvexity_iii(Instance instance, Solution solution, int min, int max) {
      boolean flag = true;
      
      int t = 0;
      for(int x = max; x >= min + instance.maxQuantity; x--) {
         for(int y = x; y >= min + instance.maxQuantity; y--) {
            for(int a = 1; a <= Math.min(max-x,instance.maxQuantity); a++) {
               double gx = solution.Cn[t][x - instance.minInventory];
               double gxa = solution.Gn[t][x + a - instance.minInventory];
               double gy = solution.Cn[t][y - instance.maxQuantity - instance.minInventory];
               double gyC = solution.Gn[t][y - instance.minInventory];
               double delta = 0.0000000001;

               if((instance.fixedOrderingCost + gyC - gy)/instance.maxQuantity >= (instance.fixedOrderingCost + gxa - gx)/a + delta){
                  System.out.println("t: "+t);
                  System.out.println("K: "+instance.fixedOrderingCost);
                  System.out.println("B: "+instance.maxQuantity);
                  System.out.println("x: "+x);
                  System.out.println("a: "+a);
                  System.out.println("y: "+y);
                  System.out.println("gx: "+gx);
                  System.out.println("gy: "+gy);
                  System.out.println("gxa: "+gxa);
                  System.out.println("gyC: "+gyC);

                  System.out.println("Discrepancy: "+(instance.fixedOrderingCost + gyC - gy)/instance.maxQuantity+" > "+(instance.fixedOrderingCost + gxa - gx)/a);
                  flag = false;
               }
               
               if((instance.fixedOrderingCost + gyC - gy)/instance.maxQuantity <= -delta) {
                  System.out.println("t: "+t);
                  System.out.println("K: "+instance.fixedOrderingCost);
                  System.out.println("B: "+instance.maxQuantity);
                  System.out.println("x: "+x);
                  System.out.println("a: "+a);
                  System.out.println("y: "+y);
                  System.out.println("gx: "+gx);
                  System.out.println("gy: "+gy);
                  System.out.println("gxa: "+gxa);
                  System.out.println("gyC: "+gyC);

                  System.out.println("Discrepancy: "+(instance.fixedOrderingCost + gyC - gy)/instance.maxQuantity+" > 0");
                  flag = false;
               }
            }
         }
      }
      return flag;
   }

   /**
    * Tests if there exists a threshold sm such that one should order for x<=sm and not order otherwise.
    */
   public static boolean testAlwaysOrder(Instance instance, Solution solution, int min, int max) {
      boolean flag = true;
      for(int t = 0; t < instance.getStages(); t++) {
         boolean orderFound = false;
         for(int i = max; i >= min; i--) {
            if(solution.optimalAction[t][i - instance.minInventory] > 0 && !orderFound) {
               orderFound = true;
            }else if(solution.optimalAction[t][i - instance.minInventory] == 0 && orderFound) {
               System.out.println("Zero order after reorder point");
               System.out.println("t="+t);
               System.out.println("i="+i);
               flag = false;
            }
         }
      }
      return flag;
   }

   /**
    * A test for quasi-(K,B)-convexity (note that a (K,B)-convex function is quasi-(K,B)-convex) based on visibility.
    */
   public static boolean testQuasiKBConvexity_visibility(Instance instance, Solution solution, int min, int max, FUNCTION f) {
      boolean flag = true;
      
      double[][] optimalCost;
      switch(f) {
      case Gn:
         optimalCost = solution.Gn;
         break;
      case Cn:
      default:
         optimalCost = solution.Cn;
      }
      
      double K = instance.fixedOrderingCost;
      int B = instance.maxQuantity;
      int t = 0;
      for(int x2 = max; x2 >= min + B; x2--) {
         double gx2 = optimalCost[t][x2 - instance.minInventory];
         double delta = 0.0000000001;
         
         for(int x1 = x2; x1 >= Math.max(min, x2 - B); x1--) {
            double gx1 = optimalCost[t][x1 - instance.minInventory];
            
            for(int a = 1; a < Math.min(B, x2-x1); a++) {
               double gx12 = optimalCost[t][x2 - a - instance.minInventory];
               
               if(gx12 > Math.max(gx1, gx2 + K) + delta){
                  System.out.println("t: "+t);
                  System.out.println("K: "+instance.fixedOrderingCost);
                  System.out.println("B: "+instance.maxQuantity);
                  System.out.println("x1: "+x1);
                  System.out.println("x2: "+x2);
                  System.out.println("a: "+a);
                  System.out.println("gx1: "+gx1);
                  System.out.println("gx2+K: "+(gx2+K));
                  System.out.println("gx12: "+(gx12));
                  System.out.println();
                  flag = false;
               }
               
            }
         }
      }
      
      return flag;
   }
   
   /*********** END TESTS ****************/
   
   public static void solveSampleInstance(Instances problemInstance, long seed) {
      
      /** Random instances **/
      Random rnd = new Random();
      rnd.setSeed(seed);
      
      int safeMin, safeMax, plotMin, plotMax, plotPeriod;
      safeMin = -1000;
      safeMax = 1000;
      plotMin = -10;
      plotMax = 100;
      plotPeriod = 0;
      
      Instance instance; 
      switch(problemInstance) {
         case A:
            instance = InstancePortfolio.generateTestInstanceA();
            break;
         case B:
            instance = InstancePortfolio.generateTestInstanceB();
            break;
         case C:
            instance = InstancePortfolio.generateTestInstanceC();
            break;
         case RANDOM:
            instance = InstancePortfolio.generateRandomInstance(rnd);
            break;
         case SPARSE_RANDOM:
            instance = InstancePortfolio.generateSparseRandomInstance(rnd);
            break;
         case SHIAOXIANG1996:
            instance = InstancePortfolio.generateInstanceShiaoxiang1996();
            safeMin = -25;
            safeMax = 100;
            plotMin = -0;
            plotMax = 100;
            break;
         case GALLEGO2000:
            instance = InstancePortfolio.generateInstanceGallego2000();
            safeMin = -25;
            safeMax = 100;
            plotMin = -0;
            plotMax = 50;
            break;
         case NO_ORDER_ORDER_1:
            instance = InstancePortfolio.generateInstanceNoOrderOrder_1();
            safeMin = -1000;
            safeMax = 1000;
            plotMin = -10;
            plotMax = 1000;
            break;
         case NO_ORDER_ORDER_2:
            instance = InstancePortfolio.generateInstanceNoOrderOrder_2();
            safeMin = -1000;
            safeMax = 1000;
            plotMin = -10;
            plotMax = 1000;
            break;
         case LOCAL_MINIMUM_COUNTEREXAMPLE:
            instance = InstancePortfolio.generateInstanceLocalMinimumCounterexample();
            safeMin = -100;
            safeMax = 500;
            plotMin = 50;
            plotMax = 500;
            plotPeriod = 6;
            break;
         case SAMPLE_NORMAL:
            instance = InstancePortfolio.generateSampleNormalInstance();
            plotMin = 0;
            plotMax = 200;
            break;
         case SAMPLE_POISSON:
         default:
            instance = InstancePortfolio.generateSamplePoissonInstance();
            plotMin = -200;
            plotMax = 200;
            break;
      }
         
      FUNCTION f = FUNCTION.Gn;
      
      System.out.println();
      System.out.println("Instance sanity check: "+(instance.maxQuantity>instance.fixedOrderingCost/(instance.getStages()*instance.penaltyCost)));
      
      Solution solution = solveInstance(instance);
      
      System.out.println();
      printSolution(instance, solution, safeMin);
      
      System.out.println();
      printPolicy(instance, solution, safeMin);
      
      System.out.println();
      System.out.println("***************** Checks *****************");
      System.out.println("testKBConvexity: "+
            testKBConvexity(instance, solution, safeMin, safeMax, f));
      System.out.println("testKBConvexity_visibility: "+
            testKBConvexity_visibility(instance, solution, safeMin, safeMax, f));
      System.out.println("testKBConvexity_ii_Shiaoxiang: "+
            testKBConvexity_ii_Shiaoxiang(instance, solution, safeMin, safeMax, f));
      System.out.println("testKBConvexity_ii_Shiaoxiang_visibility: "+
            testKBConvexity_ii_Shiaoxiang_visibility(instance, solution, safeMin, safeMax, f));
      //System.out.println("testKBConvexity_iii: "+
      //      testKBConvexity_iii(instance, solution, safeMin, safeMax));
      //System.out.println("testQuasiKBConvexity_visibility: "+
      //      testQuasiKBConvexity_visibility(instance, solution, safeMin, safeMax, f));*/
      System.out.println("testAlwaysOrder: "+
            testAlwaysOrder(instance, solution, safeMin, safeMax));
      System.out.println("*******************************************");
      System.out.println();
      
      System.out.println("***************** Gn and Cn-Gn *****************");
      boolean plot = true;
      plotCostFunction(instance, solution, plotMin, plotMax, plot, f, plotPeriod);
      plotCnMinusGn(instance, solution, plotMin, plotMax, plot);
      System.out.println("*******************************************");
      System.out.println();
      
      System.out.println("***************** Simulate *****************");
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.000",otherSymbols);
      int initialInventory = 0;
      double optimalPolicyCost = solution.Cn[0][initialInventory-instance.minInventory];
      double confidence = 0.95;
      double error = 0.0001;
      double[] results = null;
      try {
         results = CapacitatedStochasticLotSizingFast.simulate_skSk(instance, initialInventory, solution.find_Sk(instance, safeMin), solution.find_sk(instance, safeMin), confidence, error);
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
   
   public static void solveRandomInstances(int numberOfInstances, long seed) {
      
      int safeMin = -2000;
      int safeMax = 2000;
      int plotMin = -500;
      int plotMax = 500;
      
      /** Random instances **/
      Random rnd = new Random();
      rnd.setSeed(seed);
      for(int i = 0; i < numberOfInstances; i++) {
         
         System.out.println("********** Instance: "+(i+1)+" *********");
         
         Instance instance = InstancePortfolio.generateSparseRandomInstance(rnd);
         
         if(i+1 < 4000) continue;
         
         System.out.println("Instance sanity check: "+(instance.maxQuantity>instance.fixedOrderingCost/(instance.getStages()*instance.penaltyCost)));
         
         FUNCTION f = FUNCTION.Cn;
         
         Solution solution = solveInstance(instance);
         
         System.out.println();
         printSolution(instance, solution, safeMin);
         
         System.out.println();
         printPolicy(instance, solution, safeMin);
         
         System.out.println();
         System.out.println("***************** Checks *****************");
         boolean flag = true;
         //flag &= testKBConvexity(instance, solution, safeMin, safeMax, f);
         //System.out.println("testKBConvexity: "+flag);   
         //flag &= testKBConvexity_visibility(instance, solution, safeMin, safeMax, f);
         //System.out.println("testKBConvexity_visibility: "+ flag);
         //flag &= testKBConvexity_ii_Shiaoxiang(instance, solution, safeMin, safeMax, f);
         //System.out.println("testKBConvexity_ii_Shiaoxiang: "+flag);
         //flag &= testKBConvexity_ii_Shiaoxiang_visibility(instance, solution, safeMin, safeMax, f);
         //System.out.println("testKBConvexity_ii_Shiaoxiang_visibility: "+ flag);
         //flag &= testKBConvexity_iii(instance, solution, safeMin, safeMax);
         //System.out.println("testKBConvexity_iii: "+ flag);
         //flag &= testQuasiKBConvexity_visibility(instance, solution, safeMin, safeMax, f);
         //System.out.println("testQuasiKBConvexity_visibility: "+ flag);
         flag &= testAlwaysOrder(instance, solution, safeMin, safeMax);
         System.out.println("testAlwaysOrder: "+flag);
         System.out.println("*******************************************");
         System.out.println();
         
         if(flag == false) {
            boolean plot = false;
            int plotPeriod = 7;
            //plotCostFunction(instance, solution, plotMin, plotMax, plot, f, plotPeriod);
            //plotCnMinusGn(instance, solution, plotMin, plotMax, plot);
            System.out.println("********** End instance: "+(i+1)+" *********");
            System.exit(1);
         }
      }
   }
   
   /**
    * Simulation of an (s_k,S_k) policy
    */
   public static double[] simulate_skSk(
         Instance instance,
         int initialStock,
         int[][] S, // first index t, second index k
         int[][] s, // first index t, second index k
         double confidence,
         double error){
      Distribution[] demand = instance.demand;
      double orderCost = instance.fixedOrderingCost;
      double holdingCost = instance.holdingCost;
      double penaltyCost = instance.penaltyCost;
      double unitCost = instance.unitCost;
      double maxOrderQuantity = instance.maxQuantity;
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
            double currentInventory = inventory;
            if(currentInventory > s[t][s[t].length - 1]) {
               double qty = 0;
               inventory = qty+inventory-demandRealizations[t];
               replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
            }else if(currentInventory <= s[t][0]) {
               double qty = Math.min(S[t][0]-inventory, maxOrderQuantity);
               replicationCost += orderCost;
               replicationCost += qty*unitCost;
               inventory = qty+inventory-demandRealizations[t];
               replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
            }else{
               for(int k = s[t].length - 2; k >= 0; k--) {
                  if(currentInventory > s[t][k] && currentInventory <= s[t][k+1]){
                     double qty = Math.min(S[t][k+1]-inventory, maxOrderQuantity);
                     replicationCost += orderCost;
                     replicationCost += qty*unitCost;
                     inventory = qty+inventory-demandRealizations[t];
                     replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
                  }
               }
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
      
      Instances instance = Instances.RANDOM;
      //solveSampleInstance(instance, seed);
      
      @SuppressWarnings("unused")
      int instances = 1000000;
      solveRandomInstances(instances, seed);
   }
}

enum Instances {
   A, B, C, SAMPLE_POISSON, SAMPLE_NORMAL, RANDOM, SPARSE_RANDOM, SHIAOXIANG1996, GALLEGO2000, NO_ORDER_ORDER_1, NO_ORDER_ORDER_2, LOCAL_MINIMUM_COUNTEREXAMPLE
}

class InstancePortfolio{
   
   public static Instance generateTestInstanceA() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 178;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 4;
      double[] meanDemand = {20, 10, 93, 29, 49, 97, 37, 60, 38, 47};
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      int maxQuantity = 1000;


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   public static Instance generateTestInstanceB() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 336;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 5;
      double[] meanDemand = {50, 12, 97, 27, 74, 59, 7, 46, 78, 63};
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      int maxQuantity = 1000;


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   public static Instance generateTestInstanceC() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 122;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 2;
      double[] meanDemand = {6, 18, 44, 54, 26, 50, 54, 73, 18, 53};
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      int maxQuantity = 1000;


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   public static Instance generateSamplePoissonInstance() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 100;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 10;
      double[] meanDemand = {20,40,60,40};
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      
      int maxQuantity = 65;
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(meanDemand));
   
   
      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }

   public static Instance generateSampleNormalInstance() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 100;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 10;
      double[] meanDemand = {20,40,60,40};
      double cv = 0.25;
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new NormalDist(d, cv*d)).toArray(Distribution[]::new);
      int maxQuantity = 65;
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand));
   
   
      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }

   public static Instance generateRandomInstance(Random rnd) {
      /** SDP boundary conditions **/
      double tail = 0.0000000001;
      int minInventory = -10000;
      int maxInventory = 10000;
      
      /*******************************************************************
       * Problem parameters
       */
      
      int stages = 15;
      double fixedOrderingCost = rnd.nextInt(500) + 50; 
      double unitCost = 0; 
      double holdingCost = 1;
      double penaltyCost = rnd.nextInt(50)+1;
      int maxQuantity = 25+rnd.nextInt(50);
      int maxDemand = 1 + rnd.nextInt(200);
      RandomDist[] demand = new RandomDist[stages];
      Arrays.fill(demand, new RandomDist(maxDemand));
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand));
      
      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   /**
    * Create another function (Sparse random demand)
    * 
    * capacity in [2,200]
    * 
    * maxDemand = uniform[1,300]
    * 5 demand values in total, 3 above capacity, 2 below
    * 
    * N = 12
    * K = uniform[1,500]
    * h = 1
    * p = uniform[1,30]
    * 
    * initial inventory = 0
    */
   public static Instance generateSparseRandomInstance(Random rnd) {
      /** SDP boundary conditions **/
      double tail = 0.0000000001;
      int minInventory = -5000;
      int maxInventory = 5000;
      
      /*******************************************************************
       * Problem parameters
       */
      
      int stages = 6;
      double fixedOrderingCost = 100 + rnd.nextInt(400); 
      double unitCost = 0; 
      double holdingCost = 1;
      double penaltyCost = 10 + rnd.nextInt(30);
      int maxQuantity = 20 + rnd.nextInt(180);
      int maxDemand = 300;
      SparseRandomDist[] demand = new SparseRandomDist[stages];
      Arrays.fill(demand, new SparseRandomDist(maxDemand));
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand));
      
      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }

   public static Instance generateInstanceShiaoxiang1996() {
      /** SDP boundary conditions **/
      double tail = 0;
      int minInventory = -50;
      int maxInventory = 300;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 22;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 10;
      double discountFactor = 0.9;
      Shiaoxiang1996Dist[] demand = new Shiaoxiang1996Dist[20];
      Arrays.fill(demand, new Shiaoxiang1996Dist());
      int maxQuantity = 9;
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand));


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            discountFactor,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   public static Instance generateInstanceGallego2000() {
      /** SDP boundary conditions **/
      double tail = 0;
      int minInventory = -50;
      int maxInventory = 300;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 10;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 9;
      Gallego2000Dist[] demand = new Gallego2000Dist[52];
      Arrays.fill(demand, new Gallego2000Dist());
      int maxQuantity = 10;
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand));


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   public static Instance generateInstanceNoOrderOrder_1() {
      /** SDP boundary conditions **/
      double tail = 0;
      int minInventory = -5000;
      int maxInventory = 5000;
      
      /*** Problem instance ***/
      int stages = 12;
      double fixedOrderingCost = 241;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 27;
      NoOrderOrder1Dist[] demand = new NoOrderOrder1Dist[stages];
      Arrays.fill(demand, new NoOrderOrder1Dist());
      int maxQuantity = 116;
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand));


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   public static Instance generateInstanceNoOrderOrder_2() {
      /** SDP boundary conditions **/
      double tail = 0;
      int minInventory = -10000;
      int maxInventory = 10000;
      
      /*** Problem instance ***/
      int stages = 12;
      double fixedOrderingCost = 166;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 29;
      NoOrderOrder2Dist[] demand = new NoOrderOrder2Dist[stages];
      Arrays.fill(demand, new NoOrderOrder2Dist());
      int maxQuantity = 46;
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand));


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      return instance;
   }
   
   public static Instance generateInstanceLocalMinimumCounterexample() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 494;
      double unitCost = 0;
      double holdingCost = 1;
      double penaltyCost = 15;
      double[] meanDemand = {151,152,58,78,134,13,22,161,43,55,110,37};
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      int maxQuantity = 128;
      
      System.out.println(
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Penalty cost: "+penaltyCost+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(meanDemand));


      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            penaltyCost,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
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
   public int maxQuantity;
   
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
         int maxQuantity,
         double tail,
         int minInventory,
         int maxInventory) {
      this(fixedOrderingCost, unitCost, holdingCost, penaltyCost, demand, maxQuantity, tail, minInventory, maxInventory);
      this.discountFactor = discountFactor;
   }
   
   public Instance(
         double fixedOrderingCost,
         double unitCost,
         double holdingCost,
         double penaltyCost,
         Distribution[] demand,
         int maxQuantity,
         double tail,
         int minInventory,
         int maxInventory) {
      this.fixedOrderingCost = fixedOrderingCost;
      this.unitCost = unitCost;
      this.holdingCost = holdingCost;
      this.penaltyCost = penaltyCost;
      this.demand = demand;
      this.maxQuantity = maxQuantity;
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
}

class Solution {
   
   /** Explain meaning of indexes **/
   public int optimalAction[][];
   public double Gn[][];
   public double Cn[][];
   public double rightQuasiconvexEnvelopeGn[][];
   public double leftQuasiconvexEnvelopeGn[][];
   public double rightQuasiconvexEnvelopeCn[][];
   public double leftQuasiconvexEnvelopeCn[][];
   public double quasiconvexEnvelopeGn[][];
   public double quasiconvexEnvelopeCn[][];
   
   public Solution(int optimalAction[][], double Gn[][], double[][] Cn, int maxQuantity) {
      this.optimalAction = optimalAction;
      this.Gn = Gn;
      this.rightQuasiconvexEnvelopeGn = computeRightQuasiconvexEnvelopeCost(this.Gn, maxQuantity);
      this.leftQuasiconvexEnvelopeGn = computeLeftQuasiconvexEnvelopeCost(this.Gn, maxQuantity);
      this.quasiconvexEnvelopeGn = computeQuasiconvexEnvelopeCost(this.Gn);
      this.Cn = Cn;
      this.rightQuasiconvexEnvelopeCn = computeRightQuasiconvexEnvelopeCost(this.Cn, maxQuantity);
      this.leftQuasiconvexEnvelopeCn = computeLeftQuasiconvexEnvelopeCost(this.Cn, maxQuantity);
      this.quasiconvexEnvelopeCn = computeQuasiconvexEnvelopeCost(this.Cn);
   }
   
   private double[][] computeQuasiconvexEnvelopeCost(double[][] optimalCost){
      double[][] optimalQuasiconvexEnvelopeCost = new double[optimalCost.length][];
      for(int t = 0; t < optimalCost.length; t++) {
         optimalQuasiconvexEnvelopeCost[t] = optimalCost[t].clone();
         double globalMinimum = Arrays.stream(optimalCost[t]).min().getAsDouble();
         for(int i = 1; i < optimalCost[t].length; i++) {
            if(optimalQuasiconvexEnvelopeCost[t][i] <= globalMinimum) break;
            if(optimalQuasiconvexEnvelopeCost[t][i] >= optimalQuasiconvexEnvelopeCost[t][i-1]) 
               optimalQuasiconvexEnvelopeCost[t][i] = optimalQuasiconvexEnvelopeCost[t][i-1];
         }
         for(int i = optimalCost[t].length - 1; i > 0; i--) {
            if(optimalQuasiconvexEnvelopeCost[t][i] <= globalMinimum) break;
            if(optimalQuasiconvexEnvelopeCost[t][i-1] >= optimalQuasiconvexEnvelopeCost[t][i]) 
               optimalQuasiconvexEnvelopeCost[t][i-1] = optimalQuasiconvexEnvelopeCost[t][i];
         }
      }
      return optimalQuasiconvexEnvelopeCost;
   }
   
   private double[][] computeLeftQuasiconvexEnvelopeCost(double[][] optimalCost, int maxQuantity){
      double[][] optimalQuasiconvexEnvelopeCost = new double[optimalCost.length][];
      for(int t = 0; t < optimalCost.length; t++) {
         optimalQuasiconvexEnvelopeCost[t] = optimalCost[t].clone();
         for(int i = 1; i < optimalCost[t].length; i++) {
            optimalQuasiconvexEnvelopeCost[t][i] = Arrays.stream(optimalCost[t],Math.max(0, i-maxQuantity),i+1).min().getAsDouble();
         }
      }
      return optimalQuasiconvexEnvelopeCost;
   }
   
   private double[][] computeRightQuasiconvexEnvelopeCost(double[][] optimalCost, int maxQuantity){
      double[][] optimalQuasiconvexEnvelopeCost = new double[optimalCost.length][];
      for(int t = 0; t < optimalCost.length; t++) {
         optimalQuasiconvexEnvelopeCost[t] = optimalCost[t].clone();
         for(int i = 0; i < optimalCost[t].length; i++) {
            optimalQuasiconvexEnvelopeCost[t][i] = Arrays.stream(optimalCost[t],i,Math.min(i+maxQuantity+1, optimalCost[t].length)).min().getAsDouble();
         }
      }
      return optimalQuasiconvexEnvelopeCost;
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
   
   public int[][] find_Sk(Instance instance, int safeMin) {
      int[][] S = new int[instance.getStages()][];
      for(int t = 0; t < instance.getStages(); t++) {
         ArrayList<Integer> values = new ArrayList<Integer>();
         for(int i = safeMin - instance.minInventory; i < this.optimalAction[t].length; i++) {
            if(this.optimalAction[t][i] > this.optimalAction[t][i-1]) {
               values.add(Integer.valueOf(i-1 + this.optimalAction[t][i-1] + instance.minInventory));
            }else if(this.optimalAction[t][i] == 0) {
               values.add(Integer.valueOf(i-1 + this.optimalAction[t][i-1] + instance.minInventory));
               break;
            }
         }
         S[t] = values.stream().mapToInt(i -> i).toArray();
      }
      return S;
   }
   
   public int[][] find_sk(Instance instance, int safeMin) {
      int[][] s = new int[instance.getStages()][];
      for(int t = 0; t < instance.getStages(); t++) {
         ArrayList<Integer> values = new ArrayList<Integer>();
         for(int i = safeMin - instance.minInventory; i < this.optimalAction[t].length; i++) {
            if(this.optimalAction[t][i] > this.optimalAction[t][i-1]) {
               values.add(Integer.valueOf(i-1 + instance.minInventory));
            }else if(this.optimalAction[t][i] == 0) {
               values.add(Integer.valueOf(i-1 + instance.minInventory));
               break;
            }
         }
         s[t] = values.stream().mapToInt(i -> i).toArray();
      }
      return s;
   }
}

class RandomDist implements Distribution{
   public int maxDemand = Integer.MAX_VALUE;
   
   public RandomDist(int maxDemand) {
      this.maxDemand = maxDemand;
   }
   
   @Override
   public double cdf(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double barF(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double inverseF(double u) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getMean() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getVariance() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getStandardDeviation() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double[] getParams() {
      throw new NullPointerException("Unimplemented method.");
   }
   
   @Override
   public String toString() {
      return "RandomDist";
   }
}

class SparseRandomDist implements Distribution{
   public int maxDemand = Integer.MAX_VALUE;
   
   public SparseRandomDist(int maxDemand) {
      this.maxDemand = maxDemand;
   }
   
   @Override
   public double cdf(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double barF(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double inverseF(double u) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getMean() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getVariance() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getStandardDeviation() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double[] getParams() {
      throw new NullPointerException("Unimplemented method.");
   }
   
   @Override
   public String toString() {
      return "SparseRandomDist";
   }
}

class Shiaoxiang1996Dist implements Distribution{

   @Override
   public double cdf(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double barF(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double inverseF(double u) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getMean() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getVariance() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getStandardDeviation() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double[] getParams() {
      throw new NullPointerException("Unimplemented method.");
   }
   
   @Override
   public String toString() {
      return "Shiaoxiang1996Dist";
   }
   
}

class Gallego2000Dist implements Distribution{

   @Override
   public double cdf(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double barF(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double inverseF(double u) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getMean() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getVariance() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getStandardDeviation() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double[] getParams() {
      throw new NullPointerException("Unimplemented method.");
   }
   
   @Override
   public String toString() {
      return "Gallego2000Dist";
   }
   
}

class NoOrderOrder1Dist implements Distribution{

   @Override
   public double cdf(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double barF(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double inverseF(double u) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getMean() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getVariance() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getStandardDeviation() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double[] getParams() {
      throw new NullPointerException("Unimplemented method.");
   }
   
   @Override
   public String toString() {
      return "NoOrderOrder1Dist";
   }
   
}

class NoOrderOrder2Dist implements Distribution{

   @Override
   public double cdf(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double barF(double x) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double inverseF(double u) {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getMean() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getVariance() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double getStandardDeviation() {
      throw new NullPointerException("Unimplemented method.");
   }

   @Override
   public double[] getParams() {
      throw new NullPointerException("Unimplemented method.");
   }
   
   @Override
   public String toString() {
      return "NoOrderOrder2Dist";
   }
   
}
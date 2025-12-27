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

package jsdp.app.standalone.stochastic.servicelevel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.stat.Tally;

import jsdp.utilities.sampling.SampleFactory;

public class StochasticLotSizingFast {
   
   private static double computeImmediateCost(
         int inventory, 
         int quantity, 
         int demand,
         double holdingCost, 
         double penaltyCost, 
         double fixedOrderingCost, 
         double unitCost) {
      return holdingCost*Math.max(0, inventory + quantity - demand);
   }
   
   private static double[] p_vector;
   
   private static double computeImmediateCostLagrangian(
         int inventory, 
         int quantity, 
         int demand,
         double holdingCost, 
         double penaltyCost, 
         double fixedOrderingCost, 
         double unitCost,
         int period) {
      return holdingCost*Math.max(0, inventory + quantity - demand) 
            + p_vector[period] *Math.max(0, demand - inventory - quantity);
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
   
   private static Solution sdp(Instance instance) {
      if(USE_FAST) {
         return sdp_fast(instance);
      }
      
      double demandProbabilities [][] = InstancePortfolio.computeDemandProbability(instance);
      
      int optimalAction[][] = new int [instance.getStages()][instance.stateSpaceSize()];
      double Gn[][] = new double [instance.getStages()][instance.stateSpaceSize()];
      double Cn[][] = new double [instance.getStages()][instance.stateSpaceSize()];
      
      /** Compute Expected Cost **/
      
      for(int t = instance.getStages()-1; t >= 0; t--) {                               // Time
         double totalCost[][] = new double [instance.stateSpaceSize()][instance.maxQuantity+1];
         for(int i = 0; i < instance.stateSpaceSize(); i++) {                          // Inventory
            for(int a = 0; a <= instance.maxQuantity; a++) {                           // Actions
               if(a + instance.inventory(i) < (int) Math.ceil(instance.demand[t].inverseF(instance.alpha))) {
                  totalCost[i][a] = Double.MAX_VALUE;
               }else {
                  totalCost[i][a] += (a > 0) ? instance.fixedOrderingCost + instance.unitCost * a : 0;
                  double totalProbabilityMass = 0;
                  for(int d = 0; d < demandProbabilities[t].length; d++) {                // Demand
                     double immediateCost = 0;
                     double futureCost = 0;
                     if((instance.inventory(i) + a - d <= instance.maxInventory) && (instance.inventory(i) + a - d >= instance.minInventory)) {
                        immediateCost = demandProbabilities[t][d]*
                              computeImmediateCost(instance.inventory(i), a, d, instance.holdingCost, instance.alpha, instance.fixedOrderingCost, instance.unitCost);
                        futureCost = demandProbabilities[t][d]*( (t==instance.getStages()-1) ? 0 : instance.discountFactor*Cn[t+1][i+a-d] );
                        totalProbabilityMass += demandProbabilities[t][d];
                     }
                     totalCost[i][a] += immediateCost + futureCost;
                  }
                  totalCost[i][a]/=totalProbabilityMass;
               }
            }
            Gn[t][i] = totalCost[i][0];
            Cn[t][i] = getOptimalCost(totalCost[i]);
            optimalAction[t][i] = getOptimalAction(totalCost[i]);
         }
      }
      return new Solution(optimalAction, Gn, Cn, instance.maxQuantity);
   }
   
   private static int[] precomputeServiceQuantiles(Instance instance) {
      int T = instance.getStages();
      int[] q = new int[T];
      for (int t = 0; t < T; t++) {
         // demandProbabilities already normalized on support up to tail; inverseF may be expensive
         // Cache the quantile used to prune actions
         q[t] = (int) Math.ceil(instance.demand[t].inverseF(instance.alpha));
      }
      return q;
   }

   private static class PrefixStats {
      final double[] p;    // pmf
      final double[] P;    // prefix prob
      final double[] E;    // prefix expected demand
      PrefixStats(double[] p) {
         this.p = p;
         int n = p.length;
         this.P = new double[n];
         this.E = new double[n];
         double ps = 0.0, es = 0.0;
         for (int d = 0; d < n; d++) {
            ps += p[d];
            es += d * p[d];
            P[d] = ps;
            E[d] = es;
         }
      }
      // clamp index to support
      int cap(int x) { 
         if (x < 0) return 0; 
         int m = p.length - 1; 
         return Math.min(x, m); 
      }
   }

   private static Solution sdp_fast(Instance instance) {
      double[][] demandProbabilities = InstancePortfolio.computeDemandProbability(instance);
      int T = instance.getStages();

      // Precompute prefix stats per period
      PrefixStats[] stats = new PrefixStats[T];
      for (int t = 0; t < T; t++) stats[t] = new PrefixStats(demandProbabilities[t]);

      // Cache service level quantiles to prune actions
      int[] quantile = precomputeServiceQuantiles(instance);

      int[][] optimalAction = new int[T][instance.stateSpaceSize()];
      double[][] Gn = new double[T][instance.stateSpaceSize()];
      double[][] Cn = new double[T][instance.stateSpaceSize()];

      for (int t = T - 1; t >= 0; t--) {
         PrefixStats st = stats[t];
         int maxD = st.p.length - 1;
         double df = (t == T - 1) ? 0.0 : instance.discountFactor;

         for (int i = 0; i < instance.stateSpaceSize(); i++) {
            int I = instance.inventory(i);
            double[] totalCost = new double[instance.maxQuantity + 1];

            for (int a = 0; a <= instance.maxQuantity; a++) {
               int X = I + a;

               // prune actions that cannot reach service quantile
               if (X < quantile[t]) {
                  totalCost[a] = Double.MAX_VALUE;
                  continue;
               }

               // ordering cost
               double cost = (a > 0) ? instance.fixedOrderingCost + instance.unitCost * a : 0.0;

               // immediate holding cost via prefix sums over feasible demands
               int xCap = st.cap(X);
               double P_le_X = st.P[xCap];
               double E_le_X = st.E[xCap];
               cost += instance.holdingCost * (X * P_le_X - E_le_X);

               // future cost: only demands that keep next inventory within bounds
               if (t < T - 1) {
                  // feasible demand range d \in [loD, hiD]
                  int loD = Math.max(0, X - instance.maxInventory);
                  int hiD = Math.min(maxD, X - instance.minInventory);
                  // accumulate quickly over feasible d
                  double fCost = 0.0;
                  for (int d = loD; d <= hiD; d++) {
                     int nextInv = X - d;
                     fCost += st.p[d] * Cn[t + 1][instance.index(nextInv)];
                  }
                  // normalize by feasible probability mass (to match original behavior)
                  double mass = (loD <= hiD) ? (st.P[hiD] - (loD > 0 ? st.P[loD - 1] : 0.0)) : 0.0;
                  if (mass > 0.0) cost += df * fCost / mass;
                  else cost = Double.MAX_VALUE; // no feasible transition
               }

               totalCost[a] = cost;
            }

            Gn[t][i] = totalCost[0];
            Cn[t][i] = getOptimalCost(totalCost);
            optimalAction[t][i] = getOptimalAction(totalCost);
         }
      }
      return new Solution(optimalAction, Gn, Cn, instance.maxQuantity);
   }
   
   /* 
    * Newton/secant-like step, using a simple finite-difference derivative estimate 
    * per period from the previous iteration. It falls back to a small default step 
    * on the first iteration or if the derivative is ill-conditioned, and it keeps 
    * updates nonnegative and mildly damped for stability.
    * 
    * Error: err_t = results[t] - (1 - alpha)
    * Derivative estimate: d_t = (err_t - prev_err[t]) / (p_vector[t] - prev_p[t])
    * Newton step: delta_t = - err_t / d_t (secant when only past sample)
    * Damping and clipping: limit |delta_t|, ensure p_vector[t] >= 0
    */
   
   public static Solution coordinateDescent(Instance instance, int initialInventory, int safeMin) {
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.000", otherSymbols);

      p_vector = new double[instance.getStages()];
      Arrays.fill(p_vector, instance.holdingCost * instance.alpha / (1.0 - instance.alpha));

      // Newton-like parameters
      double fallback_step = 0.5;
      double max_step = 5.0;
      double damping = 0.75;

      double[] prev_p = new double[instance.getStages()];
      double[] prev_err = new double[instance.getStages()];
      Arrays.fill(prev_p, Double.NaN);
      Arrays.fill(prev_err, Double.NaN);

      // CI-aware parameters
      double confidence = 0.95;
      double epsAbs = (1.0 - instance.alpha)*0.05; // absolute CI half-width target for stockout probs (one order of magnitude less than target service level)
      double margin = epsAbs;                      // hysteresis tied to achievable precision
      double target = 1.0 - instance.alpha;

      boolean end = false;
      Solution solution = null;

      while (!end) {
         solution = sdp_lagrangian(instance);

         System.out.println("S: " + Arrays.toString(solution.find_S(instance, safeMin)));
         System.out.println("s: " + Arrays.toString(solution.find_s(instance, safeMin)));

         StockoutCI ci;
         try {
            ci = simulate_sS_stockoutCI(
                  instance,
                  initialInventory,
                  solution.find_S(instance, safeMin),
                  solution.find_s(instance, safeMin),
                  confidence,
                  epsAbs);
         } catch (Exception e) {
            System.out.println("This instance cannot be simulated.");
            break;
         }

         end = true;

         System.out.print("Stockout mean:\t");
         Arrays.stream(ci.mean).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();

         System.out.print("Stockout L:\t");
         Arrays.stream(ci.L).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();

         System.out.print("Stockout U:\t");
         Arrays.stream(ci.U).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();
         
         System.out.println("Runs: " + ci.runsUsed);

         for (int t = instance.getStages() - 1; t >= 0; t--) {
            // CI-aware “violation” test
            if (ci.U[t] > target + margin) {
               double err = ci.mean[t] - target;
               double delta;

               if (!Double.isNaN(prev_p[t]) && !Double.isNaN(prev_err[t])) {
                  double dp = p_vector[t] - prev_p[t];
                  double de = err - prev_err[t];
                  double deriv = de / dp;

                  if (Double.isFinite(deriv) && Math.abs(deriv) > 1e-8) {
                     delta = -err / deriv;
                  } else {
                     delta = fallback_step;
                  }
               } else {
                  delta = fallback_step;
               }

               delta = Math.copySign(Math.min(Math.abs(delta), max_step), delta);
               delta *= damping;

               prev_p[t] = p_vector[t];
               prev_err[t] = err;

               p_vector[t] = Math.max(0.0, p_vector[t] + delta);
               end = false;
               break;
            }
         }

         System.out.print("p_vector:\t");
         Arrays.stream(p_vector).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();
      }
      return solution;
   }
   
   private static Solution sdp_lagrangian(Instance instance) {
      if(USE_FAST) {
         return sdp_lagrangian_fast(instance);
      }  
      
      double demandProbabilities [][] = InstancePortfolio.computeDemandProbability(instance);
      
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
                           computeImmediateCostLagrangian(instance.inventory(i), a, d, instance.holdingCost, instance.alpha, instance.fixedOrderingCost, instance.unitCost, t);
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
   
   private static Solution sdp_lagrangian_fast(Instance instance) {
      double[][] demandProbabilities = InstancePortfolio.computeDemandProbability(instance);
      int T = instance.getStages();

      // Prefix stats per period (pmf, prefix prob, prefix expectation)
      PrefixStats[] stats = new PrefixStats[T];
      for (int t = 0; t < T; t++) stats[t] = new PrefixStats(demandProbabilities[t]);

      int[][] optimalAction = new int[T][instance.stateSpaceSize()];
      double[][] Gn = new double[T][instance.stateSpaceSize()];
      double[][] Cn = new double[T][instance.stateSpaceSize()];

      for (int t = T - 1; t >= 0; t--) {
         PrefixStats st = stats[t];
         int maxD = st.p.length - 1;
         double df = (t == T - 1) ? 0.0 : instance.discountFactor;

         // Totals over full support (P_all≈1)
         double P_all = st.P[maxD];
         double E_all = st.E[maxD];

         for (int i = 0; i < instance.stateSpaceSize(); i++) {
            int I = instance.inventory(i);
            double[] totalCost = new double[instance.maxQuantity + 1];

            for (int a = 0; a <= instance.maxQuantity; a++) {
               int X = I + a;

               // Ordering cost
               double cost = (a > 0) ? instance.fixedOrderingCost + instance.unitCost * a : 0.0;

               // Immediate cost via prefix sums:
               // E[(X-D)+] = X * P(D<=X) - E[D | D<=X]
               // E[(D-X)+] = E[D] - E[D | D<=X] - X * (1 - P(D<=X))
               int xCap = st.cap(X);
               double P_le_X = st.P[xCap];
               double E_le_X = st.E[xCap];

               double expectedHolding = X * P_le_X - E_le_X;
               double expectedShortage = (E_all - E_le_X) - X * (P_all - P_le_X);

               cost += instance.holdingCost * expectedHolding
                     + p_vector[t] * expectedShortage;

               // Future cost only over feasible demand window; normalize by feasible mass
               if (t < T - 1) {
                  int loD = Math.max(0, X - instance.maxInventory);
                  int hiD = Math.min(maxD, X - instance.minInventory);

                  double fCost = 0.0;
                  for (int d = loD; d <= hiD; d++) {
                     int nextInv = X - d;
                     fCost += st.p[d] * Cn[t + 1][instance.index(nextInv)];
                  }

                  double mass = (loD <= hiD) ? (st.P[hiD] - (loD > 0 ? st.P[loD - 1] : 0.0)) : 0.0;
                  if (mass > 0.0) {
                     cost += df * fCost / mass;
                  } else {
                     cost = Double.MAX_VALUE; // matches original behavior when no feasible transitions
                  }
               }

               totalCost[a] = cost;
            }

            Gn[t][i] = totalCost[0];
            Cn[t][i] = getOptimalCost(totalCost);
            optimalAction[t][i] = getOptimalAction(totalCost);
         }
      }
      return new Solution(optimalAction, Gn, Cn, instance.maxQuantity);
   }

   
   /*
    * Newton/secant-like step, using a simple finite-difference derivative estimate
    * per period from the previous iteration. It falls back to a small default step
    * on the first iteration or if the derivative is ill-conditioned, and it keeps
    * updates nonnegative and mildly damped for stability.
    * 
    * Error: err_t = results[t] - (1 - alpha)
    * Derivative estimate: d_t = (err_t - prev_err[t]) / (p_vector[t] - prev_p[t])
    * Newton step: delta_t = - err_t / d_t (secant when only past sample)
    * Damping and clipping: limit |delta_t|, ensure p_vector[t] >= 0
    */
   public static double[][] coordinateDescentFast(Instance instance, int initialInventory, int safeMin) {
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.000", otherSymbols);

      p_vector = new double[instance.getStages()];
      Arrays.fill(p_vector, instance.holdingCost * instance.alpha / (1.0 - instance.alpha));

      double fallback_step = 0.5;
      double max_step = 5.0;
      double damping = 0.75;

      double[] prev_p = new double[instance.getStages()];
      double[] prev_err = new double[instance.getStages()];
      Arrays.fill(prev_p, Double.NaN);
      Arrays.fill(prev_err, Double.NaN);

      // CI-aware parameters
      double confidence = 0.95;
      double epsAbs = (1.0 - instance.alpha)*0.05; // absolute CI half-width target for stockout probs (two order of magnitude less than target service level)
      double margin = epsAbs;                      // hysteresis tied to achievable precision
      double target = 1.0 - instance.alpha;

      boolean end = false;
      double[][] sS = null;

      while (!end) {
         if (instance.demand[0] instanceof PoissonDist)
            sS = sS_fast_Poisson(instance);
         else if (instance.demand[0] instanceof NormalDist)
            sS = sS_fast_Normal(instance);
         else {
            System.err.println("Distribution not supported");
            System.exit(-1);
         }

         int[] S = Arrays.stream(sS).map(Sr -> Sr[1]).mapToInt(v -> v.intValue()).toArray();
         int[] s = Arrays.stream(sS).map(sr -> sr[0]).mapToInt(v -> v.intValue()).toArray();

         System.out.println("S: " + Arrays.toString(S));
         System.out.println("s: " + Arrays.toString(s));

         StockoutCI ci;
         try {
            ci = simulate_sS_stockoutCI(instance, initialInventory, S, s, confidence, epsAbs);
         } catch (Exception e) {
            System.out.println("This instance cannot be simulated.");
            break;
         }

         System.out.print("Stockout mean:\t");
         Arrays.stream(ci.mean).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();
         
         System.out.print("Stockout L:\t");
         Arrays.stream(ci.L).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();

         System.out.print("Stockout U:\t");
         Arrays.stream(ci.U).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();
         
         System.out.println("Runs: " + ci.runsUsed);

         end = true;

         for (int t = instance.getStages() - 1; t >= 0; t--) {
            if (ci.U[t] > target + margin) {
               double err = ci.mean[t] - target;
               double delta;

               if (!Double.isNaN(prev_p[t]) && !Double.isNaN(prev_err[t])) {
                  double dp = p_vector[t] - prev_p[t];
                  double de = err - prev_err[t];
                  double deriv = de / dp;

                  if (Double.isFinite(deriv) && Math.abs(deriv) > 1e-8) {
                     delta = -err / deriv;
                  } else {
                     delta = fallback_step;
                  }
               } else {
                  delta = fallback_step;
               }

               delta = Math.copySign(Math.min(Math.abs(delta), max_step), delta);
               delta *= damping;

               prev_p[t] = p_vector[t];
               prev_err[t] = err;

               p_vector[t] = Math.max(0.0, p_vector[t] + delta);
               end = false;
               break;
            }
         }

         System.out.print("p_vector:\t");
         Arrays.stream(p_vector).forEach(v -> System.out.print(df.format(v) + "\t"));
         System.out.println();
      }

      return sS;
   }
   
   private static double[][] sS_fast_Poisson(Instance instance) {
      Arrays.stream(instance.demand).forEach(d -> {assert d instanceof PoissonDist;});
      assert instance.unitCost == 0;
      
      int stages = instance.getStages();
      double [][] sS = new double[stages][2];
      
      for(int t = 0; t < stages; t++) {
         double minCost = Double.MAX_VALUE;
         double S = Double.NaN;
         double[][] cycleCosts = new double[stages-t+1][stages-t+1];
         
         for(int i = t; i < stages+1; i++) {
            Arrays.fill(cycleCosts[i-t], Double.MAX_VALUE);
            for(int j = i; j < stages; j++) {
               cycleCosts[i-t][j-t+1]= new PoissonMultiperiodNewsboy(
                     instance.fixedOrderingCost,
                     instance.holdingCost, 
                     Arrays.copyOfRange(p_vector, i, j+1), 
                     Arrays.copyOfRange(instance.demand, i, j+1)).minETC;
            }
         }
         
         final ShortestPath sp = new ShortestPath(cycleCosts);
         int y = IntStream.range(1, stages)
               .filter(idx -> sp.shortestPathNodes[idx])
               .findFirst()
               .orElse(stages);
         
         PoissonMultiperiodNewsboy nb_S = new PoissonMultiperiodNewsboy(
               instance.fixedOrderingCost,
               instance.holdingCost, 
               Arrays.copyOfRange(p_vector, t, t+y), 
               Arrays.copyOfRange(instance.demand, t, t+y));
         
         S = nb_S.optimalX;
         minCost = sp.shortestPathCost;
         
         double s = S - 1;
         for(int x = 0; x <= S; x++) {
            for(int j = t; j < stages; j++) {
               cycleCosts[0][j-t+1] = new PoissonMultiperiodNewsboy(
                     instance.fixedOrderingCost,
                     instance.holdingCost, 
                     Arrays.copyOfRange(p_vector, t, j+1), 
                     Arrays.copyOfRange(instance.demand, t, j+1)).ETC(x) - instance.fixedOrderingCost;
            }
            double c = (new ShortestPath(cycleCosts)).shortestPathCost;
            if(c < minCost) {
               s = x - 1;
               break;
            }
         }
         sS[t][0] = s;
         sS[t][1] = S;      
      }
      return sS;
   }
   
   private static double[][] sS_fast_Normal(Instance instance) {
      Arrays.stream(instance.demand).forEach(d -> {assert d instanceof NormalDist;});
      assert instance.unitCost == 0;
      
      int stages = instance.getStages();
      double [][] sS = new double[stages][2];
      
      for(int t = 0; t < stages; t++) {
         double minCost = Double.MAX_VALUE;
         double S = Double.NaN;
         double[][] cycleCosts = new double[stages-t+1][stages-t+1];
         
         for(int i = t; i < stages+1; i++) {
            Arrays.fill(cycleCosts[i-t], Double.MAX_VALUE);
            for(int j = i; j < stages; j++) {
               cycleCosts[i-t][j-t+1]= new NormalMultiperiodNewsboy(
                     instance.fixedOrderingCost,
                     instance.holdingCost, 
                     Arrays.copyOfRange(p_vector, i, j+1), 
                     Arrays.copyOfRange(instance.demand, i, j+1)).minETC;
            }
         }
         
         final ShortestPath sp = new ShortestPath(cycleCosts);
         int y = IntStream.range(1, stages)
               .filter(idx -> sp.shortestPathNodes[idx])
               .findFirst()
               .orElse(stages);
         
         NormalMultiperiodNewsboy nb_S = new NormalMultiperiodNewsboy(
               instance.fixedOrderingCost,
               instance.holdingCost, 
               Arrays.copyOfRange(p_vector, t, t+y), 
               Arrays.copyOfRange(instance.demand, t, t+y));
         
         S = nb_S.optimalX;
         minCost = sp.shortestPathCost;
         
         double s = S - 1;
         for(int x = 0; x <= S; x++) {
            for(int j = t; j < stages; j++) {
               cycleCosts[0][j-t+1] = new NormalMultiperiodNewsboy(
                     instance.fixedOrderingCost,
                     instance.holdingCost, 
                     Arrays.copyOfRange(p_vector, t, j+1), 
                     Arrays.copyOfRange(instance.demand, t, j+1)).ETC(x) - instance.fixedOrderingCost;
            }
            double c = (new ShortestPath(cycleCosts)).shortestPathCost;
            if(c < minCost) {
               s = x - 1;
               break;
            }
         }
         sS[t][0] = s;
         sS[t][1] = S;      
      }
      return sS;
   }

   public static void printSolution(Instance instance, Solution solution, int safeMin) {
      int[] S = solution.find_S(instance, safeMin);
      int t = 0;
      System.out.println("Expected total cost at Sm: "+solution.Cn[t][S[t]-instance.minInventory]);
      System.out.println("Expected total cost with zero initial inventory: "+(solution.Cn[t][-instance.minInventory]));
      
   }
   
   public static void print_sS_Policy(Instance instance, Solution solution, int safeMin) {
      int[] S = solution.find_S(instance, safeMin);
      int[] s = solution.find_s(instance, safeMin);
      System.out.println("Policy");
      System.out.println("s\tS");
      for(int t =0; t < instance.getStages(); t++) {
         System.out.println("--- Period "+t+" ---");
         System.out.println(s[t]+"\t"+S[t]);
      }
   }
   
   public static void print_sS_Policy(int[] S, int[] s) {
      System.out.println("Policy");
      System.out.println("s\tS");
      for(int t =0; t < S.length; t++) {
         System.out.println("--- Period "+t+" ---");
         System.out.println(s[t]+"\t"+S[t]);
      }
   }
   
   enum FUNCTION {
      Gn, Cn
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
      String cost = "cost"+(f == FUNCTION.Gn ? "Gn" : "Cn")+" = {";
      String order_up_to = "order_up_to = {";
      String action = "action = {";
      for(int i = Math.max(0,min-instance.minInventory); i < Math.min(instance.stateSpaceSize(),max-instance.minInventory); i++) {
         series.add(i+instance.minInventory,optimalCost[t][i]);
         cost += "{" + (i+instance.minInventory) + ", " + (optimalCost[t][i]) + "}" + 
               ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
         order_up_to += "{" + (i+instance.minInventory) + ", " + (i+instance.minInventory+solution.optimalAction[t][i]) + "}" + 
               ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
         action += "{" + (i+instance.minInventory) + ", " + (solution.optimalAction[t][i]) + "}" + 
               ((i == Math.min(instance.stateSpaceSize(),max-instance.minInventory) - 1) ? "" : ", ");
      }
      cost += "};";
      order_up_to += "};";
      action += "};";
      System.out.println(cost);
      System.out.println(order_up_to);
      System.out.println(action);
      
      /** Plot the expected optimal cost **/
      if(plot) {
         XYSeriesCollection xyDataset = new XYSeriesCollection();
         xyDataset.addSeries(series);
         JFreeChart chart = ChartFactory.createXYLineChart((f == FUNCTION.Gn ? "Gn" : "Cn"), "Opening inventory level", "Expected total cost",
               xyDataset, PlotOrientation.VERTICAL, false, true, false);
         ChartFrame frame = new ChartFrame((f == FUNCTION.Gn ? "Gn" : "Cn"),chart);
         frame.setVisible(true);
         frame.setSize(500,400);
      }
   }
   
   enum METHOD {
      SDP, CD
   }
   
   public static double[] solveInstance(Instance instance, int initialInventory, int safeMin, METHOD method) {
      Solution solution;
      switch(method) {
         case SDP:
            solution = sdp(instance);
         break;
         case CD:
         default:
            solution = coordinateDescent(instance, initialInventory, safeMin);
      }
      double confidence = 0.95;
      double error = 0.0001;
      double[] results = simulate_sS(instance, solution, initialInventory, solution.find_S(instance, safeMin), solution.find_s(instance, safeMin), confidence, error, OUTPUT.COST);
      double[] serviceLevels = simulate_sS(instance, solution, 0, solution.find_S(instance, safeMin), solution.find_s(instance, safeMin), confidence, error, OUTPUT.SERVICE_LEVELS);
      double[] out = new double[results.length + serviceLevels.length];
      System.arraycopy(results, 0, out, 0, results.length);
      System.arraycopy(serviceLevels, 0, out, results.length, serviceLevels.length);
      return out;
   }

   public static void solveSampleInstance(Instances problemInstance, METHOD method) {
      
      int safeMin, plotMin, plotMax, plotPeriod;
      plotPeriod = 0;
      
      Instance instance; 
      switch(problemInstance) {
         case SAMPLE_NORMAL:
            instance = InstancePortfolio.generateSampleNormalInstance();
            safeMin = -500;
            plotMin = 0;
            plotMax = 200;
            break;
         case SAMPLE_POISSON:
         default:
            instance = InstancePortfolio.generateSamplePoissonInstance();
            safeMin = -500;
            plotMin = 26;
            plotMax = 200;
            break;
      }
         
      FUNCTION f = FUNCTION.Gn;
      
      Solution solution = sdp(instance);
      switch(method) {
         case SDP:
            solution = sdp(instance);
         break;
         case CD:
            int initialInventory = 0;
            solution = coordinateDescent(instance, initialInventory, safeMin);
      }
      
      System.out.println();
      printSolution(instance, solution, safeMin);
      
      System.out.println();
      System.out.println("***************** Simulate *****************");
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.000",otherSymbols);
      
      double confidence = 0.95;
      double error = 0.0001;
      double[] results = null;
      double[] serviceLevels = null;
      try {
         results = simulate_sS(instance, solution, 0, solution.find_S(instance, safeMin), solution.find_s(instance, safeMin), confidence, error, OUTPUT.COST);
         serviceLevels = simulate_sS(instance, solution, 0, solution.find_S(instance, safeMin), solution.find_s(instance, safeMin), confidence, error, OUTPUT.SERVICE_LEVELS);
         System.out.println(
               "\nSimulated cost: "+ df.format(results[0])+
               "\nConfidence interval=("+df.format(results[0]-results[1])+","+
               df.format(results[0]+results[1])+")@"+
               df.format(confidence*100)+"% confidence");
         System.out.println("Service levels:");
         System.out.print("Period: \t");
         IntStream.iterate(0, i -> i+1).limit(instance.getStages()).forEach(i -> System.out.print(i + "\t"));
         System.out.print("\nStockout Pr.: \t");
         Arrays.stream(serviceLevels).forEach(i -> System.out.print(df.format(i) + "\t"));
      }catch(Exception e) {
         System.out.println("This instance cannot be simulated.");
      }
      System.out.println();
      System.out.println("*******************************************");
      
      System.out.println();
      print_sS_Policy(instance, solution, safeMin);
      
      System.out.println("***************** Gn and Cn-Gn *****************");
      boolean plot = false;
      plotCostFunction(instance, solution, plotMin, plotMax, plot, f, plotPeriod);
      System.out.println("*******************************************");
      System.out.println();
   }
   
   public static double[] solveInstanceFast(Instance instance, int initialInventory, int safeMin) {
      double[][] sS = coordinateDescentFast(instance, initialInventory, safeMin);
      double confidence = 0.95;
      double error = 0.0001;
      double[] results = simulate_sS(instance, null, 0, Arrays.stream(sS).map(S -> S[1]).mapToInt(S -> S.intValue()).toArray(), Arrays.stream(sS).map(s -> s[0]).mapToInt(s -> s.intValue()).toArray(), confidence, error, OUTPUT.COST);
      double[] serviceLevels = simulate_sS(instance, null, 0, Arrays.stream(sS).map(S -> S[1]).mapToInt(S -> S.intValue()).toArray(), Arrays.stream(sS).map(s -> s[0]).mapToInt(s -> s.intValue()).toArray(), confidence, error, OUTPUT.SERVICE_LEVELS);
      double[] out = new double[results.length + serviceLevels.length];
      System.arraycopy(results, 0, out, 0, results.length);
      System.arraycopy(serviceLevels, 0, out, results.length, serviceLevels.length);
      return out;
   }

   public static void solveSampleInstanceFast(Instances problemInstance) {
      
      int safeMin;
      
      Instance instance; 
      switch(problemInstance) {
         case SAMPLE_NORMAL:
            instance = InstancePortfolio.generateSampleNormalInstance();
            safeMin = -500;
            break;
         case SAMPLE_POISSON:
         default:
            instance = InstancePortfolio.generateSamplePoissonInstance();
            safeMin = -500;
            break;
      }
         
      int initialInventory = 0;
      double[][] sS = coordinateDescentFast(instance, initialInventory, safeMin);
      
      System.out.println();
      System.out.println("***************** Simulate *****************");
      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.000",otherSymbols);
      
      double confidence = 0.95;
      double error = 0.0001;
      double[] results = null;
      double[] serviceLevels = null;
      try {
         results = simulate_sS(instance, null, 0, Arrays.stream(sS).map(S -> S[1]).mapToInt(S -> S.intValue()).toArray(), Arrays.stream(sS).map(s -> s[0]).mapToInt(s -> s.intValue()).toArray(), confidence, error, OUTPUT.COST);
         serviceLevels = simulate_sS(instance, null, 0, Arrays.stream(sS).map(S -> S[1]).mapToInt(S -> S.intValue()).toArray(), Arrays.stream(sS).map(s -> s[0]).mapToInt(s -> s.intValue()).toArray(), confidence, error, OUTPUT.SERVICE_LEVELS);
         System.out.println(
               "\nSimulated cost: "+ df.format(results[0])+
               "\nConfidence interval=("+df.format(results[0]-results[1])+","+
               df.format(results[0]+results[1])+")@"+
               df.format(confidence*100)+"% confidence");
         System.out.println("Service levels:");
         System.out.print("Period: \t");
         IntStream.iterate(0, i -> i+1).limit(instance.getStages()).forEach(i -> System.out.print(i + "\t"));
         System.out.print("\nStockout Pr.: \t");
         Arrays.stream(serviceLevels).forEach(i -> System.out.print(df.format(i) + "\t"));
      }catch(Exception e) {
         System.out.println("This instance cannot be simulated.");
      }
      System.out.println();
      System.out.println("*******************************************");
      
      System.out.println();
      print_sS_Policy(Arrays.stream(sS).map(S -> S[1]).mapToInt(S -> S.intValue()).toArray(), Arrays.stream(sS).map(s -> s[0]).mapToInt(s -> s.intValue()).toArray());
   }

   enum OUTPUT {
      COST, SERVICE_LEVELS
   }
   
   public static double[] simulate_sS(
         Instance instance,
         Solution solution,
         int initialStock,
         int[] S, 
         int[] s, 
         double confidence,
         double error,
         OUTPUT outputType){
      Distribution[] demand = instance.demand;
      double orderCost = instance.fixedOrderingCost;
      double holdingCost = instance.holdingCost;
      double unitCost = instance.unitCost;
      double discountFactor = instance.discountFactor;
      
      Tally costTally = new Tally();
      Tally[] stockPTally = new Tally[demand.length];
      Tally[] stockNTally = new Tally[demand.length];
      Tally[] stockoutTally = new Tally[demand.length];
      for(int i = 0; i < demand.length; i++) {
         stockPTally[i] = new Tally();
         stockNTally[i] = new Tally();
         stockoutTally[i] = new Tally();
      }
      
      int minRuns = 1000;
      int maxRuns = 1000000;
      
      SampleFactory.resetStartStream();
      
      double[] centerAndRadius = new double[2];
      for(int i = 0; i < maxRuns; i++){
         double[] demandRealizations = SampleFactory.getNextLHSample(demand);
         
         double replicationCost = 0;
         double inventory = initialStock;
         for(int t = 0; t < demand.length; t++){
            double stageCost = 0;
            if(inventory <= s[t]){
               stageCost += orderCost;
               stageCost += Math.max(0, S[t]-inventory)*unitCost;
               inventory = S[t]-demandRealizations[t];
               stageCost += Math.max(inventory, 0)*holdingCost;
            }else{
               inventory = inventory-demandRealizations[t];
               stageCost += Math.max(inventory, 0)*holdingCost;
            }
            replicationCost += stageCost * Math.pow(discountFactor, t);
            stockPTally[t].add(Math.max(inventory, 0));
            stockNTally[t].add(Math.max(-inventory, 0));
            stockoutTally[t].add(inventory < 0 ? 1 : 0);
         }
         costTally.add(replicationCost);
         if(i >= minRuns) {
            if(outputType == OUTPUT.SERVICE_LEVELS) {
               boolean allWithinError = true;
               for(int t = 0; t < demand.length; t++) {
                  stockoutTally[t].confidenceIntervalNormal(confidence, centerAndRadius);
                  if(centerAndRadius[1]>centerAndRadius[0]*error) {
                     allWithinError = false;
                     break;
                  }
               }
               if(allWithinError) 
                  break;
            }
            else if(outputType == OUTPUT.COST) {
               costTally.confidenceIntervalNormal(confidence, centerAndRadius);
               if(centerAndRadius[1]<=centerAndRadius[0]*error)
                  break;
            }
         }
      }
      switch(outputType) {
         case SERVICE_LEVELS:
            return Arrays.stream(stockoutTally).mapToDouble(t -> t.average()).toArray();
         case COST:
         default:
            return centerAndRadius;
      }
   }
   
   /**
    * Container for per-period stockout probability estimates and confidence intervals.
    * L/U are bounds for stockout probability.
    */
   private static final class StockoutCI {
      final double[] mean;
      final double[] L;
      final double[] U;
      final int runsUsed;

      StockoutCI(int T, int runsUsed) {
         this.mean = new double[T];
         this.L = new double[T];
         this.U = new double[T];
         this.runsUsed = runsUsed;
      }
   }

   /**
    * CI-aware simulation for service levels (stockout probabilities).
    *
    * Uses Normal CI from SSJ Tally, and stops when ALL periods have CI radius <= epsAbs.
    * This is an absolute precision rule (recommended for small probabilities).
    */
   private static StockoutCI simulate_sS_stockoutCI(
         Instance instance,
         int initialStock,
         int[] S,
         int[] s,
         double confidence,
         double epsAbs) {

      Distribution[] demand = instance.demand;
      double orderCost = instance.fixedOrderingCost;
      double holdingCost = instance.holdingCost;
      double unitCost = instance.unitCost;
      double discountFactor = instance.discountFactor;

      Tally[] stockoutTally = new Tally[demand.length];
      for (int t = 0; t < demand.length; t++) stockoutTally[t] = new Tally();

      int minRuns = 1000;
      int maxRuns = 1000000;

      SampleFactory.resetStartStream();

      double[] centerAndRadius = new double[2];
      int runsUsed = 0;

      for (int i = 0; i < maxRuns; i++) {
         runsUsed = i + 1;
         double[] demandRealizations = SampleFactory.getNextLHSample(demand);

         double inventory = initialStock;
         @SuppressWarnings("unused")
         double replicationCost = 0.0; // kept to preserve logic parity; not returned here

         for (int t = 0; t < demand.length; t++) {
            double stageCost = 0.0;
            if (inventory <= s[t]) {
               stageCost += orderCost;
               stageCost += Math.max(0, S[t] - inventory) * unitCost;
               inventory = S[t] - demandRealizations[t];
               stageCost += Math.max(inventory, 0) * holdingCost;
            } else {
               inventory = inventory - demandRealizations[t];
               stageCost += Math.max(inventory, 0) * holdingCost;
            }
            replicationCost += stageCost * Math.pow(discountFactor, t);

            stockoutTally[t].add(inventory < 0 ? 1.0 : 0.0);
         }

         if (i >= minRuns) {
            boolean allWithin = true;
            for (int t = 0; t < demand.length; t++) {
               stockoutTally[t].confidenceIntervalNormal(confidence, centerAndRadius);
               double radius = centerAndRadius[1];
               if (!(radius <= epsAbs)) {
                  allWithin = false;
                  break;
               }
            }
            if (allWithin) break;
         }
      }

      StockoutCI out = new StockoutCI(demand.length, runsUsed);
      for (int t = 0; t < demand.length; t++) {
         stockoutTally[t].confidenceIntervalNormal(confidence, centerAndRadius);
         double m = stockoutTally[t].average();
         double r = centerAndRadius[1];
         out.mean[t] = m;
         out.L[t] = Math.max(0.0, m - r);
         out.U[t] = Math.min(1.0, m + r);
      }
      return out;
   }
   
   /**
    * Test bed
    */
   
   public static void writeToFile(String fileName, String str){
      File results = new File(fileName);
      try {
         FileOutputStream fos = new FileOutputStream(results, true);
         OutputStreamWriter osw = new OutputStreamWriter(fos);
         osw.write(str+"\n");
         osw.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
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
      int safeMin = -500;
      
      double[] fixedOrderingCost = {250,500,1000};
      double proportionalOrderingCost = 0;
      double holdingCost = 1;
      double[] serviceLevels = {0.8,0.9,0.95};
      double[][] meanDemand = getDemandPatters();
      String[] demandPattern = {"STA", "LC1", "LC2", "SIN1", "SIN2", "RAND", "EMP1", "EMP2", "EMP3", "EMP4"};
      
      writeToFile(fileName, "Fixed ordering cost, Proportional ordering cost, Service Level, Expected Demand, ETC SDP, Time SDP (s), ETC CD, Time CD (s), Service Levels CD");
      
      int instances = fixedOrderingCost.length*serviceLevels.length*demandPattern.length;
      int count = 0;
      for(double oc : fixedOrderingCost) {
         for(double s : serviceLevels) {
            for(int d = 0; d < meanDemand.length; d++) {
               /* Skip instances 
               if(count < 561) {
                  count++;
                  continue;
               }*/
               
               Distribution[] demand = Arrays.stream(meanDemand[d]).mapToObj(k -> new PoissonDist(k)).toArray(Distribution[]::new);
               
               int maxQuantity = 1000;
               
               Instance instance = new Instance(oc, proportionalOrderingCost, holdingCost, s, demand, maxQuantity, tail, minInventory, maxInventory);
               
               int initialInventory = 0;
               
               long timeSDP = System.currentTimeMillis();
               double[] resultSDP = solveInstance(instance, initialInventory, safeMin, METHOD.SDP);
               timeSDP = System.currentTimeMillis() - timeSDP;
               long timeCD = System.currentTimeMillis();
               double[] resultFast = solveInstanceFast(instance, initialInventory, safeMin);
               timeCD = System.currentTimeMillis() - timeCD;
               
               String serviceLevelsStr = Arrays.toString(Arrays.copyOfRange(resultFast, 2, 2 + instance.getStages() - 1));
               writeToFile(fileName, oc + "," + s + "," + demandPattern[d] + "," + resultSDP[0] + "," + timeSDP/1000.0 + "," + resultFast[0] + "," + timeCD/1000. + "," + serviceLevelsStr.substring(1, serviceLevelsStr.length() - 1));
               System.out.println((++count)+"/"+instances);
            }
         }
      }
   }
   
   public static void runBatchNormal(String fileName){
      double tail = 0.0001;
      int minInventory = -1000;
      int maxInventory = 1000;
      int safeMin = -500;
      
      double[] fixedOrderingCost = {250,500,1000};
      double proportionalOrderingCost = 0;
      double holdingCost = 1;
      double[] serviceLevels = {0.8,0.9,0.95};
      double[][] meanDemand = getDemandPatters();
      double[] coefficient_of_variation = {0.1,0.2,0.3};
      String[] demandPattern = {"STA", "LC1", "LC2", "SIN1", "SIN2", "RAND", "EMP1", "EMP2", "EMP3", "EMP4"};
      
      writeToFile(fileName,  "Fixed ordering cost, Service Level, Expected Demand, Coefficient of Variation, ETC SDP, Time SDP (s), ETC CD, Time CD (s), Service Levels CD");
      
      int instances = fixedOrderingCost.length*serviceLevels.length*demandPattern.length*coefficient_of_variation.length;
      int count = 0;
      for(double oc : fixedOrderingCost) {
         for(double s : serviceLevels) {
            for(int d = 0; d < meanDemand.length; d++) {
               for(int c = 0; c < coefficient_of_variation.length; c++) {
                  final int idx = c;
                  Distribution[] demand = Arrays.stream(meanDemand[d]).mapToObj(k -> new NormalDist(k, k*coefficient_of_variation[idx])).toArray(Distribution[]::new);
                  
                  int maxQuantity = 1000;
                  
                  Instance instance = new Instance(oc, proportionalOrderingCost, holdingCost, s, demand, maxQuantity, tail, minInventory, maxInventory);
                  
                  int initialInventory = 0;
                  
                  long timeSDP = System.currentTimeMillis();
                  double[] resultSDP = solveInstance(instance, initialInventory, safeMin, METHOD.SDP);
                  timeSDP = System.currentTimeMillis() - timeSDP;
                  long timeCD = System.currentTimeMillis();
                  double[] resultFast = solveInstanceFast(instance, initialInventory, safeMin);
                  timeCD = System.currentTimeMillis() - timeCD;
                  
                  String serviceLevelsStr = Arrays.toString(Arrays.copyOfRange(resultFast, 2, 2 + instance.getStages() - 1));
                  writeToFile(fileName, oc + "," + s + "," + demandPattern[d] + "," + coefficient_of_variation[idx] + "," + resultSDP[0] +","+ timeSDP/1000.0 +","+ resultFast[0] +","+ timeCD/1000.0 +","+ serviceLevelsStr.substring(1, serviceLevelsStr.length() - 1));
                  System.out.println((++count)+"/"+instances);
               }
            }
         }
      }
   }
   
   static boolean USE_FAST = true; 
   
   public static void main(String[] args) {
      Instances instance = Instances.SAMPLE_NORMAL;
      //solveSampleInstance(instance, METHOD.SDP);
      //solveSampleInstance(instance, METHOD.CD);
      solveSampleInstanceFast(instance);
      
      /*Instance inst = InstancePortfolio.generateSampleNormalInstance();
      int initialInventory = 0;
      int safeMin = -500;
      System.out.println();
      Arrays.stream(solveInstance(inst, initialInventory, safeMin, METHOD.SDP)).forEach(i -> System.out.print(i + "\t"));*/
      
      //runBatchPoisson("results_poisson.csv");
      //runBatchNormal("results_normal.csv");
   }
   
}

enum Instances {
   SAMPLE_POISSON, SAMPLE_NORMAL
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
         // Continuity correction for non-integer demands +/- 0.5
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
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 100;
      double unitCost = 0;
      double holdingCost = 1;
      double alpha = 0.9;
      double[] meanDemand = {20,40,60,40};
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new PoissonDist(d)).toArray(Distribution[]::new);
      
      int maxQuantity = 1000;
   
      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            alpha,
            demand,
            maxQuantity,
            tail,
            minInventory,
            maxInventory
            );
      
      System.out.println(instance.toString());
      
      return instance;
   }

   public static Instance generateSampleNormalInstance() {
      /** SDP boundary conditions **/
      double tail = 0.0001;
      int minInventory = -2000;
      int maxInventory = 2000;
      
      /*** Problem instance ***/
      double fixedOrderingCost = 300;
      double unitCost = 0;
      double holdingCost = 1;
      double alpha = 0.9;
      double[] meanDemand = {20,40,60,40,20,40,60,40,20,40};
      double cv = 0.25;
      Distribution[] demand = Arrays.stream(meanDemand).mapToObj(d -> new NormalDist(d, cv*d)).toArray(Distribution[]::new);
      int maxQuantity = 1000;
      
      Instance instance = new Instance(
            fixedOrderingCost,
            unitCost,
            holdingCost,
            alpha,
            demand,
            maxQuantity,
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
   public double alpha;
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
         double alpha,
         double discountFactor,
         Distribution[] demand,
         int maxQuantity,
         double tail,
         int minInventory,
         int maxInventory) {
      this(fixedOrderingCost, unitCost, holdingCost, alpha, demand, maxQuantity, tail, minInventory, maxInventory);
      this.discountFactor = discountFactor;
   }
   
   public Instance(
         double fixedOrderingCost,
         double unitCost,
         double holdingCost,
         double alpha,
         Distribution[] demand,
         int maxQuantity,
         double tail,
         int minInventory,
         int maxInventory) {
      this.fixedOrderingCost = fixedOrderingCost;
      this.unitCost = unitCost;
      this.holdingCost = holdingCost;
      this.alpha = alpha;
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
   
   @Override
   public String toString() {
      return 
            "Fixed ordering: "+fixedOrderingCost+"\n"+
            "Proportional ordering: "+unitCost+"\n"+
            "Holding cost: "+holdingCost+"\n"+
            "Service level (alpha): "+alpha+"\n"+
            "Capacity: "+maxQuantity+"\n"+
            "Demand: "+ Arrays.toString(demand);
   }
}
   
class Solution {
   
   public int optimalAction[][]; // optimalAction[stages][inventory level]
   public double Gn[][];         // Gn[stages][inventory level]
   public double Cn[][];         // Cn[stages][inventory level]
   
   public Solution(int optimalAction[][], double Gn[][], double[][] Cn, int maxQuantity) {
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

abstract class LossFunction {
   public abstract double L(double x);
   public abstract double Lc(double x);
}

class PoissonLossFunction extends LossFunction {

   private final PoissonDist poissonDistribution;

   public PoissonLossFunction(double lambda) {
       this.poissonDistribution = new PoissonDist(lambda);
   }

   @Override
   public double L(double r) {
       return Lc(r) - (r - this.poissonDistribution.getLambda());
   }

   @Override
   public double Lc(double r) {
      //return Stream.iterate(0, i -> i + 1).limit((int) r).mapToDouble(v -> (r - v)*poissonDistribution.prob(v)).sum();
      return (r-this.poissonDistribution.getLambda())*poissonDistribution.cdf(r)+this.poissonDistribution.getLambda()*poissonDistribution.prob((int) r);
   }
}

class NormalLossFunction extends LossFunction {

   private final NormalDist normalDistribution;

   public NormalLossFunction(double mean, double standardDeviation) {
       this.normalDistribution = new NormalDist(mean, standardDeviation);
   }

   @Override
   public double L(double r) {
      return Lc(r) - (r - this.normalDistribution.getMean());
   }

   @Override
   public double Lc(double r) {
       double mu = normalDistribution.getMean();
       double sigma = normalDistribution.getStandardDeviation();
       return sigma * (NormalDist.density01((r - mu)/sigma) + NormalDist.cdf01((r - mu)/sigma)*(r - mu)/sigma);
   }
}

abstract class MultiperiodNewsboy {

   protected double K;
   protected double h;
   protected double[] p;
   protected Distribution[] demand;
   protected double minETC;
   protected double optimalX;
   protected final double tolerance = 1e-4;
   protected final double probCutoff = 1-tolerance;

   public MultiperiodNewsboy() {
      super();
   }
   
   abstract double ETC(double x);
   
   protected void findOptimalOrderQuantity(double low, double high, double tolerance) {
      minETC = Double.MAX_VALUE;

      while (high - low > tolerance) {
          double mid1 = low + (high - low) / 3;
          double mid2 = high - (high - low) / 3;

          double etc1 = ETC(mid1);
          double etc2 = ETC(mid2);

          if (etc1 < etc2) {
              high = mid2;
          } else {
              low = mid1;
          }
      }

      optimalX = (low + high) / 2;
      minETC = ETC(optimalX);
  }
}

class PoissonMultiperiodNewsboy extends MultiperiodNewsboy {
   public PoissonMultiperiodNewsboy(double K, double h, double[] p, Distribution[] demand) {
      super();
      this.K = K;
      this.h = h;
      this.p = p;
      this.demand = demand;
      this.findOptimalOrderQuantity(0, new PoissonDist(Arrays.stream(demand).mapToDouble(d -> d.getMean()).sum()).inverseF(probCutoff), tolerance);
   }
   
   double ETC(double x) {
      double etc = this.K;
      double cumDemand = 0;
      for(int i = 0; i < demand.length; i++) {
         cumDemand += demand[i].getMean();
         PoissonLossFunction lf = new PoissonLossFunction(cumDemand);
         //NormalLossFunction lf = new NormalLossFunction(cumDemand, Math.sqrt(cumDemand)); // Normal approximation to Poisson
         etc += this.h*lf.Lc(x) + this.p[i]*lf.L(x); 
      }
      return etc;
   }
}

class NormalMultiperiodNewsboy extends MultiperiodNewsboy{   
   public NormalMultiperiodNewsboy(double K, double h, double[] p, Distribution[] demand) {
      super();
      this.K = K;
      this.h = h;
      this.p = p;
      this.demand = demand;
      this.findOptimalOrderQuantity(0, new NormalDist(Arrays.stream(demand).mapToDouble(d -> d.getMean()).sum(), Math.sqrt(Arrays.stream(demand).mapToDouble(d -> d.getVariance()).sum())).inverseF(probCutoff), tolerance);
   }
   
   double ETC(double x) {
      double etc = this.K;
      double cumDemandMean = 0;
      double cumDemandVar = 0;
      for(int i = 0; i < demand.length; i++) {
         cumDemandMean += demand[i].getMean();
         cumDemandVar += demand[i].getVariance();
         NormalLossFunction lf = new NormalLossFunction(cumDemandMean, Math.sqrt(cumDemandVar));
         etc += this.h*lf.Lc(x) + this.p[i]*lf.L(x); 
      }
      return etc;
   }
}

class ShortestPath {
   
   int V;
   
   public ShortestPath(double[][] connectionMatrix) {
      V = connectionMatrix.length;
      this.dijkstra(connectionMatrix, 0);
   }
   
   boolean[] shortestPathNodes;
   double shortestPathCost;
   
   // A utility function to find the vertex with minimum distance value,
   // from the set of vertices not yet included in shortest path tree
   
   int minDistance(double dist[], Boolean sptSet[])
   {
       // Initialize min value
       double min = Double.MAX_VALUE;
       int min_index = -1;
       
       for (int v = 0; v < V; v++)
           if (sptSet[v] == false && dist[v] <= min) {
               min = dist[v];
               min_index = v;
           }

       return min_index;
   }

   // Function that implements Dijkstra's single source shortest path
   // algorithm for a graph represented using adjacency matrix
   // representation
   void dijkstra(double graph[][], int src)
   {
       int pred[] = new int[V]; //Predecessors
       pred[0] = -1;
       double dist[] = new double[V]; // The output array. dist[i] will hold
       // the shortest distance from src to i

       // sptSet[i] will true if vertex i is included in shortest
       // path tree or shortest distance from src to i is finalized
       Boolean sptSet[] = new Boolean[V];

       // Initialize all distances as INFINITE and stpSet[] as false
       for (int i = 0; i < V; i++) {
           dist[i] = Double.MAX_VALUE;
           sptSet[i] = false;
       }

       // Distance of source vertex from itself is always 0
       dist[src] = 0;

       // Find shortest path for all vertices
       for (int count = 0; count < V - 1; count++) {
           // Pick the minimum distance vertex from the set of vertices
           // not yet processed. u is always equal to src in first
           // iteration.
           int u = minDistance(dist, sptSet);

           // Mark the picked vertex as processed
           sptSet[u] = true;

           // Update dist value of the adjacent vertices of the
           // picked vertex.
           for (int v = 0; v < V; v++) {

               // Update dist[v] only if is not in sptSet, there is an
               // edge from u to v, and total weight of path from src to
               // v through u is smaller than current value of dist[v]
               if (!sptSet[v] && graph[u][v] != 0 && 
                  dist[u] != Integer.MAX_VALUE && dist[u] + graph[u][v] < dist[v]) {
                   dist[v] = dist[u] + graph[u][v];
                   pred[v] = u;
               }
           }
       }

       this.shortestPathNodes = new boolean[V];
       this.shortestPathNodes[V-1] = true; 
       int p = pred[V-1]; 
       while(p != -1) {
          this.shortestPathNodes[p] = true;
          p = pred[p]; 
       }
       this.shortestPathCost = dist[V-1];
   }
}


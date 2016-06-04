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

package jsdp.app.inventory.univariate.simulation;

import jsdp.app.inventory.univariate.impl.BackwardRecursionImpl;
import jsdp.app.inventory.univariate.impl.StateDescriptorImpl;
import jsdp.app.inventory.univariate.impl.StateImpl;
import jsdp.utilities.SampleFactory;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.stat.Tally;

public class SimulatePolicies {
   
   public static double[] simulate_sS(
         Distribution[] demand, 
         double orderCost, 
         double holdingCost, 
         double penaltyCost,
         double unitCost,
         double initialStock,
         double[] S,
         double[] s,
         double confidence,
         double error
         ){
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
            stockPTally[t].add(Math.max(inventory, 0));
            stockNTally[t].add(Math.max(-inventory, 0));
         }
         costTally.add(replicationCost);
         if(i >= minRuns) costTally.confidenceIntervalNormal(confidence, centerAndRadius);
      }
      return centerAndRadius;
   }
   
   public static double[] simulateStochaticLotSizing(
         Distribution[] demand, 
         double orderCost, 
         double holdingCost, 
         double penaltyCost,
         double unitCost,
         double initialStock,
         BackwardRecursionImpl recursion,
         double confidence,
         double error
         ){
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
            StateDescriptorImpl state = new StateDescriptorImpl(t, StateImpl.stateToIntState(inventory));
            double qty = StateImpl.intStateToState(recursion.getOptimalAction(state).getIntAction());
            if(qty > 0){
               replicationCost += orderCost;
               replicationCost += qty*unitCost;
               inventory = qty+inventory-demandRealizations[t];
               replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
            }else{
               inventory = qty+inventory-demandRealizations[t];
               replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
            }
            stockPTally[t].add(Math.max(inventory, 0));
            stockNTally[t].add(Math.max(-inventory, 0));
         }
         costTally.add(replicationCost);
         if(i >= minRuns) costTally.confidenceIntervalNormal(confidence, centerAndRadius);
      }
      return centerAndRadius;
   }
}

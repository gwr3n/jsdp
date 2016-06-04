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

package jsdp.app.standalone;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.DoubleStream;

/**
 * Consider a 3-period inventory control problem. At the beginning of each period the firm should decide how many units of a 
 * product should be produced. If production takes place for x units, where x {@literal >} 0, we incur a production cost c(x). 
 * This cost comprises both a fix and a variable component: c(x) = 0, if x = 0; c(x) = 3+2x, otherwise.
 * 
 * Production in each period cannot exceed 4 units. Demand in each period takes two possible values: 1 or 2 units with equal 
 * probability (0.5). Demand is observed in each period only after production has occurred. After meeting current period's 
 * demand holding cost of $1 per unit is incurred for any item that is carried over from one period to the next. Because 
 * of limited capacity the inventory at the end of each period cannot exceed 3 units. All demand should be met on time 
 * (no backorders). If at the end of the planning horizon (i.e. period 3) the firm still has units in stock, these can be 
 * salvaged at $2 per unit. The initial inventory is 1 unit.
 * 
 * @author Roberto Rossi
 *
 */

public class InventoryControl {

   int planningHorizon;
   double[][] pmf;

   public InventoryControl(int planningHorizon,
                           double[][] pmf) {
      this.planningHorizon = planningHorizon;
      this.pmf = pmf;
   }
   
   class State{
      int period;
      int initialInventory;

      public State(int period, double initialInventory){
         this.period = period;
         this.initialInventory = (int) initialInventory;
      }

      public double[] getFeasibleActions(){
         return actionGenerator.apply(this);
      }
      
      @Override
      public int hashCode(){
         String hash = "";
         hash = (hash + period) + "_" + this.initialInventory;
         return hash.hashCode();
      }

      @Override
      public boolean equals(Object o){
         if(o instanceof State)
            return  ((State) o).period == this.period &&
                    ((State) o).initialInventory == this.initialInventory;
         else
            return false;
      }

      @Override
      public String toString(){
         return this.period + " " + this.initialInventory;
      }
   }
   
   Function<State, double[]> actionGenerator;
   
   @FunctionalInterface
   interface StateTransitionFunction <S, A, R> { 
      public S apply (S s, A a, R r);
   }
   
   public StateTransitionFunction<State, Double, Double> stateTransition;
   
   @FunctionalInterface
   interface ImmediateValueFunction <S, A, R, V> { 
      public V apply (S s, A a, R r);
   }

   public ImmediateValueFunction<State, Double, Double, Double> immediateValueFunction;
         
   Map<State, Double> cacheActions = new HashMap<>();
   Map<State, Double> cacheValueFunction = new HashMap<>();
   double f(State state){
      return cacheValueFunction.computeIfAbsent(state, s -> {
         double val= Arrays.stream(s.getFeasibleActions())
                           .map(orderQty -> Arrays.stream(pmf)
                                                  .mapToDouble(p -> p[1]*immediateValueFunction.apply(s, orderQty, p[0])+
                                                                    (s.period < this.planningHorizon ?
                                                                    p[1]*f(stateTransition.apply(s, orderQty, p[0])) : 0))
                                                  .sum())
                           .min()
                           .getAsDouble();
         double bestOrderQty = Arrays.stream(s.getFeasibleActions())
                                     .filter(orderQty -> Arrays.stream(pmf)
                                                               .mapToDouble(p -> p[1]*immediateValueFunction.apply(s, orderQty, p[0])+
                                                                                 (s.period < this.planningHorizon ?
                                                                                 p[1]*f(stateTransition.apply(s, orderQty, p[0])):0))
                                                               .sum() == val)
                                     .findAny()
                                     .getAsDouble();
         cacheActions.putIfAbsent(s, bestOrderQty);
         return val;
      });
   }
   
   public static void main(String [] args){
      int planningHorizon = 3;         //Planning horizon length
      double fixedProductionCost = 3;  //Fixed production cost
      double perUnitProductionCost = 2;//Per unit production cost
      int warehouseCapacity = 3;      //Production capacity
      double holdingCost = 1;          //Holding cost
      double salvageValue = 2;         //Salvage value
      int maxOrderQty = 4;             //Max order quantity
      /**
       * Probability mass function: Demand in each period takes two possible values: 1 or 2 units 
       * with equal probability (0.5).
       */
      double pmf[][] = {{1,0.5},{2,0.5}}; 
      int maxDemand =  (int) Arrays.stream(pmf).mapToDouble(v -> v[0]).max().getAsDouble();
      int minDemand =  (int) Arrays.stream(pmf).mapToDouble(v -> v[0]).min().getAsDouble();

      InventoryControl inventory = new InventoryControl(planningHorizon, pmf);
      
      /**
       * This function returns the set of actions associated with a given state
       */
      inventory.actionGenerator = state ->{
         int minQ = Math.max(maxDemand - state.initialInventory, 0);
         return DoubleStream.iterate(minQ, orderQty -> orderQty + 1)
                            .limit(Math.min(maxOrderQty, warehouseCapacity +  
                                            minDemand - state.initialInventory - minQ) + 1)
                            .toArray();
      };
      
      /**
       * State transition function; given a state, an action and a random outcome, the function
       * returns the future state
       */
      inventory.stateTransition = (state, action, randomOutcome) -> 
         inventory.new State(state.period + 1, state.initialInventory + action - randomOutcome);
      
      /**
       * Immediate value function for a given state
       */
         inventory.immediateValueFunction = (state, action, demand) -> {
            double cost = (action > 0 ? fixedProductionCost + perUnitProductionCost*action : 0);
            cost += holdingCost*(state.initialInventory+action-demand);
            cost -= (state.period == planningHorizon ? salvageValue : 0)*(state.initialInventory+action-demand);
            return cost;
         };
      
      /**
       * Initial problem conditions
       */
      int initialPeriod = 1;
      int initialInventory = 1;
      State initialState = inventory.new State(initialPeriod, initialInventory);
      
      /**
       * Run forward recursion and determine the expected total cost of an optimal policy
       */
      System.out.println("f_1("+initialInventory+")="+inventory.f(initialState));
      /**
       * Recover optimal action for period 1 when initial inventory at the beginning of period 1 is 1.
       */
      System.out.println("b_1("+initialInventory+")="+inventory.cacheActions.get(inventory.new State(initialPeriod, initialInventory)));
   }
}


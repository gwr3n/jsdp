package jsdp.app.standalone.deterministic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.DoubleStream;

/**
 * A forward recursion implementation of the classical Knapsack problem.
 * 
 * The decision maker is given a selection of {@code nbItems} types of items 
 * with weights {@code itemWeight} and profits {@code itemProfit}. 
 * 
 * The aim is to pick a combination of items (note that each type of items
 * can be picked multiple times) such that their total weight does not exceed 
 * {@code capacity} and total profit is maximized.
 * 
 * @author Roberto Rossi
 *
 */

public class Knapsack {
   int nbItems;
   double[] itemWeight;
   double[] itemProfit;
   double capacity;
   double[][] pmf;
   
   public Knapsack(int nbItems,
                   double[] itemWeight,
                   double[] itemProfit,
                   double capacity){
      this.nbItems = nbItems;
      this.itemWeight = itemWeight.clone();
      this.itemProfit = itemProfit.clone();
      this.capacity = capacity;
   }
   
   class State{
      int item;                           
      double remainingCapacity;            
      
      public State(int item, 
                   double remainingCapacity){
         this.item = item;
         this.remainingCapacity = remainingCapacity;
      }
      
      public double[] getFeasibleActions(){
         return actionGenerator.apply(this);
      }
      
      @Override
      public int hashCode(){
         String hash = "";
         hash = (hash + this.item) + "_" + this.remainingCapacity;
         return hash.hashCode();
      }
      
      @Override
      public boolean equals(Object o){
         if(o instanceof State)
            return  ((State) o).item == this.item &&
                    ((State) o).remainingCapacity == this.remainingCapacity;
         else
            return false;
      }

      @Override
      public String toString(){
         return this.item + " " + this.remainingCapacity;
      }
   }
   
   Function<State, double[]> actionGenerator;
   
   @FunctionalInterface
   interface StateTransitionFunction <S, A> { 
      public S apply (S s, A a);
   }
   
   public StateTransitionFunction<State, Double> stateTransition;

   @FunctionalInterface
   interface ImmediateValueFunction <S, A, V> { 
      public V apply (S s, A a);
   }

   public ImmediateValueFunction<State, Double, Double> immediateValueFunction;
   
   Map<State, Double> cacheActions = new HashMap<>();
   Map<State, Double> cacheValueFunction = new HashMap<>();
   double f(State state){
      return cacheValueFunction.computeIfAbsent(state, s -> {
         double val= Arrays.stream(s.getFeasibleActions())
                           .map(num -> immediateValueFunction.apply(s, num) + ((s.item < this.nbItems - 1) ? f(stateTransition.apply(s, num)) : 0))
                           .max()
                           .getAsDouble();
         double bestBet = Arrays.stream(s.getFeasibleActions())
                                .filter(num -> (immediateValueFunction.apply(s, num) + ((s.item < this.nbItems - 1) ? f(stateTransition.apply(s, num)) : 0)) == val)
                                .findAny()
                                .getAsDouble();
         cacheActions.putIfAbsent(s, bestBet);
         return val;
      });
   }
   
   public static void main(String [] args){
      int items = 3;          
      double[] itemWeight = {4, 3, 5};
      double[] itemProfit = {11, 7, 12};
      double capacity = 10;   

      Knapsack knapsack = new Knapsack(items, itemWeight, itemProfit, capacity);
      
      /**
       * This function returns the set of actions associated with a given state
       */
      knapsack.actionGenerator = state ->{
         return DoubleStream.iterate(0, num -> num + 1)
                            .limit((int) Math.floor(state.remainingCapacity/itemWeight[state.item]) + 1)
                            .toArray();
      };
      
      /**
       * State transition function; given a state, an action and a random outcome, the function
       * returns the future state
       */
      knapsack.stateTransition = (state, action) -> 
         knapsack.new State(state.item + 1, state.remainingCapacity - action*itemWeight[state.item]);
      
      /**
       * Immediate value function for a given state
       */
      knapsack.immediateValueFunction = (state, action) -> {
            return action*itemProfit[state.item];  
         };
      
      /**
       * Initial problem conditions
       */
      int initialItem = 0;
      State initialState = knapsack.new State(initialItem, capacity);
      
      /**
       * Run forward recursion and determine the probability of achieving the target wealth when
       * one follows an optimal policy
       */
      System.out.println("f_1(10)="+knapsack.f(initialState));
      /**
       * Recover optimal action for period 2 when initial wealth at the beginning of period 2 is $1.
       */
      System.out.println("b_1(10)="+knapsack.cacheActions.get(knapsack.new State(1, 10)));
   }
}

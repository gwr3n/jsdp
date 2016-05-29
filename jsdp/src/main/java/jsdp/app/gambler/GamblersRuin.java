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

package jsdp.app.gambler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.DoubleStream;

/**
 * This problem is taken from W. L. Winston, Operations Research: Applications and Algorithms (7th Edition), Duxbury Press, 2003,
 * chap. 19, example 3.
 * 
 * A gambler has ${@code initialWealth}. She is allowed to play a game of chance over a given {@code betHorizon}, and her goal is to
 * maximize her probability of ending up with a least ${@code targetWealth}. 
 * 
 * If the gambler bets $b on a play of the game, then with probability p, she wins the game and increases her capital position by $b; 
 * with probability (1-p), she loses the game and decreases her capital by $b. 
 * 
 * On any play of the game, the gambler may not bet more money than she has available.
 * 
 * Determine a betting strategy that will maximize the gambler's probability of attaining a wealth of at least ${@code targetWealth} 
 * by the end of the betting horizon. 
 * 
 * @author Roberto Rossi
 *
 */
public class GamblersRuin {

   double targetWealth;
   int betHorizon;
   double[][] pmf;

   public GamblersRuin(double targetWealth,
                       int betHorizon,
                       double[][] pmf) {
      this.targetWealth = targetWealth;
      this.betHorizon = betHorizon;
      this.pmf = pmf;
   }
   
   class State{
      int period;
      double money;

      public State(int period, double money){
         this.period = period;
         this.money = money;
      }

      public double[] getFeasibleActions(){
         return actionGenerator.apply(this);
      }
      
      @Override
      public int hashCode(){
         String hash = "";
         hash = (hash + period) + "_" + money;
         return hash.hashCode();
      }

      @Override
      public boolean equals(Object o){
         if(o instanceof State)
            return  ((State) o).period == this.period &&
                    ((State) o).money == this.money;
         else
            return false;
      }

      @Override
      public String toString(){
         return this.period + " " + this.money;
      }
   }
   
   Function<State, double[]> actionGenerator;
   
   @FunctionalInterface
   interface StateTransitionFunction <S, A, R> { 
      public S apply (S s, A a, R r);
   }
   
   public StateTransitionFunction<State, Double, Double> stateTransition;

   public Function<State, Double> immediateValueFunction;
         
   Map<State, Double> cacheActions = new HashMap<>();
   Map<State, Double> cacheValueFunction = new HashMap<>();
   double f(State state){
      return cacheValueFunction.computeIfAbsent(state, s -> {
         if(s.period == this.betHorizon + 1){
            return immediateValueFunction.apply(s);
         }else{
            double val= Arrays.stream(s.getFeasibleActions())
                              .map(bet -> Arrays.stream(pmf)
                                                .mapToDouble(p -> p[1]*immediateValueFunction.apply(s)+
                                                                  p[1]*f(stateTransition.apply(s, bet, p[0])))
                                                .sum())
                              .max()
                              .getAsDouble();
            double bestBet = Arrays.stream(s.getFeasibleActions())
                                   .filter(bet -> Arrays.stream(pmf)
                                                        .mapToDouble(p -> p[1]*immediateValueFunction.apply(s)+
                                                                          p[1]*f(stateTransition.apply(s, bet, p[0])))
                                                        .sum() == val)
                                   .findAny()
                                   .getAsDouble();
            cacheActions.putIfAbsent(s, bestBet);
            return val;
         }
      });
   }
   
   public static void main(String [] args){
      int bettingHorizon = 4;    //Planning horizon length
      double targetWealth = 6;   //Target wealth
  
      /**
       * Probability mass function: with probability 0.6 we lose the bet amount (multiplier is 0)
       * with probability 0.4 we double the bet amount (multiplier is 2)
       */
      double pmf[][] = {{0,0.6},{2,0.4}}; 

      GamblersRuin ruin = new GamblersRuin(targetWealth, bettingHorizon, pmf);
      
      /**
       * This function returns the set of actions associated with a given state
       */
      ruin.actionGenerator = state ->{
         return DoubleStream.iterate(0, bet -> bet + 1)
                            .limit((int) Math.ceil(Math.min(targetWealth/2, state.money + 1)))
                            .toArray();
      };
      
      /**
       * State transition function; given a state, an action and a random outcome, the function
       * returns the future state
       */
      ruin.stateTransition = (state, action, randomOutcome) -> 
         ruin.new State(state.period + 1, state.money - action + action*randomOutcome);
      
      /**
       * Immediate value function for a given state
       */
      ruin.immediateValueFunction = state -> {
            if(state.period == ruin.betHorizon + 1)
               return state.money >= ruin.targetWealth ? 1.0 : 0.0;    
            else
               return 0.0;   
         };
      
      /**
       * Initial problem conditions
       */
      int initialPeriod = 1;
      double initialWealth = 2;
      State initialState = ruin.new State(initialPeriod, initialWealth);
      
      /**
       * Run forward recursion and determine the probability of achieving the target wealth when
       * one follows an optimal policy
       */
      System.out.println("f_1(2)="+ruin.f(initialState));
      /**
       * Recover optimal action for period 2 when initial wealth at the beginning of period 2 is $1.
       */
      System.out.println("b_2(1)="+ruin.cacheActions.get(ruin.new State(2, 1)));
   }
}

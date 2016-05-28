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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

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
   double support[];
   double probability[];

   public GamblersRuin(double targetWealth,
                       int betHorizon,
                       double[] support, 
                       double[] probability) {
      this.targetWealth = targetWealth;
      this.betHorizon = betHorizon;
      this.support = support;
      this.probability = probability;
   }

   Map<StateDescriptor, Double> cacheb = new HashMap<>();

   Map<StateDescriptor, Double> cachef = new HashMap<>();
   double f(StateDescriptor state){
      return cachef.computeIfAbsent(state, y -> {
         if(y.period == this.betHorizon + 1){
            double val =  state.money >= this.targetWealth ? 1.0 : 0;	
            //System.out.println(state + " " +val);
            return val;
         }else{
            double val= IntStream.iterate(0, bet -> bet + 1)
                                 .limit((int) Math.ceil(y.money + 1))
                                 .mapToDouble(bet -> IntStream.iterate(0, p -> p + 1)
                                                              .limit(support.length)
                                                              .mapToDouble(p -> probability[p]*f(new StateDescriptor(y.period + 1, y.money - bet + bet*support[p])))
                                                              .sum())
                                 .max().getAsDouble();
            double bestBet = DoubleStream.iterate(0, bet -> bet + 1)
                                         .limit((int) Math.ceil(y.money + 1))
                                         .filter(bet -> IntStream.iterate(0, p -> p + 1)
                                                                 .limit(support.length)
                                                                 .mapToDouble(p -> probability[p]*f(new StateDescriptor(y.period + 1, y.money - bet + bet*support[p])))
                                                                 .sum() == val)
                                         .findAny().getAsDouble();
            cacheb.putIfAbsent(y, bestBet);
            return val;
         }
      });
   }	

   class StateDescriptor{
      int period;
      double money;

      public StateDescriptor(int period, double money){
         this.period = period;
         this.money = money;
      }

      public int hashCode(){
         String hash = "";
         hash = (hash + period) + "_" + money;
         return hash.hashCode();
      }

      @Override
      public boolean equals(Object o){
         if(o instanceof StateDescriptor)
            return  ((StateDescriptor) o).period == this.period &&
                    ((StateDescriptor) o).money == this.money;
         else
            return false;
      }

      @Override
      public String toString(){
         return this.period + " " + this.money;
      }
   }
   
   public static void main(String [] args){
      double support[] = {0,2};
      double probability[] = {0.6,0.4};
      int initialPeriod = 1;
      int bettingHorizon = 4;
      double initialWealth = 2;
      double targetWealth = 6;
      GamblersRuin ruin = new GamblersRuin(targetWealth, bettingHorizon, support, probability);
      StateDescriptor initialState = ruin.new StateDescriptor(initialPeriod, initialWealth);
      System.out.println("f_1(2)="+ruin.f(initialState));
      System.out.println("b_2(1)="+ruin.cacheb.get(ruin.new StateDescriptor(2, 1)));
   }
}

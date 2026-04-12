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

package jsdp.sdp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

/**
 * Spark-backed implementation of {@link ValueRepository}.
 * This class keeps value, optimal value, and optimal action caches in Spark RDDs.
 */
public class SparkValueRepository extends ValueRepository {

   private final JavaSparkContext sc;
   private volatile JavaPairRDD<StateAction, Double> valueRDD;
   private volatile JavaPairRDD<State, Double> optimalValueRDD;
   private volatile JavaPairRDD<State, Action> optimalActionRDD;

   public SparkValueRepository(ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                               double discountFactor,
                               JavaSparkContext sc) {
      this.setImmediateValue(immediateValueFunction);
      this.discountFactor = discountFactor;
      this.hashType = HashType.SPARK_RDD;
      this.sc = sc;
      this.valueRDD = sc.parallelizePairs(new ArrayList<Tuple2<StateAction, Double>>());
      this.optimalValueRDD = sc.parallelizePairs(new ArrayList<Tuple2<State, Double>>());
      this.optimalActionRDD = sc.parallelizePairs(new ArrayList<Tuple2<State, Action>>());
   }

   private synchronized void upsertValue(StateAction key, double value) {
      List<Tuple2<StateAction, Double>> singleton = new ArrayList<>();
      singleton.add(new Tuple2<>(key, Double.valueOf(value)));
      this.valueRDD = this.valueRDD.filter(t -> !t._1.equals(key)).union(sc.parallelizePairs(singleton));
   }

   private synchronized void upsertOptimalValue(State state, double value) {
      List<Tuple2<State, Double>> singleton = new ArrayList<>();
      singleton.add(new Tuple2<>(state, Double.valueOf(value)));
      this.optimalValueRDD = this.optimalValueRDD.filter(t -> !t._1.equals(state)).union(sc.parallelizePairs(singleton));
   }

   private synchronized void upsertOptimalAction(State state, Action action) {
      List<Tuple2<State, Action>> singleton = new ArrayList<>();
      singleton.add(new Tuple2<>(state, action));
      this.optimalActionRDD = this.optimalActionRDD.filter(t -> !t._1.equals(state)).union(sc.parallelizePairs(singleton));
   }

   @Override
   public Map<State, Double> getOptimalValueHashTable() {
      return this.optimalValueRDD.collectAsMap();
   }

   @Override
   public Map<State, Action> getOptimalActionHashTable() {
      return this.optimalActionRDD.collectAsMap();
   }

   @Override
   public Map<StateAction, Double> getValueHashTable() {
      return this.valueRDD.collectAsMap();
   }

   @Override
   public boolean containsOptimalExpectedValue(State state) {
      return !this.optimalValueRDD.lookup(state).isEmpty();
   }

   @Override
   public double getExpectedValue(State initialState, Action action, TransitionProbability transitionProbability) {
      StateAction key = new StateAction(initialState, action);
      List<Double> existing = this.valueRDD.lookup(key);
      if (!existing.isEmpty())
         return existing.get(0).doubleValue();

      double normalisationFactor = transitionProbability.getFinalStates(initialState, action).parallelStream()
            .mapToDouble(finalState -> transitionProbability.getTransitionProbability(initialState, action, finalState))
            .sum();
      double expectedValueComputed = transitionProbability.getFinalStates(initialState, action).parallelStream()
            .mapToDouble(finalState -> (this.immediateValueFunction.apply(initialState, action, finalState)
                  + this.discountFactor * this.getOptimalExpectedValue(finalState))
                  * transitionProbability.getTransitionProbability(initialState, action, finalState))
            .sum() / normalisationFactor;

      this.upsertValue(key, expectedValueComputed);
      return expectedValueComputed;
   }

   @Override
   public void setOptimalExpectedValue(State state, double expectedValue) {
      this.upsertOptimalValue(state, expectedValue);
   }

   @Override
   public double getOptimalExpectedValue(State state) {
      List<Double> existing = this.optimalValueRDD.lookup(state);
      if (existing.isEmpty())
         throw new NullPointerException("No optimal value associated with state");
      return existing.get(0).doubleValue();
   }

   @Override
   public void setOptimalAction(State state, Action action) {
      this.upsertOptimalAction(state, action);
   }

   @Override
   public Action getOptimalAction(State state) {
      List<Action> existing = this.optimalActionRDD.lookup(state);
      if (existing.isEmpty())
         return null;
      return existing.get(0);
   }
}

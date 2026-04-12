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
   // Fast-mode stage-local caches
   private volatile boolean fastMode = false;
   private transient java.util.concurrent.ConcurrentMap<StateAction, Double> stageValueMap;
   private transient java.util.concurrent.ConcurrentMap<State, Double> stageOptimalValueMap;
   private transient java.util.concurrent.ConcurrentMap<State, Action> stageOptimalActionMap;
   // Per-stage driver-side cache to avoid repeated collect/lookup during inner loops
   private transient java.util.Map<State, Double> optimalValueCache;

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

   /**
    * Optional constructor to enable fast mode.
    */
   public SparkValueRepository(ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                               double discountFactor,
                               JavaSparkContext sc,
                               boolean fastMode) {
      this(immediateValueFunction, discountFactor, sc);
      this.fastMode = fastMode;
      if (fastMode) {
         this.stageValueMap = new java.util.concurrent.ConcurrentHashMap<>();
         this.stageOptimalValueMap = new java.util.concurrent.ConcurrentHashMap<>();
         this.stageOptimalActionMap = new java.util.concurrent.ConcurrentHashMap<>();
      }
   }

   private synchronized void upsertValue(StateAction key, double value) {
      if (fastMode && stageValueMap != null) {
         stageValueMap.put(key, Double.valueOf(value));
         return;
      }
      List<Tuple2<StateAction, Double>> singleton = new ArrayList<>();
      singleton.add(new Tuple2<>(key, Double.valueOf(value)));
      this.valueRDD = this.valueRDD.filter(t -> !t._1.equals(key)).union(sc.parallelizePairs(singleton));
   }

   private synchronized void upsertOptimalValue(State state, double value) {
      if (fastMode && stageOptimalValueMap != null) {
         stageOptimalValueMap.put(state, Double.valueOf(value));
         return;
      }
      List<Tuple2<State, Double>> singleton = new ArrayList<>();
      singleton.add(new Tuple2<>(state, Double.valueOf(value)));
      this.optimalValueRDD = this.optimalValueRDD.filter(t -> !t._1.equals(state)).union(sc.parallelizePairs(singleton));
   }

   private synchronized void upsertOptimalAction(State state, Action action) {
      if (fastMode && stageOptimalActionMap != null) {
         stageOptimalActionMap.put(state, action);
         return;
      }
      List<Tuple2<State, Action>> singleton = new ArrayList<>();
      singleton.add(new Tuple2<>(state, action));
      this.optimalActionRDD = this.optimalActionRDD.filter(t -> !t._1.equals(state)).union(sc.parallelizePairs(singleton));
   }

   @Override
   public Map<State, Double> getOptimalValueHashTable() {
      if (fastMode && stageOptimalValueMap != null && !stageOptimalValueMap.isEmpty()) {
         java.util.Map<State, Double> merged = this.optimalValueRDD.collectAsMap();
         merged.putAll(stageOptimalValueMap);
         return merged;
      }
      return this.optimalValueRDD.collectAsMap();
   }

   @Override
   public Map<State, Action> getOptimalActionHashTable() {
      if (fastMode && stageOptimalActionMap != null && !stageOptimalActionMap.isEmpty()) {
         java.util.Map<State, Action> merged = this.optimalActionRDD.collectAsMap();
         merged.putAll(stageOptimalActionMap);
         return merged;
      }
      return this.optimalActionRDD.collectAsMap();
   }

   @Override
   public Map<StateAction, Double> getValueHashTable() {
      if (fastMode && stageValueMap != null && !stageValueMap.isEmpty()) {
         java.util.Map<StateAction, Double> merged = this.valueRDD.collectAsMap();
         merged.putAll(stageValueMap);
         return merged;
      }
      return this.valueRDD.collectAsMap();
   }

   @Override
   public boolean containsOptimalExpectedValue(State state) {
      if (fastMode && stageOptimalValueMap != null && stageOptimalValueMap.containsKey(state))
         return true;
      return !this.optimalValueRDD.lookup(state).isEmpty();
   }

   @Override
   public double getExpectedValue(State initialState, Action action, TransitionProbability transitionProbability) {
      StateAction key = new StateAction(initialState, action);
      if (fastMode && stageValueMap != null && stageValueMap.containsKey(key))
         return stageValueMap.get(key).doubleValue();
      List<Double> existing = this.valueRDD.lookup(key);
      if (!existing.isEmpty())
         return existing.get(0).doubleValue();

      // Avoid repeated Spark lookups for optimal values by using a driver-side snapshot
      java.util.List<State> finals = transitionProbability.getFinalStates(initialState, action);
      double normalisationFactor = finals.parallelStream()
         .mapToDouble(finalState -> transitionProbability.getTransitionProbability(initialState, action, finalState))
         .sum();
      double expectedValueComputed = finals.parallelStream()
         .mapToDouble(finalState -> {
            double optVal;
            if (this.optimalValueCache != null && this.optimalValueCache.containsKey(finalState))
            optVal = this.optimalValueCache.get(finalState).doubleValue();
            else
            optVal = this.getOptimalExpectedValue(finalState);
            return (this.immediateValueFunction.apply(initialState, action, finalState)
               + this.discountFactor * optVal)
               * transitionProbability.getTransitionProbability(initialState, action, finalState);
         })
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
      if (fastMode && stageOptimalValueMap != null && stageOptimalValueMap.containsKey(state))
         return stageOptimalValueMap.get(state).doubleValue();
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
      if (fastMode && stageOptimalActionMap != null && stageOptimalActionMap.containsKey(state))
         return stageOptimalActionMap.get(state);
      List<Action> existing = this.optimalActionRDD.lookup(state);
      if (existing.isEmpty())
         return null;
      return existing.get(0);
   }

   /**
    * Begin a recursion stage: initialize/clear stage-local caches.
    */
   public void beginStage() {
      if (!fastMode)
         return;
      if (this.stageValueMap == null)
         this.stageValueMap = new java.util.concurrent.ConcurrentHashMap<>();
      else
         this.stageValueMap.clear();
      if (this.stageOptimalValueMap == null)
         this.stageOptimalValueMap = new java.util.concurrent.ConcurrentHashMap<>();
      else
         this.stageOptimalValueMap.clear();
      if (this.stageOptimalActionMap == null)
         this.stageOptimalActionMap = new java.util.concurrent.ConcurrentHashMap<>();
      else
         this.stageOptimalActionMap.clear();
      // Populate a driver-side snapshot of optimal values for fast lookups
      this.optimalValueCache = this.optimalValueRDD.collectAsMap();
   }

   /**
    * End a recursion stage: materialize stage-local entries into RDDs in batch.
    */
   public void endStage() {
      if (!fastMode)
         return;
      // materialize values
      if (this.stageValueMap != null && !this.stageValueMap.isEmpty()) {
         List<Tuple2<StateAction, Double>> entries = new ArrayList<>();
         for (java.util.Map.Entry<StateAction, Double> e : this.stageValueMap.entrySet())
            entries.add(new Tuple2<>(e.getKey(), e.getValue()));
         this.valueRDD = this.valueRDD.union(sc.parallelizePairs(entries));
         this.stageValueMap.clear();
      }
      if (this.stageOptimalValueMap != null && !this.stageOptimalValueMap.isEmpty()) {
         List<Tuple2<State, Double>> entries = new ArrayList<>();
         for (java.util.Map.Entry<State, Double> e : this.stageOptimalValueMap.entrySet())
            entries.add(new Tuple2<>(e.getKey(), e.getValue()));
         this.optimalValueRDD = this.optimalValueRDD.union(sc.parallelizePairs(entries));
         this.stageOptimalValueMap.clear();
      }
      if (this.stageOptimalActionMap != null && !this.stageOptimalActionMap.isEmpty()) {
         List<Tuple2<State, Action>> entries = new ArrayList<>();
         for (java.util.Map.Entry<State, Action> e : this.stageOptimalActionMap.entrySet())
            entries.add(new Tuple2<>(e.getKey(), e.getValue()));
         this.optimalActionRDD = this.optimalActionRDD.union(sc.parallelizePairs(entries));
         this.stageOptimalActionMap.clear();
      }
         // Clear driver-side cache after materialization
         if (this.optimalValueCache != null)
            this.optimalValueCache = null;
   }

   public void setFastMode(boolean enabled) {
      this.fastMode = enabled;
      if (enabled) {
         if (this.stageValueMap == null)
            this.stageValueMap = new java.util.concurrent.ConcurrentHashMap<>();
         if (this.stageOptimalValueMap == null)
            this.stageOptimalValueMap = new java.util.concurrent.ConcurrentHashMap<>();
         if (this.stageOptimalActionMap == null)
            this.stageOptimalActionMap = new java.util.concurrent.ConcurrentHashMap<>();
      }
   }
}

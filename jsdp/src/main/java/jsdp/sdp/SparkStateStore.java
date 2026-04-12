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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

/**
 * A {@code StateStore} implementation backed by an Apache Spark {@code JavaPairRDD}.
 * Each {@code put} appends a new entry via {@code union}; lookups are resolved via
 * {@code JavaPairRDD.lookup}, which exploits Spark's distributed execution.
 * {@code entrySet()} collects the full RDD to the driver, so it is suitable for use
 * with the existing {@code BackwardRecursion} loop without modification.
 *
 * <p>The state descriptor type {@code SD} and all {@code State} subclasses must be
 * {@code Serializable} for Spark to distribute them across partitions.
 *
 * @author Roberto Rossi
 *
 * @param <SD> the state descriptor type; must be {@link Serializable}.
 */
public class SparkStateStore<SD extends Serializable> implements StateStore<SD> {

   private final JavaSparkContext sc;
   private volatile JavaPairRDD<SD, State> rdd;
   // Fast-mode: stage-local cache to avoid frequent RDD unions/lookups
   private volatile boolean fastMode = false;
   private transient java.util.concurrent.ConcurrentMap<SD, State> stageLocal;

   /**
    * Constructs an empty {@code SparkStateStore} using the given Spark context.
    *
    * @param sc the active {@code JavaSparkContext}.
    */
   public SparkStateStore(JavaSparkContext sc) {
      this.sc = sc;
      List<Tuple2<SD, State>> emptyList = new ArrayList<>();
      this.rdd = sc.parallelizePairs(emptyList);
   }

   /**
    * Optional constructor to enable fast mode.
    */
   public SparkStateStore(JavaSparkContext sc, boolean fastMode) {
      this(sc);
      this.fastMode = fastMode;
      if (fastMode)
         this.stageLocal = new java.util.concurrent.ConcurrentHashMap<>();
   }

   /**
    * {@inheritDoc}
    * Performs a distributed lookup via {@code JavaPairRDD.lookup}.
    */
   @Override
   public State get(SD descriptor) {
      if (fastMode && stageLocal != null) {
         State s = stageLocal.get(descriptor);
         if (s != null)
            return s;
      }
      List<State> results = rdd.lookup(descriptor);
      return results.isEmpty() ? null : results.get(0);
   }

   /**
    * {@inheritDoc}
    * Appends the new mapping via {@code JavaPairRDD.union}.
    */
   @Override
   public void put(SD descriptor, State state) {
      if (fastMode && stageLocal != null) {
         stageLocal.put(descriptor, state);
         return;
      }
      List<Tuple2<SD, State>> singleton = new ArrayList<>();
      singleton.add(new Tuple2<>(descriptor, state));
      rdd = rdd.union(sc.parallelizePairs(singleton));
   }

   /**
    * {@inheritDoc}
    * Collects the full RDD to the driver and returns its entry set.
    * This triggers a Spark action; use sparingly on large state spaces.
    */
   @Override
   public Set<Map.Entry<SD, State>> entrySet() {
      // Collect the RDD view to a driver map. The map returned by
      // `collectAsMap()` may be unmodifiable, so copy it into a mutable
      // HashMap before merging or returning its entry set.
      java.util.Map<SD, State> driverMap = rdd.collectAsMap();
      if (fastMode && stageLocal != null && !stageLocal.isEmpty()) {
         java.util.Map<SD, State> merged = new java.util.HashMap<>(driverMap);
         merged.putAll(stageLocal);
         return merged.entrySet();
      }
      return new java.util.HashMap<>(driverMap).entrySet();
   }

   /**
    * Enable or disable fast mode. If enabling, initialize the stage-local cache.
    */
   public void setFastMode(boolean enabled) {
      this.fastMode = enabled;
      if (enabled && this.stageLocal == null)
         this.stageLocal = new java.util.concurrent.ConcurrentHashMap<>();
   }

   /**
    * Begin a recursion stage: initialize/clear the stage-local cache.
    */
   public void beginStage() {
      if (!fastMode)
         return;
      if (this.stageLocal == null)
         this.stageLocal = new java.util.concurrent.ConcurrentHashMap<>();
      else
         this.stageLocal.clear();
   }

   /**
    * End a recursion stage: materialize stage-local entries into the RDD in a single union.
    */
   public void endStage() {
      if (!fastMode || this.stageLocal == null || this.stageLocal.isEmpty())
         return;
      List<Tuple2<SD, State>> entries = new ArrayList<>();
      for (java.util.Map.Entry<SD, State> e : this.stageLocal.entrySet())
         entries.add(new Tuple2<>(e.getKey(), e.getValue()));
      this.rdd = this.rdd.union(sc.parallelizePairs(entries));
      this.stageLocal.clear();
   }
}

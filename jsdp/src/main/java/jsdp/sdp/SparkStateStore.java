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
    * {@inheritDoc}
    * Performs a distributed lookup via {@code JavaPairRDD.lookup}.
    */
   @Override
   public State get(SD descriptor) {
      List<State> results = rdd.lookup(descriptor);
      return results.isEmpty() ? null : results.get(0);
   }

   /**
    * {@inheritDoc}
    * Appends the new mapping via {@code JavaPairRDD.union}.
    */
   @Override
   public void put(SD descriptor, State state) {
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
      return rdd.collectAsMap().entrySet();
   }
}

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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.spark.api.java.JavaSparkContext;

/**
 * An abstract container that stores all generated {@code State}.
 * 
 * @author Roberto Rossi
 *
 * @param <SD> a specific state descriptor for the problem at hand.
 */
public abstract class StateSpace<SD> implements Iterable<State>{
	
	protected int period;
	protected StateStore<SD> stateStore;
	
   protected static Function<State, ArrayList<Action>> buildActionList;
   protected static Function<State, Action> idempotentAction;
   
   public static Function<State, ArrayList<Action>> getBuildActionList(){
      return buildActionList;
   }
   
   public static Function<State, Action> getIdempotentAction(){
      return idempotentAction;
   }
	
	/**
	 * Constructs a container for states associated with a given {@code period}. 
	 * Do not use {@code ConcurrentHashMap} in conjunction with forward recursion.
	 * 
	 * @param period the period associated with this container.
	 * @param hash the type of hash used to store the state space
	 */
	public StateSpace(int period, HashType hash){
		this.period = period;
		this.stateStore = new DefaultMapStateStore<>(hash);
	}
	
   /**
    * Constructs a container for states associated with a given {@code period}. 
    * Do not use {@code ConcurrentHashMap} in conjunction with forward recursion.
    * 
    * @param period the period associated with this container.
    * @param hash the type of hash used to store the state space
    * @param stateSpaceSizeLowerBound a lower bound for the sdp state space size, used to initialise the internal hash maps
    * @param loadFactor the internal hash maps load factor
    */
   public StateSpace(int period, HashType hash, int stateSpaceSizeLowerBound, float loadFactor){
      this.period = period;
      this.stateStore = new DefaultMapStateStore<>(hash, stateSpaceSizeLowerBound, loadFactor);
   }

   /**
    * Constructs a container for states associated with a given {@code period} using
    * a custom {@code StateStore} implementation.
    * Use this constructor to supply alternative storage backends (e.g. Apache Spark RDDs).
    *
    * @param period the period associated with this container.
    * @param store  the backing store implementation.
    */
   protected StateSpace(int period, StateStore<SD> store){
      this.period = period;
      this.stateStore = store;
   }

   /**
    * Constructs a container for states associated with a given {@code period},
    * routing to {@link SparkStateStore} when {@code hash} is {@link HashType#SPARK_RDD}.
    * For all other {@code HashType} values the Spark context is ignored and the
    * standard {@link DefaultMapStateStore} is used.
    *
    * @param period the period associated with this container.
    * @param hash   the storage type; use {@link HashType#SPARK_RDD} for Spark-backed storage.
    * @param sc     the active {@code JavaSparkContext}; required when {@code hash == SPARK_RDD}.
    */
   @SuppressWarnings("unchecked")
   public StateSpace(int period, HashType hash, JavaSparkContext sc){
      this.period = period;
      if(hash == HashType.SPARK_RDD){
         // Construct SparkStateStore with fast-mode enabled by default
         this.stateStore = (StateStore<SD>) new SparkStateStore<>(sc, true);
      } else {
         this.stateStore = new DefaultMapStateStore<>(hash);
      }
   }
	
	/**
	 * Returns the {@code State} associated with a given state descriptor.
	 * 
	 * @param descriptor the state descriptor.
	 * @return the {@code State} associated with {@code descriptor}.
	 */
	public abstract State getState(SD descriptor);
	
	/**
	 * Returns the period associated with this container.
	 * 
	 * @return the period associated with this container.
	 */
	public int getPeriod(){
		return period;
	}
	
	/**
	 * Returns the key entry set associated with this container.
	 * 
	 * @return the set of state descriptors that are keys in the states {@code Hashtable}.
	 */
	public Set<Map.Entry<SD,State>> entrySet(){
		return stateStore.entrySet();
	}
}

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

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gnu.trove.map.hash.THashMap;
import jsdp.utilities.hash.MapDBHashTable;
import jsdp.utilities.hash.MapDBHashTable.Storage;

/**
 * An abstraction representing a repository for the value associated with each {@code State}.
 *  
 * @author Roberto Rossi
 *
 */
public class ValueRepository {	
	protected Map<StateAction,Double> valueHashTable;
	protected Map<State,Double> optimalValueHashTable;
	protected Map<State,Action> optimalActionHashTable;
	
	protected double discountFactor;
	
	protected ImmediateValueFunction<State, Action, Double> immediateValueFunction;
	
	/**
	 * Creates a new value repository. Do not use {@code ConcurrentHashMap} in conjunction with forward recursion.
	 * 
	 * @param immediateValueFunction the immediate value of a transition from {@code initialState} to 
	 * {@code finalState} under a chosen {@code action}.
	 * @param discountFactor the value function discount factor
	 * @param hash the type of hash used to store the state space
	 */
	public ValueRepository(ImmediateValueFunction<State, Action, Double> immediateValueFunction, double discountFactor, HashType hash){
	   this.setImmediateValue(immediateValueFunction);
	   this.discountFactor = discountFactor;
	   switch(hash){
         case HASHTABLE:
            this.valueHashTable = new Hashtable<StateAction,Double>();
            this.optimalValueHashTable = new Hashtable<State,Double>();
            this.optimalActionHashTable = new Hashtable<State,Action>();
            break;
         case CONCURRENT_HASHMAP:
            this.valueHashTable = Collections.synchronizedMap(new ConcurrentHashMap<StateAction,Double>());
            this.optimalValueHashTable = Collections.synchronizedMap(new ConcurrentHashMap<State,Double>());
            this.optimalActionHashTable = Collections.synchronizedMap(new ConcurrentHashMap<State,Action>());
            break;
         case THASHMAP:
            this.valueHashTable = Collections.synchronizedMap(new THashMap<StateAction,Double>());
            this.optimalValueHashTable = Collections.synchronizedMap(new THashMap<State,Double>());
            this.optimalActionHashTable = Collections.synchronizedMap(new THashMap<State,Action>());
            break;
         case MAPDB_HEAP:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.HEAP);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.HEAP);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.HEAP);
            break;
         case MAPDB_HEAP_SHARDED:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.HEAP_SHARDED);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.HEAP_SHARDED);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.HEAP_SHARDED);
            break;
         case MAPDB_MEMORY:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.MEMORY);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.MEMORY);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.MEMORY);
            break;
         case MAPDB_MEMORY_SHARDED:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.MEMORY_SHARDED);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.MEMORY_SHARDED);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.MEMORY_SHARDED);
            break;
         case MAPDB_DISK:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.DISK);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.DISK);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.DISK);
            break;   
         default: 
            throw new NullPointerException("HashType not available");   
	   }
	}
	
	/**
	 * Creates a new value repository. Do not use {@code ConcurrentHashMap} in conjunction with forward recursion.
	 * 
	 * @param immediateValueFunction the immediate value of a transition from {@code initialState} to 
    * {@code finalState} under a chosen {@code action}.
    * @param discountFactor the value function discount factor
	 * @param stateSpaceSizeLowerBound a lower bound for the sdp state space size, used to initialise the internal hash maps
	 * @param loadFactor the internal hash maps load factor
	 * @param hash the type of hash used to store the state space
	 */
	public ValueRepository(ImmediateValueFunction<State, Action, Double> immediateValueFunction, double discountFactor, int stateSpaceSizeLowerBound, float loadFactor, HashType hash){
	   this.setImmediateValue(immediateValueFunction);
      this.discountFactor = discountFactor;
      switch(hash){
         case HASHTABLE:
            this.valueHashTable = new Hashtable<StateAction,Double>(stateSpaceSizeLowerBound,loadFactor);
            this.optimalValueHashTable = new Hashtable<State,Double>(stateSpaceSizeLowerBound,loadFactor);
            this.optimalActionHashTable = new Hashtable<State,Action>(stateSpaceSizeLowerBound,loadFactor);
            break;
         case CONCURRENT_HASHMAP:
            this.valueHashTable = Collections.synchronizedMap(new ConcurrentHashMap<StateAction,Double>(stateSpaceSizeLowerBound,loadFactor));
            this.optimalValueHashTable = Collections.synchronizedMap(new ConcurrentHashMap<State,Double>(stateSpaceSizeLowerBound,loadFactor));
            this.optimalActionHashTable = Collections.synchronizedMap(new ConcurrentHashMap<State,Action>(stateSpaceSizeLowerBound,loadFactor));
            break;
         case THASHMAP:
            this.valueHashTable = Collections.synchronizedMap(new THashMap<StateAction,Double>(stateSpaceSizeLowerBound,loadFactor));
            this.optimalValueHashTable = Collections.synchronizedMap(new THashMap<State,Double>(stateSpaceSizeLowerBound,loadFactor));
            this.optimalActionHashTable = Collections.synchronizedMap(new THashMap<State,Action>(stateSpaceSizeLowerBound,loadFactor));
            break;
         case MAPDB_HEAP:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.HEAP);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.HEAP);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.HEAP);
            break;
         case MAPDB_HEAP_SHARDED:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.HEAP_SHARDED);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.HEAP_SHARDED);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.HEAP_SHARDED);
            break;
         case MAPDB_MEMORY:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.MEMORY);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.MEMORY);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.MEMORY);
            break;
         case MAPDB_MEMORY_SHARDED:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.MEMORY_SHARDED);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.MEMORY_SHARDED);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.MEMORY_SHARDED);
            break;
         case MAPDB_DISK:
            this.valueHashTable = new MapDBHashTable<StateAction,Double>("valueHashTable", Storage.DISK);
            this.optimalValueHashTable = new MapDBHashTable<State,Double>("optimalValueHashTable", Storage.DISK);
            this.optimalActionHashTable = new MapDBHashTable<State,Action>("optimalActionHashTable", Storage.DISK);
            break;   
         default: 
            throw new NullPointerException("HashType not available");   
      }
   }
	
	protected ValueRepository(){}
	
	/**
	 * Returns the hashtable storing optimal state values.
	 * 
	 * @return the hashtable storing optimal state values
	 */
	public Map<State,Double> getOptimalValueHashTable(){
	   return this.optimalValueHashTable;
	}
	
	/**
    * Returns the hashtable storing optimal actions.
    * 
    * @return the hashtable storing optimal actions
    */
	public Map<State,Action> getOptimalActionHashTable(){
	   return this.optimalActionHashTable;
	}
	
	
	public Map<StateAction,Double> getValueHashTable(){
	   return this.valueHashTable;
	}
	
	/**
	 * Returns the immediate value of a transition from {@code initialState} to {@code finalState} under a chosen {@code action}.
	 * 
	 * @param initialState the initial state of the stochastic process.
	 * @param action the chosen action.
	 * @param finalState the final state of the stochastic process.
	 * @return the immediate value of a transition from {@code initialState} to {@code finalState} under a chosen {@code action}.
	 */
	public double getImmediateValue(State initialState, Action action, State finalState) {
      return this.immediateValueFunction.apply(initialState, action, finalState);
   }
	
	/**
    * Sets the immediate value function of a transition from {@code initialState} to {@code finalState} under a chosen {@code action}.
    * 
    * @param immediateValueFunction the immediate value of a transition from {@code initialState} to 
    * {@code finalState} under a chosen {@code action}.
    */
	public void setImmediateValue(ImmediateValueFunction<State, Action, Double> immediateValueFunction) {
      this.immediateValueFunction = immediateValueFunction;
   }
	
	/**
	 * Returns the discount factor for the problem value function
	 * 
	 * @return the discount factor
	 */
	public double getDiscountFactor(){
      return this.discountFactor;
   }
	
	/**
	 * Returns the expected value associated with {@code initialState} and {@code action} under one-step transition probabilities
	 * described in {@code transitionProbability}.
	 * 
	 * @param initialState the initial state of the stochastic process.
	 * @param action the chosen action. 
	 * @param transitionProbability the transition probabilities of the stochastic process.
	 * @return the expected value associated with {@code initialState} and {@code action} under one-step transition probabilities
	 * described in {@code transitionProbability}.
	 */
	public double getExpectedValue(State initialState, Action action, TransitionProbability transitionProbability) {
      StateAction key = new StateAction(initialState, action);
      return this.valueHashTable.computeIfAbsent(key, y -> {
         double normalisationFactor = transitionProbability.getFinalStates(initialState, action).parallelStream()
                 .mapToDouble(finalState -> transitionProbability.getTransitionProbability(initialState, action, finalState))
                 .sum();
         double expectedValue = transitionProbability.getFinalStates(initialState, action).parallelStream()
                 .mapToDouble(finalState -> 
                 (this.immediateValueFunction.apply(initialState, action, finalState)+this.discountFactor*this.getOptimalExpectedValue(finalState))*
                 transitionProbability.getTransitionProbability(initialState, action, finalState)
         ).sum()/normalisationFactor;
         return expectedValue;
      });
   }
	
	/**
	 * Associates an optimal expected value {@code expectedValue} to {@code state}.
	 * 
	 * @param state the target state.
	 * @param expectedValue the optimal expected total cost.
	 */
	public void setOptimalExpectedValue(State state, double expectedValue){
		this.optimalValueHashTable.put(state, Double.valueOf(expectedValue));
	}
	
	/**
	 * Returns the optimal expected value associated with {@code state}.
	 * 
	 * @param state the target state.
	 * @return the optimal expected value.
	 */
	public double getOptimalExpectedValue(State state){
		return this.optimalValueHashTable.get(state).doubleValue();
	}
	
	/**
	 * Associates an optimal action {@code action} to state {@code state}.
	 * 
	 * @param state the target state.
	 * @param action the optimal action.
	 */
	public void setOptimalAction(State state, Action action){
		this.optimalActionHashTable.put(state, action);
	}
	
	/**
	 * Returns the optimal action associated with {@code state}.
	 * 
	 * @param state the target state.
	 * @return the optimal action.
	 */
	public Action getOptimalAction(State state){
		return this.optimalActionHashTable.get(state);
	}
}
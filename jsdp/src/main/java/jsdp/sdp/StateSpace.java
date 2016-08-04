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
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import gnu.trove.map.hash.THashMap;
import jsdp.utilities.hash.MapDBHashTable;
import jsdp.utilities.hash.MapDBHashTable.Storage;

/**
 * An abstract container that stores all generated {@code State}.
 * 
 * @author Roberto Rossi
 *
 * @param <SD> a specific state descriptor for the problem at hand.
 */
public abstract class StateSpace<SD> implements Iterable<State>{
	
	protected int period;
	protected Map<SD,State> states;
	
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
	 * @param the type of hash used to store the state space
	 */
	public StateSpace(int period, HashType hash){
		this.period = period;
		switch(hash){
		case HASHTABLE:
		   this.states = new Hashtable<SD,State>();
		   break;
		case CONCURRENT_HASHMAP:
		   this.states = new ConcurrentHashMap<SD,State>();
		   break;
		case THASHMAP:
		   this.states = Collections.synchronizedMap(new THashMap<SD,State>());
		   break;
		case MAPDB_MEMORY:
         this.states = new MapDBHashTable<SD,State>("states", Storage.MEMORY);
         break;   
		case MAPDB_DISK:
         this.states = new MapDBHashTable<SD,State>("states", Storage.DISK);
         break;      
      default: 
         throw new NullPointerException("HashType not available");
		}
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
      switch(hash){
         case HASHTABLE:
            states = new Hashtable<SD,State>(stateSpaceSizeLowerBound,loadFactor);
            break;
         case CONCURRENT_HASHMAP:
            states = new ConcurrentHashMap<SD,State>(stateSpaceSizeLowerBound,loadFactor);
            break;
         case THASHMAP:
            states = Collections.synchronizedMap(new THashMap<SD,State>(stateSpaceSizeLowerBound,loadFactor));
            break;
         case MAPDB_MEMORY:
            this.states = new MapDBHashTable<SD,State>("states", Storage.MEMORY);
            break;   
         case MAPDB_DISK:
            this.states = new MapDBHashTable<SD,State>("states", Storage.DISK);
            break;    
         default: 
            throw new NullPointerException("HashType not available");   
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
		return states.entrySet();
	}
}

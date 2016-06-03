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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import gnu.trove.map.hash.THashMap;

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
	
	protected Function<State, ArrayList<Action>> buildActionList;
	protected Function<State, Action> idempotentAction;
	
	/**
	 * Constructs a container for states associated with a given {@code period}. This implementation is based on {@code ConcurrentHashMap}.
	 * 
	 * @param period the period associated with this container.
	 */
	public StateSpace(int period){
		this.period = period;
		states = new ConcurrentHashMap<SD,State>();
	}
	
	/**
	 * Constructs a container for states associated with a given {@code period}. This implementation is based on Trove4j {@code THashMap}.
	 * 
	 * @param period the period associated with this container.
	 * @param stateSpaceSizeLowerBound a lower bound for the sdp state space size, used to initialise the internal hash maps
	 * @param loadFactor the internal hash maps load factor
	 */
	public StateSpace(int period, int stateSpaceSizeLowerBound, float loadFactor){
      this(period);
      states = new THashMap<SD,State>(stateSpaceSizeLowerBound,loadFactor);
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

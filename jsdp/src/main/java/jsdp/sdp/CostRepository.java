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

import java.util.Hashtable;

public abstract class CostRepository {	
	protected Hashtable<StateAction,Double> costHashTable = new Hashtable<StateAction,Double>();
	protected Hashtable<State,Double> optimalCostHashTable = new Hashtable<State,Double>();
	protected Hashtable<State,Action> optimalActionHashTable = new Hashtable<State,Action>();
	
	protected abstract double getImmediateCost(State initialState, Action action, State finalState);
	public abstract double getExpectedCost(State initialState, Action action, TransitionProbability transitionProbability);
	
	public synchronized void setOptimalExpectedCost(State state, double expectedCost){
		this.optimalCostHashTable.put(state, new Double(expectedCost));
	}
	
	public double getOptimalExpectedCost(State state){
		return this.optimalCostHashTable.get(state).doubleValue();
	}
	
	public synchronized void setOptimalAction(State state, Action action){
		this.optimalActionHashTable.put(state, action);
	}
	
	public Action getOptimalAction(State state){
		return this.optimalActionHashTable.get(state);
	}
	
	protected class StateAction{
		State initialState;
		Action action;
		
		public StateAction(State initialState, Action action){
			this.initialState = initialState;
			this.action = action; 
		}
		
		public boolean equals(StateAction stateAction){
			return this.initialState.equals(stateAction.initialState) && this.action.equals(stateAction.action);
		}
		
		public int hashCode(){
			String hash = "";
	        hash = hash + initialState.hashCode() + action.hashCode();
	        return hash.hashCode();
		}
	}
}
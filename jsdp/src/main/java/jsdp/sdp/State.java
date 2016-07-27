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

/**
 * An abstraction for a state in which the system may be found in period {@code period}.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class State implements Serializable{
	
   private static final long serialVersionUID = 1L;
   
	protected int period;
	protected ArrayList<Action> feasibleActions;
	protected Action noAction;
	
	/**
	 * Creates a {@code State} associated with a given {@code period}.
	 * 
	 * @param period planning horizon period associated with the state.
	 */
	public State(int period){
		this.period = period;
	}
	
	/**
	 * Returns an {@code ArrayList<Action>} of feasible actions for this 
	 * {@code State}.
	 * 
	 * @return the {@code ArrayList<Action>} of feasible actions.
	 */
	public ArrayList<Action> getFeasibleActions() {
		return this.feasibleActions;
	}
	
	/**
	 * Returns the idempotent {@code Action} for this {@code State}.
	 * 
	 * @return the idempotent {@code Action} for this {@code State}.
	 */
	public Action getNoAction(){
		return noAction;
	}
	
	/**
	 * Returns the planning horizon period associated with the state.
	 * 
	 * @return the planning horizon period associated with the state.
	 */
	public int getPeriod(){
		return this.period;
	}
	
	public abstract boolean equals(Object state);
	public abstract int hashCode();
	
	/**
	 * This method constructs the set of feasible actions stored in 
	 * {@code ArrayList<Action> feasibleAction}.
	 */
	protected abstract void buildActionList();
}

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

/**
 * An abstraction to capture the stochastic decision process transition probabilities.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class TransitionProbability {
	
	/**
	 * This method returns the transition probability from {@code initialState} to {@code finalState} when
	 * {@code action} is selected.
	 * 
	 * @param initialState the initial state.
	 * @param action the action chosen.
	 * @param finalState the final state.
	 * @return the transition probability from {@code initialState} to {@code finalState} when
	 * {@code action} is selected.
	 */
	public abstract double getTransitionProbability(State initialState, Action action, State finalState);
	
	  /**
    * This method constructs an {@code ArrayList<State>} of states towards which the stochastic process may 
    * transition in period {@code t+1} if {@code action} is selected in {@code initialState} at period  
    * {@code t}; note that these states may not yet exist in the state space. This method is used by 
    * forward recursion procedures.
    * 
    * @param initialState the initial state of the stochastic process; note that we assume that 
    * {@code initialState} is associated with period {@code t}.
    * @param action the action selected at period {@code t}.
    * @return an {@code ArrayList<State>} of states towards which the stochastic process may 
    * transition in period {@code t+1}.
    */
   public abstract ArrayList<State> generateFinalStates(State initialState, Action action);
   
   /**
    * This method retrieves an {@code ArrayList<State>} of existing states towards which the stochastic process may 
    * transition in period {@code t+1} if {@code action} is selected in {@code initialState} at period  
    * {@code t}. This method is used by backward recursion procedures.
    * 
    * @param initialState the initial state of the stochastic process; note that we assume that 
    * {@code initialState} is associated with period {@code t}.
    * @param action the action selected at period {@code t}.
    * @return an {@code ArrayList<State>} of states towards which the stochastic process may 
    * transition in period {@code t+1}.
    */
   public abstract ArrayList<State> getFinalStates(State initialState, Action action);
}

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An abstraction for a recursive solution method for the stochastic dynamic program.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class Recursion {
	
   static final Logger logger = LogManager.getLogger(Recursion.class.getName());
   
	public enum OptimisationDirection {
		MIN,
		MAX
	};
	
	protected int horizonLength;
	protected StateSpace<?>[] stateSpace;
	protected TransitionProbability transitionProbability;
	protected ValueRepository valueRepository;
	final protected OptimisationDirection direction;
	
	/**
	 * Creates an instance of {@code Recursion} with the given optimisatio direction.
	 * 
	 * @param direction the direction of optimisation.
	 */
	protected Recursion(OptimisationDirection direction){
		this.direction = direction; 
	}
	
	/**
	 * Returns the expected value associated with {@code state}.
	 * 
	 * @param state the target state.
	 * @return the expected value associated with {@code state}.
	 */
	public double getExpectedValue(State state){
		return this.getValueRepository().getOptimalExpectedValue(state);
	}
	
	/**
	 * Returns the {@code StateSpace} for period {@code period}.
	 * 
	 * @param period the target period.
	 * @return the {@code StateSpace} for period {@code period}.
	 */
	public StateSpace<?> getStateSpace(int period){
		return this.stateSpace[period];
	}
	
	/**
	 * Returns the {@code StateSpace} array for the stochastic process planning horizon.
	 * 
	 * @return the {@code StateSpace} array for the stochastic process planning horizon.
	 */
	public StateSpace<?>[] getStateSpace(){
		return this.stateSpace;
	}
	
	/**
	 * Returns the {@code TransitionProbability} of the stochastic process.
	 * 
	 * @return the {@code TransitionProbability} of the stochastic process.
	 */
	public TransitionProbability getTransitionProbability(){
		return this.transitionProbability; 
	}
	
	/**
	 * Returns the {@code ValueRepository} of the stochastic process.
	 * 
	 * @return the {@code ValueRepository} of the stochastic process.
	 */
	public ValueRepository getValueRepository(){
		return this.valueRepository;
	}
}

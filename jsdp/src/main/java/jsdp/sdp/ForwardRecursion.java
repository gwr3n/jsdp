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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Implements a forward recursion algorithm to solve a stochastic dynamic program.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class ForwardRecursion extends Recursion{
	static final Logger logger = LogManager.getLogger(ForwardRecursion.class.getName());
	
	/**
	 * Creates an instance of {@code ForwardRecursion} with the given optimization direction.
	 * 
	 * @param direction the direction of optimization.
	 */
	public ForwardRecursion(OptimisationDirection direction){
		super(direction);
	}
	
	/**
	 * Runs the forward recursion algorithm for the given stochastic dynamic program.
	 */
	public double runForwardRecursion(State state){
		return this.valueRepository.optimalValueHashTable.computeIfAbsent(state, y -> {
			if(y.getPeriod() >= horizonLength - 1){
				BestActionRepository repository = new BestActionRepository();
				y.getFeasibleActions().stream().forEach(action -> {
					double normalisationFactor = transitionProbability.getFinalStates(y, action).stream()
							  .mapToDouble(finalState -> transitionProbability.getTransitionProbability(y, action, finalState))
							  .sum();
					double currentCost = this.transitionProbability.getFinalStates(y, action).stream()
							 				 .mapToDouble(c -> this.getValueRepository().getImmediateValue(y, action, c)*
							 						 	          this.getTransitionProbability().getTransitionProbability(y, action, c))
							 				 .sum()/normalisationFactor;
					repository.update(action, currentCost);
				});
				this.getValueRepository().setOptimalExpectedValue(y, repository.getBestValue());
				this.getValueRepository().setOptimalAction(y, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestValue());
				return repository.getBestValue();
			}else{
				BestActionRepository repository = new BestActionRepository();
				y.getFeasibleActions().stream().forEach(action -> {
					double normalisationFactor = transitionProbability.getFinalStates(y, action).stream()
							  .mapToDouble(finalState -> transitionProbability.getTransitionProbability(y, action, finalState))
							  .sum();
					double currentCost = this.transitionProbability.getFinalStates(y, action).stream()
							 				 .mapToDouble(c -> this.getValueRepository().getImmediateValue(y, action, c)*
							 						 	          this.getTransitionProbability().getTransitionProbability(y, action, c))
							 				 .sum() +
							 			 this.transitionProbability.getFinalStates(y, action).stream()
							 				 .mapToDouble(c -> runForwardRecursion(c)*
							 				 				       this.getTransitionProbability().getTransitionProbability(y, action, c))
							 				 .sum()/normalisationFactor;
					repository.update(action, currentCost);
				});
				this.getValueRepository().setOptimalExpectedValue(y, repository.getBestValue());
				this.getValueRepository().setOptimalAction(y, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestValue());
				return repository.getBestValue();
			}
		});
	}
}

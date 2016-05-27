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

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Implements a backward recursion algorithm to solve a stochastic dynamic program.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class BackwardRecursion extends Recursion{
	static final Logger logger = LogManager.getLogger(BackwardRecursion.class.getName());
	
	/**
	 * Creates an instance of {@code BackwardRecursion} with the given optimization direction.
	 * 
	 * @param direction the direction of optimization.
	 */
	public BackwardRecursion(OptimisationDirection direction){
		super(direction);
	}
	
	/**
	 * Generates the complete state space for the discrete time, discrete space, stochastic dynamic program.
	 */
	protected void generateStates(){
		CountDownLatch latch = new CountDownLatch(horizonLength + 1);
		for(int i = horizonLength; i >= 0; i--){
			Iterator<State> iterator = this.getStateSpace(i).iterator();
			Runnable r = () -> {
				while(iterator.hasNext()) 
					iterator.next();
				latch.countDown();
				};
			new Thread(r).start();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.getStateSpace(horizonLength).entrySet()
			.parallelStream()
			.forEach(entry -> this.getValueRepository().setOptimalExpectedValue(entry.getValue(), 0));
	}
	
	/**
	 * Runs the backward recursion algorithm for the given stochastic dynamic program.
	 */
	public void runBackwardRecursion(){
		logger.info("Generating states...");
		generateStates();
		for(int i = horizonLength - 1; i >= 0; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
	}
	
	/**
	 * Runs the backward recursion algorithm for the given stochastic dynamic program up to period {@code period}.
	 * This implementation of the backward recursion algorithm assumes that the idempotent action is selected in 
	 * period {@code period}.
	 */
	public void runBackwardRecursion(int period){
		logger.info("Generating states...");
		generateStates();
		for(int i = horizonLength - 1; i > period; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
		
		logger.info("Processing period["+period+"]...");
		this.getStateSpace(period).entrySet()
		.parallelStream()
		.forEach(entry -> {
			State state = entry.getValue();
			Action bestAction = state.getNoAction();
			double bestCost = this.getValueRepository().getExpectedValue(state, bestAction, this.getTransitionProbability());
			this.getValueRepository().setOptimalExpectedValue(state, bestCost);
			this.getValueRepository().setOptimalAction(state, bestAction);
		});
	}
	
	/**
	 * Backward recursion step; in order to run the recursion step for period {@code period} 
	 * the recursion step must have been already run for all subsequent periods.  
	 * 
	 * @param period the target period for the step.
	 */
	protected void recurse(int period){
		this.getStateSpace(period).entrySet()
			.parallelStream()
			//.stream()
			.forEach(entry -> {
				State state = entry.getValue();
				BestActionRepository repository = new BestActionRepository();
				state.getFeasibleActions().parallelStream().forEach(action -> {
					double currentCost = this.getValueRepository().getExpectedValue(state, action, this.getTransitionProbability());
					repository.update(action, currentCost);
				});
				this.getValueRepository().setOptimalExpectedValue(state, repository.getBestValue());
				this.getValueRepository().setOptimalAction(state, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestValue());
				//System.out.println(repository.getBestAction()+"\tCost: "+repository.getBestCost());
			});
	}
}

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
public abstract class BackwardRecursion {
	static final Logger logger = LogManager.getLogger(BackwardRecursion.class.getName());
	
	protected int horizonLength;
	protected StateSpace<?>[] stateSpace;
	protected TransitionProbability transitionProbability;
	protected CostRepository costRepository;
	
	/**
	 * Returns the expected cost associated with {@code state}.
	 * 
	 * @param state the target state.
	 * @return the expected cost associated with {@code state}.
	 */
	public double getExpectedCost(State state){
		return this.getCostRepository().getOptimalExpectedCost(state);
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
	 * Returns the {@code CostRepository} of the stochastic process.
	 * 
	 * @return the {@code CostRepository} of the stochastic process.
	 */
	public CostRepository getCostRepository(){
		return this.costRepository;
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
			double bestCost = this.getCostRepository().getExpectedCost(state, bestAction, this.getTransitionProbability());
			this.getCostRepository().setOptimalExpectedCost(state, bestCost);
			this.getCostRepository().setOptimalAction(state, bestAction);
		});
	}
	
	/**
	 * Backward recursion step. In order to run the recursion step for period {@code period} 
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
					double currentCost = this.getCostRepository().getExpectedCost(state, action, this.getTransitionProbability());
					repository.update(action, currentCost);
				});
				this.getCostRepository().setOptimalExpectedCost(state, repository.getBestCost());
				this.getCostRepository().setOptimalAction(state, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestCost());
				//System.out.println(repository.getBestAction()+"\tCost: "+repository.getBestCost());
			});
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
			.forEach(entry -> this.getCostRepository().setOptimalExpectedCost(entry.getValue(), 0));
	}
	
	/**
	 * Stores the best action and its cost.
	 * 
	 * @author Roberto Rossi
	 *
	 */
	class BestActionRepository {
		Action bestAction = null;
		double bestCost = Double.MAX_VALUE;
		
		/**
		 * Compares {@code currentAction} and {@code currentCost} to the best action currently stored and updates
		 * the value of the best action and of its cost if necessary.
		 * 
		 * @param currentAction the action.
		 * @param currentCost the action expected total cost.
		 */
		public synchronized void update(Action currentAction, double currentCost){
			if(currentCost < bestCost){
				bestCost = currentCost;
				bestAction = currentAction;
			}
		}
		
		/**
		 * Returns the best action stored.
		 * 
		 * @return the best action stored.
		 */
		public Action getBestAction(){
			return this.bestAction;
		}
		
		/**
		 * Returns the cost associated with the best action stored.
		 * 
		 * @return the cost associated with the best action stored.
		 */
		public double getBestCost(){
			return this.bestCost;
		}
	}
}

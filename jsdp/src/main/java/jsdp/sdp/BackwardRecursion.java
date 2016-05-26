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

public abstract class BackwardRecursion {
	static final Logger logger = LogManager.getLogger(BackwardRecursion.class.getName());
	
	protected int horizonLength;
	protected StateSpace<?>[] stateSpace;
	protected TransitionProbability transitionProbability;
	protected CostRepository costRepository;
	
	public double getExpectedCost(State state){
		return this.getCostRepository().getOptimalExpectedCost(state);
	}
	
	public StateSpace<?> getStateSpace(int period){
		return this.stateSpace[period];
	}
	
	public StateSpace<?>[] getStateSpace(){
		return this.stateSpace;
	}
	
	public TransitionProbability getTransitionProbability(){
		return this.transitionProbability; 
	}
	
	public CostRepository getCostRepository(){
		return this.costRepository;
	}
	
	public void runBackwardRecursion(){
		logger.info("Generating states...");
		generateStates();
		for(int i = horizonLength - 1; i >= 0; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
	}
	
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
	
	class BestActionRepository {
		Action bestAction = null;
		double bestCost = Double.MAX_VALUE;
		
		public synchronized void update(Action currentAction, double currentCost){
			if(currentCost < bestCost){
				bestCost = currentCost;
				bestAction = currentAction;
			}
		}
		
		public Action getBestAction(){
			return this.bestAction;
		}
		
		public double getBestCost(){
			return this.bestCost;
		}
	}
	
	protected void recurse(int period){
		this.getStateSpace(period).entrySet()
			.parallelStream()
			//.stream()
			.forEach(entry -> {
				State state = entry.getValue();
				BestActionRepository repository = new BestActionRepository();
				state.getPermissibleActionsParallelStream().forEach(action -> {
					double currentCost = this.getCostRepository().getExpectedCost(state, action, this.getTransitionProbability());
					repository.update(action, currentCost);
				});
				this.getCostRepository().setOptimalExpectedCost(state, repository.getBestCost());
				this.getCostRepository().setOptimalAction(state, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestCost());
				//System.out.println(repository.getBestAction()+"\tCost: "+repository.getBestCost());
			});
	}
	
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
}

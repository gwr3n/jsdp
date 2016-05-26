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

public abstract class ForwardRecursion {
	static final Logger logger = LogManager.getLogger(ForwardRecursion.class.getName());
	
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
	
	public double runForwardRecursion(State state){
		return this.costRepository.optimalCostHashTable.computeIfAbsent(state, y -> {
			if(y.getPeriod() >= horizonLength - 1){
				BestActionRepository repository = new BestActionRepository();
				y.getPermissibleActionsStream().forEach(action -> {
					double normalisationFactor = transitionProbability.getFinalStatesStream(y, action)
							  .mapToDouble(finalState -> transitionProbability.getTransitionProbability(y, action, finalState))
							  .sum();
					double currentCost = this.transitionProbability.getFinalStatesStream(y, action)
							 				 .mapToDouble(c -> this.getCostRepository().getImmediateCost(y, action, c)*
							 						 	       this.getTransitionProbability().getTransitionProbability(y, action, c))
							 				 .sum()/normalisationFactor;
					repository.update(action, currentCost);
				});
				this.getCostRepository().setOptimalExpectedCost(y, repository.getBestCost());
				this.getCostRepository().setOptimalAction(y, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestCost());
				return repository.getBestCost();
			}else{
				BestActionRepository repository = new BestActionRepository();
				y.getPermissibleActionsStream().forEach(action -> {
					double normalisationFactor = transitionProbability.getFinalStatesStream(y, action)
							  .mapToDouble(finalState -> transitionProbability.getTransitionProbability(y, action, finalState))
							  .sum();
					double currentCost = this.transitionProbability.getFinalStatesStream(y, action)
							 				 .mapToDouble(c -> this.getCostRepository().getImmediateCost(y, action, c)*
							 						 	       this.getTransitionProbability().getTransitionProbability(y, action, c))
							 				 .sum() +
							 			 this.transitionProbability.getFinalStatesStream(y, action)
							 				 .mapToDouble(c -> runForwardRecursion(c)*
							 				 				   this.getTransitionProbability().getTransitionProbability(y, action, c))
							 				 .sum()/normalisationFactor;
					repository.update(action, currentCost);
				});
				this.getCostRepository().setOptimalExpectedCost(y, repository.getBestCost());
				this.getCostRepository().setOptimalAction(y, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestCost());
				return repository.getBestCost();
			}
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
}

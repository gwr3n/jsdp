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

package jsdp.app.lotsizing;

import java.util.Iterator;

import jsdp.sdp.Action;
import jsdp.sdp.BackwardRecursion;
import jsdp.sdp.State;

import umontreal.ssj.probdist.Distribution;

/**
 * A concrete implementation of a backward recursion procedure to compute (s,S) policy parameters.
 * 
 * @author Roberto Rossi
 *
 */
public class sS_BackwardRecursion extends BackwardRecursion{
	
	Distribution[] demand;
	
	/**
	 * Creates an instance of the problem and initialises state space, transition probability and value repository.
	 * 
	 * @param demand the distribution of random demand in each period, an array of {@code Distribution}.
	 * @param fixedOrderingCost the fixed ordering cost.
	 * @param proportionalOrderingCost the proportional (per unit) ordering cost.
	 * @param holdingCost the proportional (per unit) holding cost; this is paid for each item brought from one period to the next.
	 * @param penaltyCost the proportional (per unit) penalty cost; this is paid for each item short at the end of each period.
	 * @param samplingScheme the sampling scheme used in the generation of the state space.
	 * @param maxSampleSize the maximum sample size.
	 */
	public sS_BackwardRecursion(Distribution[] demand,
	                            double minDemand,
	                            double maxDemand,
								double fixedOrderingCost, 
								double proportionalOrderingCost, 
								double holdingCost,
								double penaltyCost,
								sS_StateSpaceSampleIterator.SamplingScheme samplingScheme,
								int maxSampleSize){
		super(OptimisationDirection.MIN);
		this.demand = demand;
		this.horizonLength = demand.length;
		
		this.stateSpace = new sS_StateSpace[this.horizonLength+1];
		for(int i = 0; i < this.horizonLength + 1; i++) 
			this.stateSpace[i] = new sS_StateSpace(i, samplingScheme, maxSampleSize);
		this.transitionProbability = new sS_TransitionProbability(demand,minDemand,maxDemand,(sS_StateSpace[])this.getStateSpace(),sS_State.getStepSize());
		this.valueRepository = new sS_CostRepository(fixedOrderingCost, proportionalOrderingCost, holdingCost, penaltyCost);
	}
	
	@Override
	public sS_TransitionProbability getTransitionProbability(){
		return (sS_TransitionProbability) this.transitionProbability; 
	}
	
	@Override
	public sS_CostRepository getValueRepository(){
		return (sS_CostRepository) this.valueRepository;
	}
	
	public double[][] getOptimalPolicy(double initialInventory){
		double[][] optimalPolicy = new double[2][];
		double[] S = new double[demand.length];
		double[] s = new double[demand.length];
		for(int i = 0; i < demand.length; i++){
			if(i == 0) {
				sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(0, sS_State.inventoryToState(initialInventory));
				s[i] = sS_State.stateToInventory(this.find_s(i).getInitialIntState());
				S[i] = sS_Action.actionToOrderQuantity(this.getOptimalAction(stateDescriptor).getIntAction())+initialInventory;
			}
			else{
				s[i] = sS_State.stateToInventory(this.find_s(i).getInitialIntState());
				S[i] = sS_State.stateToInventory(this.find_S(i).getInitialIntState());
			}
		}
		optimalPolicy[0] = s;
		optimalPolicy[1] = S;
		return optimalPolicy;
	}
	
	public double getExpectedCost(double initialInventory){
		sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(0, sS_State.inventoryToState(initialInventory));
		return getExpectedCost(stateDescriptor);
	}
	
	public double getExpectedCost(sS_StateDescriptor stateDescriptor){
		State state = ((sS_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
		return getExpectedValue(state);
	}
	
	public sS_Action getOptimalAction(sS_StateDescriptor stateDescriptor){
		State state = ((sS_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
		return (sS_Action) this.getValueRepository().getOptimalAction(state);
	}

	public sS_State find_S(int period){
		sS_State s = this.find_s(period);
		int i = ((sS_Action)this.getValueRepository().getOptimalAction(s)).getIntAction()+s.getInitialIntState();
		sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period, i);
		sS_State state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
		return state;
	}
	
	public sS_State find_s(int period){
		Iterator<State> iterator = this.getStateSpace()[period].iterator();
		//Iterator<State> iterator = new sS_StateSpaceIterator((sS_StateSpace)this.getStateSpace()[period]);
		sS_State state = null;
		do{
			state = (sS_State) iterator.next();
			Action action = this.getValueRepository().getOptimalAction(state);
			if(action == null)
			   continue;
			if(((sS_Action)action).getIntAction() > 0){
				return state;
			}
		}while(iterator.hasNext());
		return state;
	}
}

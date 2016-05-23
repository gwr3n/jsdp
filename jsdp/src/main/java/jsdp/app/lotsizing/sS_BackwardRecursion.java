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

import jsdp.sdp.Action;
import jsdp.sdp.BackwardRecursion;
import umontreal.ssj.probdist.Distribution;

public class sS_BackwardRecursion extends BackwardRecursion{
	
	double fixedOrderingCost; 
	double proportionalOrderingCost; 
	double holdingCost;
	double penaltyCost;
	Distribution[] demand;
	
	public sS_TransitionProbability getTransitionProbability(){
		return (sS_TransitionProbability) this.transitionProbability; 
	}
	
	public sS_CostRepository getCostRepository(){
		return (sS_CostRepository) this.costRepository;
	}
	
	public sS_BackwardRecursion(Distribution[] demand,
								double fixedOrderingCost, 
								double proportionalOrderingCost, 
								double holdingCost,
								double penaltyCost){
		this.demand = demand;
		
		this.horizonLength = demand.length;
		
		this.fixedOrderingCost = fixedOrderingCost;
		this.proportionalOrderingCost = proportionalOrderingCost;
		this.holdingCost = holdingCost;
		this.penaltyCost = penaltyCost;
		
		this.stateSpace = new sS_StateSpace[this.horizonLength+1];
		for(int i = 0; i < this.horizonLength + 1; i++) 
			this.stateSpace[i] = new sS_StateSpace(i);
		this.transitionProbability = new sS_TransitionProbability(demand,(sS_StateSpace[])this.getStateSpace(),sS_State.factor);
		this.costRepository = new sS_CostRepository(fixedOrderingCost, proportionalOrderingCost, holdingCost, penaltyCost);
	}

	public sS_State find_S(int period){
		sS_State s = this.find_s(period);
		int i = ((sS_Action)this.getCostRepository().getOptimalAction(s)).getOrderQuantity()+s.getInitialInventory();
		sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period, i);
		sS_State state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
		return state;
	}
	
	public sS_State find_s(int period){
		sS_StateSpaceIterator iterator = (sS_StateSpaceIterator)this.getStateSpace()[period].iterator();
		sS_State state = null;
		do{
			state = (sS_State) iterator.next();
			Action action = this.getCostRepository().getOptimalAction(state);
			if(((sS_Action)action).getOrderQuantity() > 0){
				return state;
			}
		}while(iterator.hasNext());
		return state;
	}
}

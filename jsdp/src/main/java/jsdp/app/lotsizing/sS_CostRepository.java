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

import java.util.Enumeration;

import jsdp.sdp.Action;
import jsdp.sdp.CostRepository;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;

public class sS_CostRepository extends CostRepository {
	double fixedOrderingCost;
	double proportionalOrderingCost;
	double holdingCost;
	double penaltyCost;
	
	public sS_CostRepository(double fixedOrderingCost, double proportionalOrderingCost, double holdingCost, double penaltyCost){
		this.fixedOrderingCost = fixedOrderingCost;
		this.proportionalOrderingCost = proportionalOrderingCost;
		this.holdingCost = holdingCost;
		this.penaltyCost = penaltyCost;
	}
	
	@Override
	protected double getImmediateCost(State initialState, Action action, State finalState) {
		sS_Action a = (sS_Action)action;
		sS_State fs = (sS_State)finalState;
		double totalCost = a.getOrderQuantity() > 0 ? (fixedOrderingCost + a.getOrderQuantity()*sS_State.factor*proportionalOrderingCost) : 0;
		totalCost += Math.max(fs.getInitialInventory()*sS_State.factor,0)*holdingCost+Math.max(-fs.getInitialInventory()*sS_State.factor,0)*penaltyCost;
		return totalCost;
	}

	@Override
	public double getExpectedCost(State initialState, Action action, TransitionProbability transitionProbability) {
		StateAction key = new StateAction(initialState, action);
		if(this.costHashTable.containsKey(key)) 
			return this.costHashTable.get(key).doubleValue();
		
		Enumeration<State> e = transitionProbability.getFinalStates(initialState, action);
		double expectedTotalCost = 0;
		while(e.hasMoreElements()){
			sS_State finalState = (sS_State)e.nextElement();
			expectedTotalCost += (this.getImmediateCost(initialState, action, finalState)+this.getOptimalExpectedCost(finalState))
					*transitionProbability.getTransitionProbability(initialState, action, finalState);
		}
		this.costHashTable.put(key, new Double(expectedTotalCost));
		return expectedTotalCost;
	}

}

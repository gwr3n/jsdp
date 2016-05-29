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
import jsdp.sdp.ValueRepository;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;

public class sS_CostRepository extends ValueRepository {
	double fixedOrderingCost;
	double proportionalOrderingCost;
	double holdingCost;
	double penaltyCost;
	
	public sS_CostRepository(double fixedOrderingCost, double proportionalOrderingCost, double holdingCost, double penaltyCost){
		this.fixedOrderingCost = fixedOrderingCost;
		this.proportionalOrderingCost = proportionalOrderingCost;
		this.holdingCost = holdingCost;
		this.penaltyCost = penaltyCost;
		
		this.immediateValueFunction = (initialState, action, finalState) -> {
	      sS_Action a = (sS_Action)action;
	      sS_State fs = (sS_State)finalState;
	      double totalCost = a.getIntAction() > 0 ? (fixedOrderingCost + sS_Action.actionToOrderQuantity(a.getIntAction())*proportionalOrderingCost) : 0;
	      totalCost +=   Math.max(sS_State.stateToInventory(fs.getInitialIntState()),0)*holdingCost+
	                     Math.max(-sS_State.stateToInventory(fs.getInitialIntState()),0)*penaltyCost;
	      return totalCost;
	   };
	}

	@Override
	public double getExpectedValue(State initialState, Action action, TransitionProbability transitionProbability) {
		StateAction key = new StateAction(initialState, action);
		return this.valueHashTable.computeIfAbsent(key, y -> {
			double normalisationFactor = transitionProbability.getFinalStates(initialState, action).parallelStream()
					  .mapToDouble(finalState -> transitionProbability.getTransitionProbability(initialState, action, finalState))
					  .sum();
			double expectedTotalCost = transitionProbability.getFinalStates(initialState, action).parallelStream()
					  .mapToDouble(finalState -> 
					  (this.immediateValueFunction.apply(initialState, action, finalState)+this.getOptimalExpectedValue(finalState))*
					  transitionProbability.getTransitionProbability(initialState, action, finalState)
			).sum()/normalisationFactor;
			return expectedTotalCost;
		});
	}

}

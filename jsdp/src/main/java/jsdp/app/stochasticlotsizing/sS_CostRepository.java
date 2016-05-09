package jsdp.app.stochasticlotsizing;

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
		double totalCost = a.getOrderQuantity() > 0 ? (fixedOrderingCost + a.getOrderQuantity()/sS_State.factor*proportionalOrderingCost) : 0;
		totalCost += Math.max(fs.getInitialInventory()/sS_State.factor,0)*holdingCost+Math.max(-fs.getInitialInventory()/sS_State.factor,0)*penaltyCost;
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

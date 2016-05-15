package jsdp.app.lotsizing;

import java.util.Enumeration;

import org.apache.logging.log4j.Logger;

import jsdp.sdp.Action;

import org.apache.logging.log4j.LogManager;

public class sS_SequentialBackwardRecursionPoisson extends sS_BackwardRecursionPoisson {
	static final Logger logger = LogManager.getLogger(sS_BackwardRecursionPoisson.class.getName());
	
	public sS_SequentialBackwardRecursionPoisson(double[] demand,
			double fixedOrderingCost, 
			double proportionalOrderingCost, 
			double holdingCost,
			double penaltyCost){
		super(demand,fixedOrderingCost,proportionalOrderingCost,holdingCost,penaltyCost);
	}
	
	@Override
	protected void recurse(int period){
		for(int i = sS_State.maxInventory; i >= sS_State.minInventory; i--){
			sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period, i);
			sS_State state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
			Action bestAction = null;
			double bestCost = Double.MAX_VALUE;
			Enumeration<Action> actions = state.getPermissibleActions();
			while(actions.hasMoreElements()){
				Action currentAction = actions.nextElement();
				double currentCost = this.getCostRepository().getExpectedCost(state, currentAction, this.getTransitionProbability());
				if(currentCost < bestCost){
					bestCost = currentCost;
					bestAction = currentAction;
				}
			}
			this.getCostRepository().setOptimalExpectedCost(state, bestCost);
			this.getCostRepository().setOptimalAction(state, bestAction);
			logger.trace("Period["+period+"]\tInventory: "+i+"\tOrder: "+((sS_Action)bestAction).orderQty+"\tCost: "+bestCost);
			//System.out.println("Period["+period+"]\tInventory: "+i+"\tOrder: "+((sS_Action)bestAction).orderQty+"\tCost: "+bestCost);
			
			if(((sS_Action)bestAction).getOrderQuantity() > 0){
				double initialOrderQty = ((sS_Action)bestAction).getOrderQuantity();
				while(--i >= sS_State.minInventory){
					stateDescriptor = new sS_StateDescriptor(period, i);
					state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
					double orderingCostIncrement = (((sS_Action)bestAction).orderQty+1-initialOrderQty)/sS_State.factor*this.proportionalOrderingCost;
					this.getCostRepository().setOptimalExpectedCost(state, bestCost+orderingCostIncrement);
					bestAction = new sS_Action(state, ((sS_Action)bestAction).orderQty+1); 
					this.getCostRepository().setOptimalAction(state, bestAction);
					logger.trace("Period["+period+"]\tInventory: "+i+"\tOrder: "+((sS_Action)bestAction).orderQty+"\tCost: "+bestCost);
					//System.out.println("Period["+period+"]\tInventory: "+i+"\tOrder: "+((sS_Action)bestAction).orderQty+"\tCost: "+bestCost);
				}
			}
		}
	}
}

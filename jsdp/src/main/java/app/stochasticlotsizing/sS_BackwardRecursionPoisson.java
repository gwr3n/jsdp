package app.stochasticlotsizing;

import sdp.Action;
import sdp.BackwardRecursion;

public class sS_BackwardRecursionPoisson extends BackwardRecursion{
	
	double fixedOrderingCost; 
	double proportionalOrderingCost; 
	double holdingCost;
	double penaltyCost;
	double[] demand;
	
	public sS_TransitionProbabilityPoisson getTransitionProbability(){
		return (sS_TransitionProbabilityPoisson) this.transitionProbability; 
	}
	
	public sS_CostRepository getCostRepository(){
		return (sS_CostRepository) this.costRepository;
	}
	
	public sS_BackwardRecursionPoisson(double[] demand,
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
		this.transitionProbability = new sS_TransitionProbabilityPoisson(demand,(sS_StateSpace[])this.getStateSpace());
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

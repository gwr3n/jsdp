package sdp;

import java.util.Hashtable;

public abstract class CostRepository {	
	protected Hashtable<StateAction,Double> costHashTable = new Hashtable<StateAction,Double>();
	protected Hashtable<State,Double> optimalCostHashTable = new Hashtable<State,Double>();
	protected Hashtable<State,Action> optimalActionHashTable = new Hashtable<State,Action>();
	
	protected abstract double getImmediateCost(State initialState, Action action, State finalState);
	public abstract double getExpectedCost(State initialState, Action action, TransitionProbability transitionProbability);
	
	public void setOptimalExpectedCost(State state, double expectedCost){
		this.optimalCostHashTable.put(state, new Double(expectedCost));
	}
	
	public double getOptimalExpectedCost(State state){
		return this.optimalCostHashTable.get(state).doubleValue();
	}
	
	public void setOptimalAction(State state, Action action){
		this.optimalActionHashTable.put(state, action);
	}
	
	public Action getOptimalAction(State state){
		return this.optimalActionHashTable.get(state);
	}
	
	protected class StateAction{
		State initialState;
		Action action;
		
		public StateAction(State initialState, Action action){
			this.initialState = initialState;
			this.action = action; 
		}
		
		public boolean equals(StateAction stateAction){
			return this.initialState.equals(stateAction.initialState) && this.action.equals(stateAction.action);
		}
		
		public int hashCode(){
			String hash = "";
	        hash = hash + initialState.hashCode() + action.hashCode();
	        return hash.hashCode();
		}
	}
}
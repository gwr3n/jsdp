package jsdp.app.stochasticlotsizing;

import jsdp.sdp.State;
import jsdp.sdp.StateSpaceIterator;

public class sS_StateSpaceIterator extends StateSpaceIterator {
	
	sS_StateSpace stateSpace;
	sS_StateDescriptor currentStateDescriptor;
	
	public sS_StateSpaceIterator(int period, sS_StateSpace stateSpace){
		this.stateSpace = stateSpace;
		currentStateDescriptor = new sS_StateDescriptor(period, sS_State.maxInventory);
	}
    
    public boolean hasNext() {
    	if(currentStateDescriptor.getInitialInventory() <= sS_State.maxInventory && 
    	   currentStateDescriptor.getInitialInventory() >= sS_State.minInventory)
    		return true;
    	else
    		return false;
	}

	public State next() {
		if(currentStateDescriptor.getInitialInventory() <= sS_State.maxInventory && 
		   currentStateDescriptor.getInitialInventory() >= sS_State.minInventory){
			State state = stateSpace.getState(currentStateDescriptor);
			currentStateDescriptor = new sS_StateDescriptor(currentStateDescriptor.getPeriod(), 
															currentStateDescriptor.getInitialInventory() - 1);
			return state;
		}else{
			return null;
		}
	}
}

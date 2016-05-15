package jsdp.app.lotsizing;

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class sS_Action extends Action {
	
	int orderQty;
	
	public sS_Action(State state, int orderQty){
		this.state = state;
		this.orderQty = orderQty;
	}
	
	public int getOrderQuantity(){
		return this.orderQty;
	}
	
	@Override
	public boolean equals(Object action){
		if(action instanceof sS_Action)
			return this.orderQty == ((sS_Action)action).orderQty;
		else
			return false;
	}
	
	@Override
	public int hashCode(){
		String hash = "";
        hash = (hash + orderQty);
        return hash.hashCode();
	}
	
	@Override
	public String toString(){
		return state+"\tAction: "+this.orderQty;
	}
}

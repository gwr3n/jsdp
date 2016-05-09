package jsdp.app.stochasticlotsizing;

import java.util.ArrayList;

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class sS_State extends State {
	
	private int initialInventory;
	
	//Factor must be 1 for discrete distributions
	public static double factor = 1;
	public static int minInventory = -100;
	public static int maxInventory = 150;
	
	public sS_State(sS_StateDescriptor descriptor){
		this.period = descriptor.getPeriod();
		this.initialInventory = descriptor.getInitialInventory();
		this.buildActionList();
	}
	
	public int getInitialInventory(){
		return this.initialInventory;
	}
	
	@Override
	protected void buildActionList(){
		this.noAction = new sS_Action(this, 0);
		this.permissibleActions = new ArrayList<Action>();
		for(int i = this.initialInventory; i <= maxInventory; i++){
			permissibleActions.add(new sS_Action(this, i - this.initialInventory));
		}
	}
	
	@Override
	public boolean equals(Object state){
		if(state instanceof sS_State)
			return this.period == ((sS_State)state).period && this.initialInventory == ((sS_State)state).initialInventory;
		else return false;
	}
	
	@Override
	public int hashCode(){
		String hash = "";
        hash = (hash + period) + initialInventory;
        return hash.hashCode();
	}

	@Override
	public int compareTo(State state) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String toString(){
		return "Period: "+this.period+"\tInventory:"+this.initialInventory;
	}
}

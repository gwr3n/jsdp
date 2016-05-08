package app.stochasticlotsizing;

import java.util.Iterator;

import sdp.State;
import sdp.StateSpace;

public class sS_StateSpace extends StateSpace<sS_StateDescriptor>{
	
	public sS_StateSpace(int period){
		super(period);
	}
	
	public State getState(sS_StateDescriptor descriptor){
		State value = states.get(descriptor);
		if(value == null){
			State state = new sS_State(descriptor);
			this.states.put(descriptor, state);
			return state;
		}else
			return (sS_State)value;
	}

	public Iterator<State> iterator() {
		return new sS_StateSpaceIterator(this.getPeriod(), this);
	}
}

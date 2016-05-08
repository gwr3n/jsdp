package sdp;

import java.util.Hashtable;

public abstract class StateSpace<SD> implements Iterable<State>{
	
	protected int period;
	protected Hashtable<SD,State> states = new Hashtable<SD,State>();
	
	public StateSpace(int period){
		this.period = period;
	}
	
	protected abstract State getState(SD descriptor);
	
	public int getPeriod(){
		return period;
	}
}

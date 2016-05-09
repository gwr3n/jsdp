package sdp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;

public abstract class State implements Comparable<State>{
	
	protected int period;
	protected ArrayList<Action> permissibleActions;
	protected Action noAction;
	
	public Enumeration<Action> getPermissibleActions() {
		Enumeration<Action> e = Collections.enumeration(this.permissibleActions);
		return e;
	}
	
	public Stream<Action> getPermissibleActionsStream(){
		return this.permissibleActions.parallelStream();
	}
	
	public Action getNoAction(){
		return noAction;
	}
	
	public int getPeriod(){
		return this.period;
	}
	
	public abstract int compareTo(State state);
	public abstract boolean equals(Object state);
	public abstract int hashCode();
	protected abstract void buildActionList();
}

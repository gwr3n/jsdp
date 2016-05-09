package jsdp.sdp;

public abstract class Action {
	
	protected State state;

	public State getState() {
		return this.state;
	}
	
	public abstract boolean equals(Object action);
	public abstract int hashCode();
}

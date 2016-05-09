package sdp;

public abstract class StateDescriptor{
	
	protected int period;
	
	public abstract boolean equals(Object descriptor);
	public abstract int hashCode();
	public int getPeriod(){
		return period;
	}
}
package jsdp.sdp;

import java.util.Iterator;

public abstract class StateSpaceIterator implements Iterator<State> {
	
	public abstract boolean hasNext();
	public abstract State next();

}

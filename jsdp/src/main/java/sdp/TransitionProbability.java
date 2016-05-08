package sdp;

import java.util.Enumeration;

public abstract class TransitionProbability {
	public abstract double getTransitionProbability(State initialState, Action action, State finalState);
	public abstract Enumeration<State> getFinalStates(State initialState, Action action);
}

package jsdp.app.stochasticlotsizing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;
import umontreal.iro.lecuyer.probdist.PoissonDist;

public class sS_TransitionProbabilityPoisson extends TransitionProbability {
	double[] meanDemand;
	double[] stdDemand;
	sS_StateSpace[] stateSpace;
	
	public sS_TransitionProbabilityPoisson(double[] meanDemand, sS_StateSpace[] stateSpace){
		this.meanDemand = meanDemand;
		this.stateSpace = stateSpace;
	}
	
	@Override
	public double getTransitionProbability(State initialState, Action action, State finalState) {
		double realizedDemand = ((sS_State)initialState).getInitialInventory()+((sS_Action)action).getOrderQuantity()-((sS_State)finalState).getInitialInventory();
		int period = ((sS_State)initialState).getPeriod();
		PoissonDist distribution = new PoissonDist(meanDemand[period]);
		//return (distribution.cdf(realizedDemand/sS_State.factor+0.5/sS_State.factor)-distribution.cdf(realizedDemand/sS_State.factor-0.5/sS_State.factor))/computeNormalization(initialState, action);
		return (distribution.prob((int)Math.round(realizedDemand/sS_State.factor))/computeNormalization(initialState, action));
	}

	@Override
	public Enumeration<State> getFinalStates(State initialState, Action action) {
		ArrayList<State> states = new ArrayList<State>();
		int openingInventory = ((sS_State) initialState).getInitialInventory() + ((sS_Action) action).getOrderQuantity();
		for(int i = openingInventory; i >= sS_State.minInventory; i--){
			sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(((sS_State) initialState).getPeriod()+1,i);
			states.add(this.stateSpace[((sS_State) initialState).getPeriod()+1].getState(stateDescriptor));
		}
		Enumeration<State> e = Collections.enumeration(states);
		return e;
	}
	
	public double computeNormalization(State initialState, Action action){
		int period = ((sS_State)initialState).getPeriod();
		PoissonDist distribution = new PoissonDist(meanDemand[period]);
		int maxDemand = ((sS_State) initialState).getInitialInventory() + ((sS_Action) action).getOrderQuantity() - sS_State.minInventory;
		return distribution.cdf((int)Math.round(maxDemand/sS_State.factor));
	}
}

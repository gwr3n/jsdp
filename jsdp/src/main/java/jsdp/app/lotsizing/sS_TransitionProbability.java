/**
 * jsdp: A Java Stochastic Dynamic Programming Library
 * 
 * MIT License
 * 
 * Copyright (c) 2016 Roberto Rossi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jsdp.app.lotsizing;

import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;
import jsdp.utilities.DiscreteDistributionFactory;
import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.Distribution;

public class sS_TransitionProbability extends TransitionProbability {
	DiscreteDistribution[] demand;
	sS_StateSpace[] stateSpace;
	
	public sS_TransitionProbability(Distribution[] demand, sS_StateSpace[] stateSpace, double stepSize){
		this.demand = IntStream.iterate(0, i -> i + 1)
							   .limit(demand.length)
							   .mapToObj(i -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
									   							demand[i],
									   							0,
									   							sS_State.getMaxInventory()-sS_State.getMinInventory(),
									   							stepSize))
							   .toArray(DiscreteDistribution[]::new);
		this.stateSpace = stateSpace;
	}
	
	@Override
	public double getTransitionProbability(State initialState, Action action, State finalState) {
		double realizedDemand = ((sS_State)initialState).getInitialIntState() +
								((sS_Action)action).getIntAction() -
								((sS_State)finalState).getInitialIntState();
		int period = ((sS_State)initialState).getPeriod();
		return this.demand[period].prob((int)Math.round(realizedDemand));
	}

	/*@Override
	public Stream<State> getFinalStates(State initialState, Action action) {
		int period = ((sS_State) initialState).getPeriod();
		ArrayList<State> states = new ArrayList<State>();
		int initialIntState = 	((sS_State) initialState).getInitialIntState() + ((sS_Action) action).getIntAction();
		for(int i = initialIntState; i >= sS_State.minIntState; i--){
			sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period+1,i);
			states.add(this.stateSpace[period+1].getState(stateDescriptor));
		}
		return states.parallelStream();
	}*/
	
	@Override
	public Stream<State> getFinalStates(State initialState, Action action) {
		int period = ((sS_State) initialState).getPeriod();
		ArrayList<State> states = new ArrayList<State>();
		int initialIntState = ((sS_State) initialState).getInitialIntState() + ((sS_Action) action).getIntAction();	
		for(int i = 0; this.demand[period].cdf(sS_State.stateToInventory(i-1)) < 1 && initialIntState-i >= sS_State.getMinIntState(); i++){
			sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period+1,initialIntState-i);
			states.add(this.stateSpace[period+1].getState(stateDescriptor));
		}
		return states.parallelStream();
	}
}


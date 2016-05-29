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

package jsdp.sdp.impl;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jsdp.sdp.Action;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;
import jsdp.sdp.StateTransitionFunction;
import jsdp.sdp.TransitionProbability;
import jsdp.utilities.DiscreteDistributionFactory;

import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.Distribution;

public class TransitionProbabilityImpl extends TransitionProbability {
   DiscreteDistribution[] demand;
   StateSpaceImpl[] stateSpace;

   public TransitionProbabilityImpl(Distribution[] demand,
                                    ImmediateValueFunction<State, Action, Double> randomOutcomeFunction,
                                    StateSpaceImpl[] stateSpace, 
                                    double stepSize){
      this.demand = IntStream.iterate(0, i -> i + 1)
                             .limit(demand.length)
                             .mapToObj(i -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                               demand[i], 0, StateImpl.getMaxState()-StateImpl.getMinState(), stepSize))
                             .toArray(DiscreteDistribution[]::new);
      this.randomOutcomeFunction = randomOutcomeFunction;
      this.stateSpace = stateSpace;
   }

   protected ImmediateValueFunction<State, Action, Double> randomOutcomeFunction;
   
   @Override
   public double getTransitionProbability(State initialState, Action action, State finalState) {
      int randomOutcome = StateImpl.stateToIntState(this.randomOutcomeFunction.apply(initialState, action, finalState));
      int period = ((StateImpl)initialState).getPeriod();
      return this.demand[period].prob(randomOutcome);
   }
   
   @Override  
   public ArrayList<State> generateFinalStates(State initialState, Action action) {
      throw new NullPointerException("Method not implemented");
   }
   
   @Override
   public ArrayList<State> getFinalStates(State initialState, Action action) {
      ArrayList<State> states = new ArrayList<State>();
      this.stateSpace[initialState.getPeriod()+1].forEach(entry -> states.add(entry));
      return states.parallelStream().filter(s -> this.getTransitionProbability(initialState, action, s) > 0)
                                    .collect(Collectors.toCollection(ArrayList<State>::new));
   }
}


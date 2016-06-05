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

package jsdp.sdp.impl.univariate;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jsdp.sdp.Action;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;
import jsdp.utilities.probdist.DiscreteDistributionFactory;
import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.Distribution;

public class TransitionProbabilityImpl extends TransitionProbability {
   DiscreteDistribution[] stateIndependentDistributions;
   DiscreteDistribution[][] stateDependentDistributions;
   DiscreteDistribution[][][] stateActionDependentDistributions;
   StateSpaceImpl[] stateSpace;

   private enum DistributionType{
      STATE_ACTION_DEPENDENT,
      STATE_DEPENDENT,
      STATE_INDEPENDENT
   };
   
   private DistributionType distributionType;
   
   public TransitionProbabilityImpl(Distribution[] distributions,
                                    RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction,
                                    StateSpaceImpl[] stateSpace, 
                                    double stepSize){
      this.distributionType = DistributionType.STATE_INDEPENDENT;
      this.stateIndependentDistributions = IntStream.iterate(0, i -> i + 1)
                             .limit(distributions.length)
                             .mapToObj(i -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                               distributions[i], 0, StateImpl.getMaxState()-StateImpl.getMinState(), stepSize))
                             .toArray(DiscreteDistribution[]::new);
      this.randomOutcomeFunction = randomOutcomeFunction;
      this.stateSpace = stateSpace;
   }
   
   public TransitionProbabilityImpl(Distribution[][][] distributions,
                                    RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction,
                                    StateSpaceImpl[] stateSpace, 
                                    double stepSize){
      this.distributionType = DistributionType.STATE_ACTION_DEPENDENT;
      this.stateActionDependentDistributions = new DiscreteDistribution[distributions.length][distributions[0].length][distributions[0][0].length];
      for(int t = 0; t < distributions.length; t++){
         for(int a = 0; a < distributions[0].length; a++){
            final Distribution[] d = distributions[t][a];
            this.stateActionDependentDistributions[t][a] = IntStream.iterate(0, i -> i + 1)
                                                                    .limit(d.length)
                                                                    .mapToObj(i -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                                                                   d[i], 0, StateImpl.getMaxState()-StateImpl.getMinState(), stepSize))
                                                                    .toArray(DiscreteDistribution[]::new);
         }
      }
      this.randomOutcomeFunction = randomOutcomeFunction;
      this.stateSpace = stateSpace;
   }   

   protected RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction;
   
   @Override
   public double getTransitionProbability(State initialState, Action action, State finalState) {
      switch(distributionType){
      case STATE_ACTION_DEPENDENT:
         return this.getStateActionDependentTransitionProbability(initialState, action, finalState);
      case STATE_INDEPENDENT:
         return this.getStateIndependentTransitionProbability(initialState, action, finalState);
      case STATE_DEPENDENT: 
         return this.getStateDependentTransitionProbability(initialState, action, finalState);
      default:
         throw new NullPointerException("Method not implemented");
      }
   }
   
   private double getStateActionDependentTransitionProbability(State initialState, Action action, State finalState) {
      int period = ((StateImpl)initialState).getPeriod();
      return this.stateActionDependentDistributions[period][((ActionImpl)action).getIntAction()][((StateImpl)initialState).getInitialIntState()].prob(((StateImpl)finalState).getInitialIntState());
   }
   
   private double getStateIndependentTransitionProbability(State initialState, Action action, State finalState) {
      int randomOutcome = StateImpl.stateToIntState(this.randomOutcomeFunction.apply(initialState, action, finalState));
      int period = ((StateImpl)initialState).getPeriod();
      return this.stateIndependentDistributions[period].prob(randomOutcome);
   }
   
   private double getStateDependentTransitionProbability(State initialState, Action action, State finalState) {
      throw new NullPointerException("Method not implemented");
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


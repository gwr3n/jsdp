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

package jsdp.sdp.impl.multivariate;

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
import umontreal.ssj.probdistmulti.DiscreteDistributionIntMulti;

public class TransitionProbabilityImpl extends TransitionProbability {
   DiscreteDistributionIntMulti[] multiVariateDistributions;
   DiscreteDistribution[][] univariateDistributions;
   StateSpaceImpl[] stateSpace;

   private enum Mode {
      MULTIVARIATE,
      INDEPENDENT_UNIVARIATE
   };
   
   Mode distributionMode;
   
   public TransitionProbabilityImpl(DiscreteDistributionIntMulti[] multiVariateDistributions,
                                    RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction,
                                    StateSpaceImpl[] stateSpace){
      this.multiVariateDistributions = multiVariateDistributions;
      this.randomOutcomeFunction = randomOutcomeFunction;
      this.stateSpace = stateSpace;
      this.distributionMode = Mode.MULTIVARIATE;
   }
   
   public TransitionProbabilityImpl(Distribution[][] univariateDistributions,
                                    RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction,
                                    StateSpaceImpl[] stateSpace,
                                    double[] stepSize){
      
      this.univariateDistributions = IntStream.iterate(0, j -> j + 1).limit(univariateDistributions.length).mapToObj(j -> 
         IntStream.iterate(0, i -> i + 1).limit(univariateDistributions[j].length)
                  .mapToObj(i -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(univariateDistributions[j][i], 0, StateImpl.getMaxState()[i]-StateImpl.getMinState()[i], stepSize[i]))
                  .toArray(DiscreteDistribution[]::new)
      ).toArray(DiscreteDistribution[][]::new);
      
      this.randomOutcomeFunction = randomOutcomeFunction;
      this.stateSpace = stateSpace;
      this.distributionMode = Mode.INDEPENDENT_UNIVARIATE;
   }

   protected RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction;
   
   @Override
   public double getTransitionProbability(State initialState, Action action, State finalState) {
      switch(this.distributionMode){
      case INDEPENDENT_UNIVARIATE:
         return getUnivariateTransitionProbability(initialState, action, finalState);
      case MULTIVARIATE:
         return getMultivariateTransitionProbability(initialState, action, finalState);
      default:
         return Double.NaN;
      }
   }
   
   private double getMultivariateTransitionProbability(State initialState, Action action, State finalState) {
      int[] randomOutcome = StateImpl.stateToIntState(this.randomOutcomeFunction.apply(initialState, action, finalState));
      int period = ((StateImpl)initialState).getPeriod();
      return this.multiVariateDistributions[period].prob(randomOutcome);
   }
   
   private double getUnivariateTransitionProbability(State initialState, Action action, State finalState) {
      int[] randomOutcome = StateImpl.stateToIntState(this.randomOutcomeFunction.apply(initialState, action, finalState));
      int period = ((StateImpl)initialState).getPeriod();
      double transitionProbability = 1;
      for(int i = 0; i < univariateDistributions[period].length; i++){
         transitionProbability *= this.univariateDistributions[period][i].prob(randomOutcome[i]);
      }
      return transitionProbability;
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


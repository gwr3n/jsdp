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
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.BackwardRecursion;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.ValueRepository;
import jsdp.sdp.impl.multivariate.StateImpl;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdistmulti.DiscreteDistributionIntMulti;

/**
 * A concrete implementation of a backward recursion procedure to compute (s,S) policy parameters.
 * 
 * @author Roberto Rossi
 *
 */
public class BackwardRecursionImpl extends BackwardRecursion{
   
   /**
    * Creates an instance of the problem and initialises state space, transition probability and value repository.
    * 
    * @param demand the distribution of random demand in each period, an array of {@code Distribution}.
    * @param immediateValueFunction the immediate value function.
    * @param randomOutcomeFunction the random outcome function.
    * @param buildActionList the action list builder.
    * @param idempotentAction the idempotent action; i.e. an action that leaves the system in the same state from period {@code t} to period {@code t+1}.
    * @param samplingScheme the sampling scheme adopted.
    * @param maxSampleSize the maximum sample size.
    */
   public BackwardRecursionImpl(OptimisationDirection optimisationDirection,
                                DiscreteDistributionIntMulti[] demand,
                                ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                                RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction,
                                Function<State, ArrayList<Action>> buildActionList,
                                Function<State, Action> idempotentAction,
                                double discountFactor,
                                SamplingScheme samplingScheme,
                                int maxSampleSize){
      super(optimisationDirection);
      this.horizonLength = demand.length;
      
      this.stateSpace = new StateSpaceImpl[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new StateSpaceImpl(i, buildActionList, idempotentAction, samplingScheme, maxSampleSize);
      this.transitionProbability = new TransitionProbabilityImpl(
            demand,randomOutcomeFunction,(StateSpaceImpl[])this.getStateSpace());
      this.valueRepository = new ValueRepository(immediateValueFunction, discountFactor);
   }
   
   /**
    * Creates an instance of the problem and initialises state space, transition probability and value repository.
    * 
    * @param demand the distribution of random demand in each period, an array of {@code Distribution}.
    * @param immediateValueFunction the immediate value function.
    * @param randomOutcomeFunction the random outcome function.
    * @param buildActionList the action list builder.
    * @param idempotentAction the idempotent action; i.e. an action that leaves the system in the same state from period {@code t} to period {@code t+1}.
    * @param samplingScheme the sampling scheme adopted.
    * @param maxSampleSize the maximum sample size.
    */
   public BackwardRecursionImpl(OptimisationDirection optimisationDirection,
                                Distribution[][] demand,
                                ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                                RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction,
                                Function<State, ArrayList<Action>> buildActionList,
                                Function<State, Action> idempotentAction,
                                double discountFactor,
                                SamplingScheme samplingScheme,
                                int maxSampleSize){
      super(optimisationDirection);
      this.horizonLength = demand.length;
      
      this.stateSpace = new StateSpaceImpl[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new StateSpaceImpl(i, buildActionList, idempotentAction, samplingScheme, maxSampleSize);
      this.transitionProbability = new TransitionProbabilityImpl(
            demand,randomOutcomeFunction,(StateSpaceImpl[])this.getStateSpace(),StateImpl.getStepSize());
      this.valueRepository = new ValueRepository(immediateValueFunction, discountFactor);
   }
   
   @Override
   public TransitionProbabilityImpl getTransitionProbability(){
      return (TransitionProbabilityImpl) this.transitionProbability; 
   }
   
   public double getExpectedCost(double[] initialState){
      StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(0, StateImpl.stateToIntState(initialState));
      return getExpectedCost(stateDescriptor);
   }
   
   public double getExpectedCost(StateDescriptorImpl stateDescriptor){
      State state = ((StateSpaceImpl)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      try{
         return getExpectedValue(state);
      }catch(NullPointerException e){
         recurse(0);
         return getExpectedValue(state);
      }
   }
   
   public ActionImpl getOptimalAction(StateDescriptorImpl stateDescriptor){
      State state = ((StateSpaceImpl)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return (ActionImpl) this.getValueRepository().getOptimalAction(state);
   }
}

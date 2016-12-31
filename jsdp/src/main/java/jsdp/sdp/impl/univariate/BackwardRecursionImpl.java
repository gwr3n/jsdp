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
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.BackwardRecursion;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.ValueRepository;

import umontreal.ssj.probdist.Distribution;

/**
 * A concrete implementation of a backward recursion procedure to compute (s,S) policy parameters.
 * 
 * @author Roberto Rossi
 *
 */
public class BackwardRecursionImpl extends BackwardRecursion{
   
   /**
    * Creates an instance of the problem and initializes state space, transition probability and value repository.
    * 
    * @param optimisationDirection specifies if this is a mininimisation or a maximisation problem 
    * @param demand the state-independent distribution of random demand in each period, an array of {@code Distribution}.
    * @param supportLB the lower bounds for the state dependent distribution of random demand in each period.
    * @param supportUB the upper bounds for the state dependent distribution of random demand in each period.
    * @param immediateValueFunction the immediate value function.
    * @param randomOutcomeFunction the random outcome function.
    * @param buildActionList the action list builder.
    * @param idempotentAction the idempotent action; i.e. an action that leaves the system in the same state from period {@code t} to period {@code t+1}.
    * @param discountFactor the discount factor in the functional equation        
    * @param samplingScheme the sampling scheme adopted.
    * @param maxSampleSize the maximum sample size.
    * @param reductionFactorPerStage the sample waning exponential state reduction factor
    * @param hash the type of hash used to store the state space
    */
   public BackwardRecursionImpl(OptimisationDirection optimisationDirection,
                                Distribution[] demand,
                                double[] supportLB,
                                double[] supportUB,
                                ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                                RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction,
                                Function<State, ArrayList<Action>> buildActionList,
                                Function<State, Action> idempotentAction,
                                double discountFactor,
                                SamplingScheme samplingScheme,
                                int maxSampleSize,
                                double reductionFactorPerStage,
                                HashType hash){
      super(optimisationDirection);
      this.horizonLength = demand.length;
      
      this.stateSpace = new StateSpaceImpl[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new StateSpaceImpl(i, buildActionList, idempotentAction, hash, samplingScheme, maxSampleSize, reductionFactorPerStage);
      this.transitionProbability = new TransitionProbabilityImpl(
            demand,supportLB,supportUB,randomOutcomeFunction,(StateSpaceImpl[])this.getStateSpace(),StateImpl.getStepSize());
      this.valueRepository = new ValueRepository(immediateValueFunction, discountFactor, hash);
   }
   
   /**
    * Creates an instance of the problem and initializes state space, transition probability and value repository.
    * 
    * @param optimisationDirection specifies if this is a mininimisation or a maximisation problem 
    * @param demand the state-independent distribution of random demand in each period, an array of {@code Distribution}.
    * @param supportLB the lower bounds for the state dependent distribution of random demand in each period.
    * @param supportUB the upper bounds for the state dependent distribution of random demand in each period.
    * @param immediateValueFunction the immediate value function.
    * @param randomOutcomeFunction the random outcome function.
    * @param buildActionList the action list builder.
    * @param idempotentAction the idempotent action; i.e. an action that leaves the system in the same state from period {@code t} to period {@code t+1}.
    * @param discountFactor the discount factor in the functional equation    
    * @param samplingScheme the sampling scheme adopted.
    * @param maxSampleSize the maximum sample size.
    * @param reductionFactorPerStage the sample waning exponential state reduction factor
    * @param stateSpaceSizeLowerBound the maximum size of hashtables used to store the state space
    * @param loadFactor hashtable load factor (typically 0.8)
    * @param hash the type of hash used to store the state space
    */
   
   public BackwardRecursionImpl(OptimisationDirection optimisationDirection,
                                Distribution[] demand,
                                double[] supportLB,
                                double[] supportUB,                                
                                ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                                RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction,
                                Function<State, ArrayList<Action>> buildActionList,
                                Function<State, Action> idempotentAction,
                                double discountFactor,
                                SamplingScheme samplingScheme,
                                int maxSampleSize,
                                double reductionFactorPerStage,
                                int stateSpaceSizeLowerBound, 
                                float loadFactor,
                                HashType hash){
      super(optimisationDirection);
      this.horizonLength = demand.length;
      
      this.stateSpace = new StateSpaceImpl[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new StateSpaceImpl(i, buildActionList, idempotentAction, hash, samplingScheme, maxSampleSize, reductionFactorPerStage, stateSpaceSizeLowerBound, loadFactor);
      this.transitionProbability = new TransitionProbabilityImpl(
            demand,supportLB,supportUB,randomOutcomeFunction,(StateSpaceImpl[])this.getStateSpace(),StateImpl.getStepSize());
      this.valueRepository = new ValueRepository(immediateValueFunction, discountFactor, stateSpaceSizeLowerBound, loadFactor, hash);
   }
   
   /**
    * Creates an instance of the problem and initializes state space, transition probability and value repository.
    * 
    * @param optimisationDirection specifies if this is a mininimisation or a maximisation problem 
    * @param demand the state dependent distribution of random demand in each period, a two-dimensional array of {@code Distribution}, first index is the time period, second index is the state index.
    * @param supportLB the lower bounds for the state dependent distribution of random demand in each period.
    * @param supportUB the upper bounds for the state dependent distribution of random demand in each period.
    * @param immediateValueFunction the immediate value function.
    * @param randomOutcomeFunction the random outcome function.
    * @param buildActionList the action list builder.
    * @param idempotentAction the idempotent action; i.e. an action that leaves the system in the same state from period {@code t} to period {@code t+1}.
    * @param discountFactor the discount factor in the functional equation
    * @param samplingScheme the sampling scheme adopted.
    * @param maxSampleSize the maximum sample size.
    * @param reductionFactorPerStage the sample waning exponential state reduction factor
    * @param hash the type of hashtable used
    */
   public BackwardRecursionImpl(OptimisationDirection optimisationDirection,
                                Distribution[][] demand,
                                double[][] supportLB,
                                double[][] supportUB,          
                                ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                                RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction,
                                Function<State, ArrayList<Action>> buildActionList,
                                Function<State, Action> idempotentAction,
                                double discountFactor,
                                SamplingScheme samplingScheme,
                                int maxSampleSize,
                                double reductionFactorPerStage,
                                HashType hash){
      super(optimisationDirection);
      throw new NullPointerException("Method not implemented!");
   }
   
   /**
    * Creates an instance of the problem and initializes state space, transition probability and value repository.
    * 
    * @param optimisationDirection specifies if this is a mininimisation or a maximisation problem 
    * @param demand the state and action dependent distribution of random demand in each period, a three-dimensional array of {@code Distribution}, first index is the time period, second index is the action index, third index is the state index.
    * @param supportLB the lower bound of demand support
    * @param supportUB the upper bound of demand support
    * @param immediateValueFunction the immediate value function.
    * @param randomOutcomeFunction the random outcome function.
    * @param buildActionList the action list builder.
    * @param idempotentAction the idempotent action; i.e. an action that leaves the system in the same state from period {@code t} to period {@code t+1}.
    * @param discountFactor the discount factor in the functional equation
    * @param samplingScheme the sampling scheme adopted.
    * @param maxSampleSize the maximum sample size.
    * @param reductionFactorPerStage the sample waning exponential state reduction factor
    * @param hash the type of hashtable used
    */
   public BackwardRecursionImpl(OptimisationDirection optimisationDirection,
                                Distribution[][][] demand,
                                double[][][] supportLB,
                                double[][][] supportUB,            
                                ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                                RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction,
                                Function<State, ArrayList<Action>> buildActionList,
                                Function<State, Action> idempotentAction,
                                double discountFactor,
                                SamplingScheme samplingScheme,
                                int maxSampleSize,
                                double reductionFactorPerStage,
                                HashType hash){
      super(optimisationDirection);
      this.horizonLength = demand.length;

      this.stateSpace = new StateSpaceImpl[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new StateSpaceImpl(i, buildActionList, idempotentAction, hash, samplingScheme, maxSampleSize, reductionFactorPerStage);
      this.transitionProbability = new TransitionProbabilityImpl(
            demand,supportLB,supportUB,randomOutcomeFunction,(StateSpaceImpl[])this.getStateSpace(),StateImpl.getStepSize());
      this.valueRepository = new ValueRepository(immediateValueFunction, discountFactor, hash);
   }
   
   @Override
   public TransitionProbabilityImpl getTransitionProbability(){
      return (TransitionProbabilityImpl) this.transitionProbability; 
   }
   
   public double getExpectedCost(double initialState){
      StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(0, initialState);
      return getExpectedCost(stateDescriptor);
   }
   
   public double getExpectedCost(StateDescriptorImpl stateDescriptor){
      State state = ((StateSpaceImpl)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return getExpectedValue(state);
   }
   
   public ActionImpl getOptimalAction(StateDescriptorImpl stateDescriptor){
      State state = ((StateSpaceImpl)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return (ActionImpl) this.getValueRepository().getOptimalAction(state);
   }
}

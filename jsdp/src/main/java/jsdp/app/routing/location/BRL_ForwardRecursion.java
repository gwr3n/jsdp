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

package jsdp.app.routing.location;

import java.util.ArrayList;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.ForwardRecursion;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;
import jsdp.sdp.ValueRepository;
import jsdp.sdp.impl.univariate.SamplingScheme;

public class BRL_ForwardRecursion extends ForwardRecursion {
   
   double[][][] machineLocation; 
   int[][] fuelConsumption;
   
   public BRL_ForwardRecursion(int horizonLength,
                               double[][][] machineLocation, 
                               int[][] fuelConsumption,
                               ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                               Function<State, ArrayList<Action>> buildActionList,
                               double discountFactor,
                               HashType hashType,
                               int stateSpaceSizeLowerBound,
                               float loadFactor,
                               SamplingScheme samplingScheme,
                               int sampleSize,
                               double reductionFactorPerStage){
      super(OptimisationDirection.MIN);
      this.horizonLength = horizonLength;
      this.machineLocation = machineLocation;
      this.fuelConsumption = fuelConsumption;

      this.stateSpace = new BRL_StateSpace[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new BRL_StateSpace(i, buildActionList, hashType, stateSpaceSizeLowerBound, loadFactor); 
      this.transitionProbability = new BRL_TransitionProbability(machineLocation, 
                                                                fuelConsumption, 
                                                                (BRL_StateSpace[])this.getStateSpace(),
                                                                samplingScheme,
                                                                sampleSize,
                                                                reductionFactorPerStage);
      this.valueRepository = new ValueRepository(immediateValueFunction, 
                                                 discountFactor, 
                                                 stateSpaceSizeLowerBound, 
                                                 loadFactor, 
                                                 hashType);   
   }
   
   public double getExpectedCost(BRL_StateDescriptor stateDescriptor){
      State state = ((BRL_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return getExpectedValue(state);
   }
   
   public BRL_Action getOptimalAction(BRL_StateDescriptor stateDescriptor){
      State state = ((BRL_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return (BRL_Action) this.getValueRepository().getOptimalAction(state);
   }
}

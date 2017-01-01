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
import java.util.Iterator;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.State;
import jsdp.sdp.StateSpace;

/**
 * A concrete implementation of {@code StateSpace}.
 * 
 * @author Roberto Rossi
 *
 */
public class StateSpaceImpl extends StateSpace<StateDescriptorImpl>{

   SamplingScheme samplingScheme = SamplingScheme.NONE;
   int maxSampleSize = Integer.MAX_VALUE;
   double reductionFactorPerStage = 1;
   
   public StateSpaceImpl(int period,
                         Function<State, ArrayList<Action>> buildActionList,
                         Function<State, Action> idempotentAction,
                         HashType hash){
      super(period, hash);
      StateSpace.buildActionList = buildActionList;
      StateSpace.idempotentAction = idempotentAction;
   }
   
   public StateSpaceImpl(int period,
                         Function<State, ArrayList<Action>> buildActionList,
                         Function<State, Action> idempotentAction,
                         HashType hash,
                         int stateSpaceSizeLowerBound, 
                         float loadFactor){
      super(period, hash, stateSpaceSizeLowerBound, loadFactor);
      StateSpace.buildActionList = buildActionList;
      StateSpace.idempotentAction = idempotentAction;
   }   
  
   public StateSpaceImpl(int period, 
                         Function<State, ArrayList<Action>> buildActionList,
                         Function<State, Action> idempotentAction,
                         SamplingScheme samplingScheme,
                         int maxSampleSize,
                         HashType hash,
                         double reductionFactorPerStage){
      super(period, hash);
      StateSpace.buildActionList = buildActionList;
      StateSpace.idempotentAction = idempotentAction;
      this.setSamplingScheme(samplingScheme, maxSampleSize, reductionFactorPerStage);
   }
   
   public StateSpaceImpl(int period, 
                         Function<State, ArrayList<Action>> buildActionList,
                         Function<State, Action> idempotentAction,
                         SamplingScheme samplingScheme,
                         int maxSampleSize,
                         double reductionFactorPerStage,
                         HashType hash,
                         int stateSpaceSizeLowerBound, 
                         float loadFactor){
      super(period, hash, stateSpaceSizeLowerBound, loadFactor);
      StateSpace.buildActionList = buildActionList;
      StateSpace.idempotentAction = idempotentAction;
      this.setSamplingScheme(samplingScheme, maxSampleSize, reductionFactorPerStage);
   }
   
   public void setSamplingScheme(SamplingScheme samplingScheme, int maxSampleSize, double reductionFactorPerStage){
      switch(samplingScheme){
      case NONE:
         this.samplingScheme = SamplingScheme.NONE;
         this.maxSampleSize = Integer.MAX_VALUE;
         this.reductionFactorPerStage = 1;
         break;
      default: 
         this.samplingScheme = samplingScheme;
         this.maxSampleSize = maxSampleSize;
         this.reductionFactorPerStage = reductionFactorPerStage;
      }
   }

   public boolean exists (StateDescriptorImpl descriptor){
      return states.get(descriptor) != null;
   }
   
   public State getState(StateDescriptorImpl descriptor){
      State value = states.get(descriptor);
      if(value == null){
         State state = new StateImpl(descriptor);
         this.states.put(descriptor, state);
         return state;
      }else
         return (StateImpl) value;
   }

   public Iterator<State> iterator() {
      /**
       * This implementation is correct because the optimal cost will be generated on 
       * demand for period 0, see BackwardRecursionImpl.getExpectedCost
       */
      if(this.period == 0) 
         return null;
      else if(this.samplingScheme == SamplingScheme.NONE)
         return new StateSpaceIteratorImpl(this);
      else
         return new StateSpaceSampleIteratorImpl(this, this.samplingScheme, this.maxSampleSize, this.reductionFactorPerStage);
   }
}

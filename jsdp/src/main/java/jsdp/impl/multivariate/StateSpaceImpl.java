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

package jsdp.impl.multivariate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

import jsdp.sdp.Action;
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
   
   public StateSpaceImpl(int period,
                         Function<State, ArrayList<Action>> buildActionList,
                         Function<State, Action> idempotentAction){
      super(period);
      this.buildActionList = buildActionList;
      this.idempotentAction = idempotentAction;
   }
   
   public StateSpaceImpl(int period, 
                         Function<State, ArrayList<Action>> buildActionList,
                         Function<State, Action> idempotentAction,
                         SamplingScheme samplingScheme,
                         int maxSampleSize){
      super(period);
      this.buildActionList = buildActionList;
      this.idempotentAction = idempotentAction;
      this.setSamplingScheme(samplingScheme, maxSampleSize);
   }
   
   public void setSamplingScheme(SamplingScheme samplingScheme, int maxSampleSize){
      switch(samplingScheme){
      case NONE:
         this.samplingScheme = SamplingScheme.NONE;
         this.maxSampleSize = Integer.MAX_VALUE;
         break;
      default: 
         this.samplingScheme = samplingScheme;
         this.maxSampleSize = maxSampleSize;
      }
   }

   public boolean exists (StateDescriptorImpl descriptor){
      return states.get(descriptor) != null;
   }
   
   public State getState(StateDescriptorImpl descriptor){
      State value = states.get(descriptor);
      if(value == null){
         State state = new StateImpl(descriptor, this.buildActionList, this.idempotentAction);
         this.states.put(descriptor, state);
         return state;
      }else
         return (StateImpl) value;
   }

   public Iterator<State> iterator() {
      if(this.period == 0)
         return null;
      else if(this.samplingScheme == SamplingScheme.NONE)
         return new StateSpaceIteratorImpl(this);
      else
         return new StateSpaceSampleIteratorImpl(this, this.samplingScheme, this.maxSampleSize);
   }
}

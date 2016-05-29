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

import java.util.Iterator;

import jsdp.sdp.State;
import jsdp.sdp.StateSpace;

public class sS_StateSpace extends StateSpace<sS_StateDescriptor>{

   sS_StateSpaceSampleIterator.SamplingScheme samplingScheme = sS_StateSpaceSampleIterator.SamplingScheme.NONE;
   int maxSampleSize = Integer.MAX_VALUE;
   
   public sS_StateSpace(int period){
      super(period);
   }
   
   public sS_StateSpace(int period, 
                        sS_StateSpaceSampleIterator.SamplingScheme samplingScheme,
                        int maxSampleSize){
      super(period);
      this.setSamplingScheme(samplingScheme, maxSampleSize);
   }
   
   public void setSamplingScheme(sS_StateSpaceSampleIterator.SamplingScheme samplingScheme, int maxSampleSize){
      switch(samplingScheme){
      case NONE:
         this.samplingScheme = sS_StateSpaceSampleIterator.SamplingScheme.NONE;
         this.maxSampleSize = Integer.MAX_VALUE;
         break;
      default: 
         this.samplingScheme = samplingScheme;
         this.maxSampleSize = maxSampleSize;
      }
   }

   public boolean exists (sS_StateDescriptor descriptor){
      return states.get(descriptor) != null;
   }
   
   public State getState(sS_StateDescriptor descriptor){
      State value = states.get(descriptor);
      if(value == null){
         State state = new sS_State(descriptor);
         this.states.put(descriptor, state);
         return state;
      }else
         return (sS_State) value;
   }

   public Iterator<State> iterator() {
      if(period == 0 || this.samplingScheme == sS_StateSpaceSampleIterator.SamplingScheme.NONE)
         return new sS_StateSpaceIterator(this);
      else
         return new sS_StateSpaceSampleIterator(this, this.maxSampleSize);
   }
}

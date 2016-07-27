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

import jsdp.sdp.StateDescriptor;

/**
 * A concrete implementation of {@code StateDescriptor}.
 * 
 * @author Roberto Rossi
 *
 */
public class StateDescriptorImpl extends StateDescriptor{

   private static final long serialVersionUID = 1L;
   
   int initialIntState;

   public StateDescriptorImpl(int period, int initialIntState){
      super(period);
      this.initialIntState = initialIntState;
   }
   
   public StateDescriptorImpl(int period, double initialState){
      super(period);
      this.initialIntState = StateImpl.stateToIntState(initialState);
   }
   
   @Override
   public boolean equals(Object descriptor){
      if(descriptor instanceof StateDescriptorImpl)
         return this.period == ((StateDescriptorImpl)descriptor).period &&
         this.initialIntState == ((StateDescriptorImpl)descriptor).initialIntState;
      else
         return false;
   }

   @Override
   public int hashCode(){
      String hash = "";
      hash = (hash + period) + "_" + initialIntState;
      return hash.hashCode();
   }

   public int getInitialIntState(){
      return initialIntState;
   }
}
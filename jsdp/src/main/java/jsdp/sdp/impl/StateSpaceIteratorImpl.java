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

import jsdp.sdp.State;
import jsdp.sdp.StateSpaceIterator;

public class StateSpaceIteratorImpl extends StateSpaceIterator {

   StateSpaceImpl stateSpace;
   StateDescriptorImpl currentStateDescriptor;

   public StateSpaceIteratorImpl(StateSpaceImpl stateSpace){
      this.stateSpace = stateSpace;
      currentStateDescriptor = new StateDescriptorImpl(this.stateSpace.getPeriod(), StateImpl.getMaxIntState());
   }

   public boolean hasNext() {
      if(currentStateDescriptor.getInitialIntState() <= StateImpl.getMaxIntState() && 
            currentStateDescriptor.getInitialIntState() >= StateImpl.getMinIntState())
         return true;
      else
         return false;
   }

   public State next() {
      if(currentStateDescriptor.getInitialIntState() <= StateImpl.getMaxIntState() && 
            currentStateDescriptor.getInitialIntState() >= StateImpl.getMinIntState()){
         State state = stateSpace.getState(currentStateDescriptor);
         currentStateDescriptor = new StateDescriptorImpl(currentStateDescriptor.getPeriod(), 
               currentStateDescriptor.getInitialIntState() - 1);
         return state;
      }else{
         return null;
      }
   }
}

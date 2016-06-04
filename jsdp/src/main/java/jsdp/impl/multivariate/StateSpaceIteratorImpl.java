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

import jsdp.sdp.State;
import jsdp.sdp.StateSpaceIterator;

/**
 * A concrete implementation of {@code StateSpaceIterator}.
 * 
 * @author Roberto Rossi
 *
 */
public class StateSpaceIteratorImpl extends StateSpaceIterator {

   StateSpaceImpl stateSpace;
   StateDescriptorImpl currentStateDescriptor;
   int stateSpacePointer;
   
   public StateSpaceIteratorImpl(StateSpaceImpl stateSpace){
      this.stateSpace = stateSpace;
      currentStateDescriptor = new StateDescriptorImpl(this.stateSpace.getPeriod(), StateImpl.getMaxIntState());
      stateSpacePointer = getStateSpaceCardinality() - 1;
   }
   
   private static int getStateSpaceCardinality(){
      int cardinality = 1;
      for(int i = 0; i < StateImpl.getStateDimension(); i++){
         cardinality *= StateImpl.getMaxIntState()[i] - StateImpl.getMinIntState()[i] + 1;
      }
      return cardinality;
   }
   
   private static int getResidualStateSpaceCardinality(int j){
      int cardinality = 1;
      for(int i = j; i < StateImpl.getStateDimension(); i++){
         cardinality *= StateImpl.getMaxIntState()[i] - StateImpl.getMinIntState()[i] + 1;
      }
      return cardinality;
   }
   
   private static int[] getIntState(int stateSpacePointer){
      int[] intState = new int[StateImpl.getStateDimension()];
      for(int i = 0; i < StateImpl.getStateDimension(); i++){
         intState[i] = Math.floorDiv(Math.floorMod(stateSpacePointer, getResidualStateSpaceCardinality(i)), i < StateImpl.getStateDimension() - 1 ? getResidualStateSpaceCardinality(i+1) : 1) + StateImpl.getMinIntState()[i];
      }
      return intState;
   }
   
   public boolean hasNext() {
      if(stateSpacePointer >= 0)
         return true;
      else
         return false;
   }
   
   public State next() {
      if(stateSpacePointer >= 0){
         State state = stateSpace.getState(currentStateDescriptor);
         stateSpacePointer -= 1;
         if(stateSpacePointer >= 0 )
            currentStateDescriptor = new StateDescriptorImpl(currentStateDescriptor.getPeriod(), getIntState(stateSpacePointer));
         else
            currentStateDescriptor = null;
         return state;
      }else{
         return null;
      }
   }
}

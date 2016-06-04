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

package jsdp.app.inventory.multivariate;

import jsdp.impl.multivariate.ActionImpl;
import jsdp.impl.multivariate.StateImpl;
import jsdp.sdp.Action;
import jsdp.sdp.ActionIterator;

/**
 * A concrete implementation of {@code StateSpaceIterator}.
 * 
 * @author Roberto Rossi
 *
 */
public class ActionIteratorImpl extends ActionIterator{

   StateImpl state;
   ActionImpl currentAction;
   int actionPointer;
   
   public ActionIteratorImpl(StateImpl state){
      this.state = state;
      actionPointer = getActionSpaceCardinality(this.state);
      currentAction = new ActionImpl(this.state, getIntAction(actionPointer, this.state));
   }
   
   private static int getActionSpaceCardinality(StateImpl state){
      int cardinality = 1;
      for(int i = 0; i < StateImpl.getStateDimension(); i++){
         cardinality *= StateImpl.getMaxIntState()[i] - state.getInitialIntState()[i] + 1;
      }
      return cardinality;
   }
   
   private static int getResidualStateSpaceCardinality(int j, StateImpl state){
      int cardinality = 1;
      for(int i = j; i < StateImpl.getStateDimension(); i++){
         cardinality *= StateImpl.getMaxIntState()[i] - state.getInitialIntState()[i] + 1;
      }
      return cardinality;
   }
   
   private static int[] getIntAction(int actionPointer, StateImpl state){
      int[] intState = new int[StateImpl.getStateDimension()];
      for(int i = 0; i < StateImpl.getStateDimension(); i++){
         intState[i] = Math.floorDiv(Math.floorMod(actionPointer, getResidualStateSpaceCardinality(i, state)), i < StateImpl.getStateDimension() - 1 ? getResidualStateSpaceCardinality(i+1, state) : 1);
      }
      return intState;
   }
   
   public boolean hasNext() {
      if(actionPointer > 0)
         return true;
      else
         return false;
   }
   
   public Action next() {
      if(actionPointer > 0){
         Action action = currentAction;
         actionPointer -= 1;
         if(actionPointer > 0 )
            currentAction = new ActionImpl(this.state, getIntAction(actionPointer, this.state));
         else
            currentAction = null;
         return action;
      }else{
         return null;
      }
   }
}

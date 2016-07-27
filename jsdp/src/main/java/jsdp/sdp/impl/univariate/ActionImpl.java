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

import jsdp.sdp.Action;
import jsdp.sdp.State;

/**
 * A concrete implementation of {@code Action}.
 * 
 * @author Roberto Rossi
 *
 */
public class ActionImpl extends Action {
   
   private static final long serialVersionUID = 1L;
   
   int intAction;
   
   public static double intActionToAction(int action){
      return action*StateImpl.getStepSize();
   }
   
   public static int actionToIntAction(double action){
      return (int) Math.round(action/StateImpl.getStepSize());
   }
   
   public ActionImpl(State state, int intAction){
      super(state);
      this.intAction = intAction;
   }
   
   public ActionImpl(State state, double action){
      super(state);
      this.intAction = actionToIntAction(action);
   }
   
   public int getIntAction(){
      return this.intAction;
   }
   
   public double getAction(){
      return intActionToAction(this.intAction);
   }
   
   @Override
   public boolean equals(Object action){
      if(action instanceof ActionImpl)
         return this.intAction == ((ActionImpl)action).intAction;
      else
         return false;
   }
   
   @Override
   public int hashCode(){
      String hash = "";
        hash = (hash + intAction);
        return hash.hashCode();
   }
   
   @Override
   public String toString(){
      return state+"\tAction: "+ intActionToAction(this.intAction);
   }
}


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

package jsdp.app.inventory.multivariate.impl;

import java.util.Arrays;

import jsdp.sdp.Action;
import jsdp.sdp.State;

/**
 * A concrete implementation of {@code Action}.
 * 
 * @author Roberto Rossi
 *
 */
public class ActionImpl extends Action {
   
   int[] intAction;
   
   private static double[] arrayProduct(int[] integerArray, double[] doubleArray){
      if(integerArray.length != doubleArray.length)
         throw new NullPointerException("Array sizes do not agree");
      double[] result = new double[integerArray.length];
      for(int i = 0; i < integerArray.length; i++){
         result[i] = integerArray[i]*doubleArray[i];
      }
      return result;
   } 
   
   public static double[] intActionToAction(int[] action){
      return arrayProduct(action, StateImpl.getStepSize());
   }
   
   private static double[] arrayDivision(double[] doubleArray1, double[] doubleArray2){
      if(doubleArray1.length != doubleArray2.length)
         throw new NullPointerException("Array sizes do not agree");
      double[] result = new double[doubleArray1.length];
      for(int i = 0; i < doubleArray1.length; i++){
         result[i] = doubleArray1[i]/doubleArray2[i];
      }
      return result;
   } 
   
   public static int[] actionToIntAction(double[] action){
      double[] result = arrayDivision(action, StateImpl.getStepSize());
      int[] intResult = new int[result.length];
      for(int i = 0; i < result.length; i++){
         intResult[i] = (int) Math.round(result[i]);
      }
      return intResult;
   }
   
   public ActionImpl(State state, int[] intAction){
      super(state);
      this.intAction = Arrays.copyOf(intAction, intAction.length);
   }
   
   public ActionImpl(State state, double[] action){
      super(state);
      this.intAction = actionToIntAction(action);
   }
   
   public int[] getIntAction(){
      return this.intAction;
   }
   
   public double[] getAction(){
      return intActionToAction(this.intAction);
   }
   
   @Override
   public boolean equals(Object action){
      if(action instanceof ActionImpl)
         return Arrays.equals(this.intAction, ((ActionImpl)action).intAction);
      else
         return false;
   }
   
   @Override
   public int hashCode(){
      String hash = "";
        hash = (hash + Arrays.toString(intAction));
        return hash.hashCode();
   }
   
   @Override
   public String toString(){
      return state+"\tAction: "+ Arrays.toString(intActionToAction(this.intAction));
   }
}


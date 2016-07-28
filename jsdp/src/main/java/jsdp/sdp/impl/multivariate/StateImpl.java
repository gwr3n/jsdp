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
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import jsdp.sdp.Action;
import jsdp.sdp.State;

/**
 * A concrete implementation of {@code State}.
 * 
 * @author Roberto Rossi
 *
 */
public class StateImpl extends State {

   private static final long serialVersionUID = 1L;
   
   private int initialIntState[];

   private static double[] stepSize;
   private static int[] minIntState;
   private static int[] maxIntState;

   /**
    * Initializes the state space boundaries. Note that {@code stepSize} must be one if 
    * {@code DiscreteDistributionInt} are used.
    * 
    * @param stepSize the discretization step used to encode the state space
    * @param minState the minimum value used to encode a state
    * @param maxState the maximum value used to encode a state
    */
   public static void setStateBoundaries(double[] stepSize, double[] minState, double[] maxState){
      if(stepSize.length != minState.length || stepSize.length != maxState.length)
         throw new NullPointerException("Array sizes do not agree");
      StateImpl.stepSize = Arrays.copyOf(stepSize, stepSize.length);
      StateImpl.minIntState = IntStream.iterate(0, i -> i + 1).limit(minState.length).map(i -> (int)Math.round(minState[i]/stepSize[i])).toArray();
      StateImpl.maxIntState = IntStream.iterate(0, i -> i + 1).limit(maxState.length).map(i -> (int)Math.round(maxState[i]/stepSize[i])).toArray();
   }
   
   public static int getStateDimension(){
      return stepSize.length;
   }

   public static double[] getStepSize(){
      return StateImpl.stepSize;
   }
   
   private static double[] arrayProduct(int[] integerArray, double[] doubleArray){
      if(integerArray.length != doubleArray.length)
         throw new NullPointerException("Array sizes do not agree");
      double[] result = new double[integerArray.length];
      for(int i = 0; i < integerArray.length; i++){
         result[i] = integerArray[i]*doubleArray[i];
      }
      return result;
   } 

   public static double[] intStateToState(int[] intState){
      return arrayProduct(intState, stepSize);
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

   public static int[] stateToIntState(double[] state){
      double[] result = arrayDivision(state, stepSize);
      int[] intResult = new int[result.length];
      for(int i = 0; i < result.length; i++){
         intResult[i] = (int) Math.max(Math.min(Math.round(result[i]), StateImpl.maxIntState[i]), StateImpl.minIntState[i]);
      }
      return intResult;
   }

   public static int[] getMinIntState(){
      return StateImpl.minIntState;
   }

   public static int[] getMaxIntState(){
      return StateImpl.maxIntState;
   }

   public static double[] getMinState(){
      return intStateToState(minIntState);
   }

   public static double[] getMaxState(){
      return intStateToState(maxIntState);
   }

   public StateImpl(StateDescriptorImpl descriptor){
      super(descriptor.getPeriod());
      this.initialIntState = Arrays.copyOf(descriptor.getInitialIntState(), descriptor.getInitialIntState().length);
   }

   public int[] getInitialIntState(){
      return this.initialIntState;
   }
   
   public double[] getInitialState(){
      return intStateToState(this.initialIntState);
   }

   /*@Override
   protected void buildActionList(Function<State, ArrayList<Action>> buildActionList,
                                  Function<State, Action> idempotentAction){
      this.noAction = idempotentAction.apply(this);
      this.feasibleActions = buildActionList.apply(this);
   }*/

   @Override
   public boolean equals(Object state){
      if(state instanceof StateImpl)
         return this.period == ((StateImpl)state).period && 
                Arrays.equals(this.initialIntState, ((StateImpl)state).initialIntState);
      else return false;
   }

   @Override
   public int hashCode(){
      String hash = "";
      hash = (hash + period) + Arrays.toString(initialIntState);
      return hash.hashCode();
   }

   @Override
   public String toString(){
      return "Period: "+this.period+"\tState:"+Arrays.toString(intStateToState(this.initialIntState));
   }
}

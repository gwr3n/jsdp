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

package jsdp.sdp.impl.unidimensional;

import java.util.ArrayList;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.State;

/**
 * A concrete implementation of {@code State}.
 * 
 * @author Roberto Rossi
 *
 */
public class StateImpl extends State {

   private int initialIntState;

   private static double stepSize;
   private static int minIntState;
   private static int maxIntState;

   /**
    * Initializes the state space boundaries. Note that {@code stepSize} must be one if 
    * {@code DiscreteDistributionInt} are used.
    * 
    * @param stepSize the discretization step used to encode the state space
    * @param minIntState the minimum integer value used to encode a state
    * @param maxIntState the maximum integer value used to encode a state
    */
   public static void setStateBoundaries(double stepSize, int minIntState, int maxIntState){
      StateImpl.stepSize = stepSize;
      StateImpl.minIntState = minIntState;
      StateImpl.maxIntState = maxIntState;
   }

   public static double getStepSize(){
      return StateImpl.stepSize;
   }

   public static double intStateToState(int intState){
      return intState*stepSize;
   }

   public static int stateToIntState(double state){
      return (int) Math.max(Math.min(Math.round(state/stepSize), StateImpl.maxIntState), StateImpl.minIntState);
   }

   public static int getMinIntState(){
      return StateImpl.minIntState;
   }

   public static int getMaxIntState(){
      return StateImpl.maxIntState;
   }

   public static double getMinState(){
      return intStateToState(minIntState);
   }

   public static double getMaxState(){
      return intStateToState(maxIntState);
   }

   public StateImpl(StateDescriptorImpl descriptor,
                    Function<State, ArrayList<Action>> buildActionList,
                    Function<State, Action> idempotentAction){
      super(descriptor.getPeriod());
      this.initialIntState = descriptor.getInitialIntState();
      this.buildActionList(buildActionList, idempotentAction);
   }

   public int getInitialIntState(){
      return this.initialIntState;
   }
   
   public double getInitialState(){
      return intStateToState(this.initialIntState);
   }

   @Override
   protected void buildActionList(){
      throw new NullPointerException("Method not implemented");
   }
   
   protected void buildActionList(Function<State, ArrayList<Action>> buildActionList,
                                  Function<State, Action> idempotentAction){
      this.noAction = idempotentAction.apply(this);
      this.feasibleActions = buildActionList.apply(this);
   }

   @Override
   public boolean equals(Object state){
      if(state instanceof StateImpl)
         return this.period == ((StateImpl)state).period && this.initialIntState == ((StateImpl)state).initialIntState;
      else return false;
   }

   @Override
   public int hashCode(){
      String hash = "";
      hash = (hash + period) + initialIntState;
      return hash.hashCode();
   }

   @Override
   public String toString(){
      return "Period: "+this.period+"\tInventory:"+intStateToState(this.initialIntState);
   }
}

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

import java.util.ArrayList;

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class sS_State extends State {

   private static final long serialVersionUID = 1L;
   
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
      sS_State.stepSize = stepSize;
      sS_State.minIntState = minIntState;
      sS_State.maxIntState = maxIntState;
   }

   public static double getStepSize(){
      return sS_State.stepSize;
   }

   public static double stateToInventory(int state){
      return state*stepSize;
   }

   public static int inventoryToState(double inventory){
      return (int) Math.max(Math.min(Math.round(inventory/stepSize), sS_State.maxIntState), sS_State.minIntState);
   }

   public static int getMinIntState(){
      return sS_State.minIntState;
   }

   public static int getMaxIntState(){
      return sS_State.maxIntState;
   }

   public static double getMinInventory(){
      return stateToInventory(minIntState);
   }

   public static double getMaxInventory(){
      return stateToInventory(maxIntState);
   }

   public sS_State(sS_StateDescriptor descriptor){
      super(descriptor.getPeriod());
      this.initialIntState = descriptor.getInitialIntState();
   }

   public int getInitialIntState(){
      return this.initialIntState;
   }

   @Override
   public ArrayList<Action> getFeasibleActions() {
      ArrayList<Action> feasibleActions = new ArrayList<Action>();
      for(int i = this.initialIntState; i <= sS_State.maxIntState; i++){
         feasibleActions.add(new sS_Action(this, i - this.initialIntState));
      }
      return feasibleActions;
   }
   
   @Override
   public Action getNoAction(){
      return  new sS_Action(this, 0);
   }

   @Override
   public boolean equals(Object state){
      if(state instanceof sS_State)
         return this.period == ((sS_State)state).period && this.initialIntState == ((sS_State)state).initialIntState;
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
      return "Period: "+this.period+"\tInventory:"+stateToInventory(this.initialIntState);
   }
}

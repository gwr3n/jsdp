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

package jsdp.app.routing.deterministic;

import java.util.ArrayList;
import java.util.Arrays;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;

public class BR_TransitionProbability extends TransitionProbability {

   private double[][][] machineLocation;
   private int[][] fuelConsumption;
   BR_StateSpace[] stateSpace;
   
   public BR_TransitionProbability(double[][][] machineLocation, int[][] fuelConsumption, BR_StateSpace[] stateSpace){
      this.machineLocation = machineLocation;
      this.fuelConsumption = fuelConsumption;
      this.stateSpace = stateSpace;
   }
   
   @Override
   public double getTransitionProbability(State initialState, Action action, State finalState) {
      double probability = 1;
      return probability;
   }

   @Override
   public ArrayList<State> generateFinalStates(State initialState, Action action) {      
      int bowserTankLevel = ((BR_State) initialState).getBowserTankLevel() +
                            ((BR_Action) action).getBowserRefuelQty() - 
                            Arrays.stream(((BR_Action)action).getMachineRefuelQty()).sum();
      int bowserLocation = ((BR_Action) action).getBowserNewLocation();
      
      int machineTankLevel[] = Arrays.stream(((BR_State) initialState).getMachineTankLevel()).map(i -> Math.max(i, 0)).toArray();
      for(int i = 0; i < machineTankLevel.length; i++){
         machineTankLevel[i] += ((BR_Action) action).getMachineRefuelQty()[i] - this.fuelConsumption[i][initialState.getPeriod()];
      }
      
      int[] machineLocations = new int[((BR_State) initialState).getMachineLocation().length];
      double[][] locationProbabilityMatrix = this.machineLocation[Math.min(initialState.getPeriod()+1, this.machineLocation.length-1)];
      for(int machine = 0; machine < machineLocations.length; machine++){
         for(int i = 0; i < locationProbabilityMatrix[machine].length; i++){
            if(locationProbabilityMatrix[machine][i] == 1){
               machineLocations[machine] = i;
            }
         }
      }
      
      BR_StateDescriptor descriptor = new BR_StateDescriptor(initialState.getPeriod() + 1, 
            bowserTankLevel,
            bowserLocation,
            machineTankLevel,
            machineLocations);
      
      ArrayList<State> finalStates = new ArrayList<State>();
      finalStates.add(this.stateSpace[initialState.getPeriod() + 1].getState(descriptor));
      
      return finalStates;
   }

   @Override
   public ArrayList<State> getFinalStates(State initialState, Action action) {
      throw new NullPointerException("Method not implemented");
   }

}

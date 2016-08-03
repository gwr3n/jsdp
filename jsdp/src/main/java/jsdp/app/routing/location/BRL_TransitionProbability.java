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

package jsdp.app.routing.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.stream.Collectors;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;
import jsdp.sdp.impl.univariate.SamplingScheme;

public class BRL_TransitionProbability extends TransitionProbability {

   private double[][][] machineLocationProbability;
   private int[][] fuelConsumption;
   BRL_StateSpace[] stateSpace;
   
   private SamplingScheme samplingScheme;
   private int sampleSize;
   
   public BRL_TransitionProbability(double[][][] machineLocationProbability, 
                                    int[][] fuelConsumption, 
                                    BRL_StateSpace[] stateSpace,
                                    SamplingScheme samplingScheme,
                                    int sampleSize){
      this.machineLocationProbability = machineLocationProbability;
      this.fuelConsumption = fuelConsumption;
      this.stateSpace = stateSpace;
      
      if(samplingScheme == SamplingScheme.NONE || samplingScheme == SamplingScheme.SIMPLE_RANDOM_SAMPLING)
         this.samplingScheme = samplingScheme;
      else
         throw new NullPointerException("Unsupported sampling scheme: "+samplingScheme);
      
      if(sampleSize > 0)
         this.sampleSize = sampleSize;
      else
         throw new NullPointerException("Sample size must be positive.");
   }
   
   @Override
   public double getTransitionProbability(State initialState, Action action, State finalState) {
      double probability = 1;
      int[] machineLocations = ((BRL_State) finalState).getMachineLocation();
      for(int i = 0; i < machineLocations.length; i++){
         probability *= this.machineLocationProbability[finalState.getPeriod()][i][machineLocations[i]];
      }
      return probability;
   }

   @Override
   public ArrayList<State> generateFinalStates(State initialState, Action action) {      
      int bowserTankLevel = ((BRL_State) initialState).getBowserTankLevel() +
                            ((BRL_Action) action).getBowserRefuelQty() - 
                            Arrays.stream(((BRL_Action)action).getMachineRefuelQty()).sum();
      int bowserLocation = ((BRL_Action) action).getBowserNewLocation();
      
      int machineTankLevel[] = Arrays.stream(((BRL_State) initialState).getMachineTankLevel()).map(i -> Math.max(i, 0)).toArray();
      for(int i = 0; i < machineTankLevel.length; i++){
         machineTankLevel[i] += ((BRL_Action) action).getMachineRefuelQty()[i] - this.fuelConsumption[i][initialState.getPeriod()];
      }
      
      ArrayList<int[]> machineLocationsArray = new ArrayList<int[]>();
      
      int[] machineLocations = new int[((BRL_State) initialState).getMachineLocation().length];
      generateLocations(machineLocations, 0, this.machineLocationProbability[initialState.getPeriod()+1], machineLocationsArray);
      
      ArrayList<State> finalStates = machineLocationsArray.parallelStream().map(array ->
                  this.stateSpace[initialState.getPeriod() + 1].getState(
                        new BRL_StateDescriptor(initialState.getPeriod() + 1, 
                                                bowserTankLevel,
                                                bowserLocation,
                                                machineTankLevel,
                                                array)
                        )
               ).collect(Collectors.toCollection(ArrayList::new));
            
      
      if(this.samplingScheme == SamplingScheme.NONE)
         return finalStates;
      else{
         Random rnd = new Random(12345);
         Collections.shuffle(finalStates, rnd);
         return new ArrayList<State>(finalStates.subList(0, this.sampleSize));
      }
   }
   
   private static void generateLocations(int[] machineLocations, 
                                         int machine, 
                                         double[][] locationProbabilityMatrix, 
                                         ArrayList<int[]> machineLocationsArray){
      if(machine == locationProbabilityMatrix.length - 1){
         for(int i = 0; i < locationProbabilityMatrix[machine].length; i++){
            if(locationProbabilityMatrix[machine][i] > 0){
               machineLocations[machine] = i;
               machineLocationsArray.add(Arrays.copyOf(machineLocations, machineLocations.length));
            }
         }
      }else{
         for(int i = 0; i < locationProbabilityMatrix[machine].length; i++){
            if(locationProbabilityMatrix[machine][i] > 0){
               machineLocations[machine] = i;
               generateLocations(Arrays.copyOf(machineLocations, machineLocations.length), machine + 1, locationProbabilityMatrix, machineLocationsArray);
            }
         }
      }
   }

   @Override
   public ArrayList<State> getFinalStates(State initialState, Action action) {
      throw new NullPointerException("Method not implemented");
   }

}

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

package jsdp.app.routing.fuel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import edu.emory.mathcs.backport.java.util.Collections;
import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;
import jsdp.sdp.impl.univariate.SamplingScheme;
import umontreal.ssj.probdist.DiscreteDistribution;

public class BRF_TransitionProbability extends TransitionProbability {

   private double[][][] machineLocation;
   private DiscreteDistribution[][] fuelConsumption;
   BRF_StateSpace[] stateSpace;
   
   private SamplingScheme samplingScheme;
   private int sampleSize;
   
   public BRF_TransitionProbability(double[][][] machineLocation, 
                                    DiscreteDistribution[][] fuelConsumption, 
                                    BRF_StateSpace[] stateSpace,
                                    SamplingScheme samplingScheme,
                                    int sampleSize){
      this.machineLocation = machineLocation;
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
      
      int initialMachineTankLevel[] = ((BRF_State) initialState).getMachineTankLevel();
      int finalMachineTankLevel[] = ((BRF_State) finalState).getMachineTankLevel();
      
      for(int i = 0; i < initialMachineTankLevel.length; i++){
         int fuelConsumed = initialMachineTankLevel[i] + ((BRF_Action) action).getMachineRefuelQty()[i] - finalMachineTankLevel[i];
         DiscreteDistribution dist = fuelConsumption[i][initialState.getPeriod()];
         probability *= dist.cdf(fuelConsumed) - dist.cdf(fuelConsumed - 1);
      }
      
      return probability;
   }

   public int[][] getFuelConsumptionScenarios(int period){
      int machines = fuelConsumption.length;
      int numberOfScenarios = 1;
      for(int i = 0; i < machines; i++){
         numberOfScenarios *= fuelConsumption[i][period].getN();
      }
      
      int[][] scenarios = new int[numberOfScenarios][machines];
      int residuals = numberOfScenarios;
      for(int j = 0; j < machines; j++){
         residuals /= fuelConsumption[j][period].getN();
            for(int i = 0; i < scenarios.length; i++){
               scenarios[i][j] = (int) fuelConsumption[j][period].getValue(i/residuals % fuelConsumption[j][period].getN());
            }
      }
 
      return scenarios;
   }
   
   @Override
   public ArrayList<State> generateFinalStates(State initialState, Action action) {   
      int bowserTankLevel = ((BRF_State) initialState).getBowserTankLevel() +
                            ((BRF_Action) action).getBowserRefuelQty() - 
                            Arrays.stream(((BRF_Action)action).getMachineRefuelQty()).sum();
      int bowserLocation = ((BRF_Action) action).getBowserNewLocation();
      
      ArrayList<int[]> machineTankLevelArray = new ArrayList<int[]>();
      
      int[][] scenarios = getFuelConsumptionScenarios(initialState.getPeriod());
      for(int s = 0; s < scenarios.length; s++){
         int machineTankLevel[] = Arrays.stream(((BRF_State) initialState).getMachineTankLevel()).map(i -> Math.max(i, 0)).toArray();
         for(int i = 0; i < machineTankLevel.length; i++){
            machineTankLevel[i] += ((BRF_Action) action).getMachineRefuelQty()[i] - scenarios[s][i];
         }
         machineTankLevelArray.add(machineTankLevel);
      }
      
      int[] machineLocations = new int[((BRF_State) initialState).getMachineLocation().length];
      double[][] locationProbabilityMatrix = this.machineLocation[initialState.getPeriod()+1];
      for(int machine = 0; machine < machineLocations.length; machine++){
         for(int i = 0; i < locationProbabilityMatrix[machine].length; i++){
            if(locationProbabilityMatrix[machine][i] == 1){
               machineLocations[machine] = i;
            }
         }
      }
      
      ArrayList<State> finalStates = new ArrayList<State>();
      for(int i = 0; i < machineTankLevelArray.size(); i++){
         BRF_StateDescriptor descriptor = new BRF_StateDescriptor(initialState.getPeriod() + 1, 
               bowserTankLevel,
               bowserLocation,
               machineTankLevelArray.get(i),
               machineLocations);
            finalStates.add(this.stateSpace[initialState.getPeriod() + 1].getState(descriptor));
      }
      
      if(this.samplingScheme == SamplingScheme.NONE)
         return finalStates;
      else{
         Random rnd = new Random(12345);
         Collections.shuffle(finalStates, rnd);
         return new ArrayList<State>(finalStates.subList(0, this.sampleSize));
      }
   }

   @Override
   public ArrayList<State> getFinalStates(State initialState, Action action) {
      throw new NullPointerException("Method not implemented");
   }

}

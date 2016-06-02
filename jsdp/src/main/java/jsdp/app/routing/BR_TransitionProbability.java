package jsdp.app.routing;

import java.util.ArrayList;
import java.util.Arrays;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;

public class BR_TransitionProbability extends TransitionProbability {

   private double[][][] transitionProbabilities;
   private int[][] fuelConsumption;
   BR_StateSpace[] stateSpace;
   
   public BR_TransitionProbability(double[][][] transitionProbabilities, int[][] fuelConsumption, BR_StateSpace[] stateSpace){
      this.transitionProbabilities = transitionProbabilities;
      this.fuelConsumption = fuelConsumption;
      this.stateSpace = stateSpace;
   }
   
   @Override
   public double getTransitionProbability(State initialState, Action action, State finalState) {
      double probability = 1;
      int[] machineLocations = ((BR_State) finalState).getMachineLocation();
      for(int i = 0; i < machineLocations.length; i++){
         probability *= this.transitionProbabilities[finalState.getPeriod()][i][machineLocations[i]];
      }
      //if(probability == 0)
         //System.out.println(initialState + " " + action + " " + finalState);
      return probability;
   }

   @Override
   public ArrayList<State> generateFinalStates(State initialState, Action action) {
      // TODO Auto-generated method stub
      
      int bowserTankLevel = ((BR_State) initialState).getBowserTankLevel() +
                            ((BR_Action) action).getBowserRefuelQty() - 
                            Arrays.stream(((BR_Action)action).getMachineRefuelQty()).sum();
      int bowserLocation = ((BR_Action) action).getBowserNewLocation();
      
      int machineTankLevel[] = Arrays.copyOf(((BR_State) initialState).getMachineTankLevel(), ((BR_State) initialState).getMachineTankLevel().length);
      for(int i = 0; i < machineTankLevel.length; i++){
         machineTankLevel[i] += ((BR_Action) action).getMachineRefuelQty()[i] - this.fuelConsumption[i][initialState.getPeriod()];
      }
      
      ArrayList<int[]> machineLocationsArray = new ArrayList<int[]>();
      
      /*if(((BR_State) initialState).getPeriod() == 1 || ((BR_State) initialState).getPeriod() == 2){
      System.out.println(((BR_State) initialState).getPeriod());
      System.out.println(Arrays.deepToString(this.transitionProbabilities[initialState.getPeriod()+1]));
      }*/
      int[] machineLocations = new int[((BR_State) initialState).getMachineLocation().length];
      generateLocations(machineLocations, 0, this.transitionProbabilities[initialState.getPeriod()+1], machineLocationsArray);
      
      /*if(((BR_State) initialState).getPeriod() == 1 || ((BR_State) initialState).getPeriod() == 2){
      System.out.println(((BR_State) initialState).getPeriod());
      System.out.println(Arrays.toString(((BR_State) initialState).getMachineLocation()));
      machineLocationsArray.stream().forEach(a -> System.out.println(Arrays.toString(a)));
      System.out.println();
      System.out.println();
      System.out.println();
      }*/
      
      ArrayList<State> finalStates = new ArrayList<State>();
      for(int i = 0; i < machineLocationsArray.size(); i++){
         BR_StateDescriptor descriptor = new BR_StateDescriptor(initialState.getPeriod() + 1, 
                                                                bowserTankLevel,
                                                                bowserLocation,
                                                                machineTankLevel,
                                                                machineLocationsArray.get(i));
         finalStates.add(this.stateSpace[initialState.getPeriod() + 1].getState(descriptor));
      }
      //if(initialState.getPeriod() == 3)
         //finalStates.stream().forEach(s -> System.out.println(s));
      //if(initialState.getPeriod() == 0)
         //System.out.println();
      return finalStates;
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
      // TODO Auto-generated method stub
      throw new NullPointerException("Method not implemented");
   }

}

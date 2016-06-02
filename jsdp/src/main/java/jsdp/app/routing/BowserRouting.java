package jsdp.app.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.lang3.time.StopWatch;

import jsdp.sdp.Action;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;

public class BowserRouting {
   public static void main(String args[]){
      
      /*******************************************************************
       * Problem parameters
       */
      int T = 5;   //time horizon
      int M = 3;   //machines
      int N = 5;   //nodes
      int[] tankCapacity = {20, 20, 20};
      int[] initialTankLevel ={0, 0, 0};
      int[][] fuelConsumption ={{2, 4, 3, 4, 4},
      {2, 1, 3, 1, 4},
      {4, 3, 3, 4, 2}};
      int[][] connectivity = {
            {1, 1, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {1, 0, 0, 0, 1},
            {0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0}};
      double[][] distance = {{0., 98.3569, 0., 0., 0.},
      {0., 0., 72.6373, 0., 0.},
      {44.2516, 0., 0., 0., 99.4693},
      {0., 0., 87.9929, 0., 0.},
      {0., 0., 0., 78.212, 0.}};
      double[][][] machineLocation = {
      {{0, 0, 0, 0, 1},
      {0, 1, 0, 0, 0},
      {0, 0, 0, 0, 1}},
      
      {{0, 0, 0, 0, 1},   
      {0, 0, 0, 1, 0},
      {0.5, 0.5, 0, 0, 0}},
      
      {{0, 0, 1, 0, 0},
      {0, 0, 1, 0, 0},
      {0, 0, 1, 0, 0}},
      
      {{1, 0, 0, 0, 0},
      {0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0}},
      
      {{0, 0, 0, 0.2, 0.8},
      {0, 0, 0, 1, 0},
      {0, 0, 1, 0, 0}},
      
      {{0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0},
      {0, 0, 1, 0, 0}}};
      
      int fuelStockOutPenaltyCost = 10;
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      int minBowserTankLevel = 0;
      int maxBowserTankLevel = 20;
      int[] minMachineTankLevel = new int[M];
      int[] maxMachineTankLevel = Arrays.copyOf(tankCapacity, tankCapacity.length);
      int networkSize = N;
      
      BR_State.setStateBoundaries(minBowserTankLevel, 
                                  maxBowserTankLevel,
                                  minMachineTankLevel,
                                  maxMachineTankLevel,
                                  networkSize);
      
      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         BR_State state = (BR_State) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(int i = 0; i < N; i++){
            if(connectivity[state.getBowserLocation()][i] == 1){
               final int bowserNewLocation = i;
               if(state.getBowserLocation() == 0){
                  for(int j = 0; j <= BR_State.getMaxBowserTankLevel() - state.getBowserTankLevel(); j++){
                     final int bowserRefuelQty = j;
                     BR_Action.computeMachineRefuelQtys(state, j).stream().forEach(action -> 
                        feasibleActions.add(new BR_Action(state, bowserNewLocation, bowserRefuelQty, action))
                     );
                  }
               }else{
                  final int bowserRefuelQty = 0;
                  BR_Action.computeMachineRefuelQtys(state, 0).stream().forEach(action -> 
                     feasibleActions.add(new BR_Action(state, bowserNewLocation, bowserRefuelQty, action))
                  );
               }
            }
         }
         return feasibleActions;
      };
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         BR_State is = (BR_State)initialState;
         BR_Action a = (BR_Action)action;
         BR_State fs = (BR_State)finalState;
         double travelCost = distance[is.getBowserLocation()][a.bowserNewLocation];
         double penaltyCost = Arrays.stream(fs.getMachineTankLevel()).map(l -> Math.max(-l, 0)).sum()*fuelStockOutPenaltyCost;
         return travelCost + penaltyCost;
      };
      
      BR_ForwardRecursion recursion = new BR_ForwardRecursion(T, machineLocation, fuelConsumption, immediateValueFunction, buildActionList);
      
      int period = 0;
      int bowserInitialTankLevel = 0;
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = initialTankLevel;
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BR_StateDescriptor initialState = new BR_StateDescriptor(period, 
                                                               bowserInitialTankLevel, 
                                                               bowserInitialLocation,
                                                               machinesInitialTankLevel,
                                                               machinesInitialLocation);

      StopWatch timer = new StopWatch();
      timer.start();
      recursion.runForwardRecursion(((BR_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
      timer.stop();
      double ETC = recursion.getExpectedCost(initialState);
      System.out.println("Expected total cost: "+ETC);
      System.out.println("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
      System.out.println("Time elapsed: "+timer);
      System.out.println();
      
      /*for(int t = 1; t < N; t++){
         bowserInitialLocation = ((BR_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation();
         bowserInitialTankLevel += ((BR_Action)recursion.getOptimalAction(initialState)).getBowserRefuelQty() - Arrays.stream(((BR_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()).sum();
         machinesInitialLocation = getMachineLocationArray(M, machineLocation[t]);
         for(int i = 0; i < M; i++){
            machinesInitialTankLevel[i] += ((BR_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()[i] - fuelConsumption[i][t-1];
         }
         initialState = new BR_StateDescriptor(period + t, 
               bowserInitialTankLevel, 
               bowserInitialLocation,
               machinesInitialTankLevel,
               machinesInitialLocation);
         System.out.println(initialState.toString());
         System.out.println("Expected total cost: "+recursion.getExpectedCost(initialState));
         System.out.println("Optimal action: "+recursion.getOptimalAction(initialState).toString());
      }*/
   }
   
   public static int[] getMachineLocationArray(int M, double[][] machineLocationMatrix){
      int[] machineLocationArray = new int[M];
      for(int i = 0; i < machineLocationMatrix.length; i++){
         for(int j = 0; j < machineLocationMatrix[i].length; j++){
            if(machineLocationMatrix[i][j] == 1) 
               machineLocationArray[i] = j;
            else if(machineLocationMatrix[i][j] > 0 && machineLocationMatrix[i][j] < 1)
               throw new NullPointerException("Initial location must be certain");
         }
      }
      return machineLocationArray;
   }
}

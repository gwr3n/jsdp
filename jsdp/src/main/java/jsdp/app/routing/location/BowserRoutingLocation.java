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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.lang3.time.StopWatch;

import com.sun.management.OperatingSystemMXBean;

import jsdp.sdp.Action;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;

/**
 * Stochastic Dynamic Bowser Routing Problem under Asset Location Uncertainty
 * 
 * @author Roberto Rossi
 *
 */

public class BowserRoutingLocation {
   
   static int T, M, N;
   static int[] tankCapacity;
   static int[] initialTankLevel;
   static int[][] fuelConsumption;
   static int[][] connectivity;
   static double[][] distance;
   static double[][][] machineLocation;
   static int fuelStockOutPenaltyCost;
   
   static enum InstanceType {
      TINY,
      SMALL,
      LARGE
   }
   
   static InstanceType type;
   
   static void tinyInstance(){
      type = InstanceType.TINY;
      /*******************************************************************
       * Problem parameters
       */
      T = 3;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{0, 0, 0};
      fuelConsumption = new int[][]{{1, 1, 1},
                                    {1, 1, 1},
                                    {1, 1, 1}};
                                    
      connectivity = new int[][]{
            {1, 1, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {1, 0, 0, 0, 1},
            {0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0}};
      distance = new double[][]{
      {0., 98.3569, 0., 0., 0.},
      {0., 0., 72.6373, 0., 0.},
      {44.2516, 0., 0., 0., 99.4693},
      {0., 0., 87.9929, 0., 0.},
      {0., 0., 0., 78.212, 0.}};
      machineLocation = new double[][][]{
           {{0, 0, 0, 1, 0},
            {0, 1, 0, 0, 0},
            {0, 0, 0, 0, 1}},
            
            {{0, 0.5, 0, 0, 0.5},   
            {0, 0.5, 0, 0.5, 0},
            {0.5, 0, 0, 0, 0.5}},
            
            {{0, 0.5, 0, 0, 0.5},
            {0, 0, 0.5, 0.5, 0},
            {0, 0.5, 0, 0, 0.5}},
            
            {{0, 0.5, 0, 0, 0.5},
            {0, 0.5, 0, 0.5, 0},
            {0.5, 0, 0.5, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   static void smallInstance(){
      type = InstanceType.SMALL;
      /*******************************************************************
       * Problem parameters
       */
      T = 5;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{0, 0, 0};
      fuelConsumption = new int[][]{{2, 4, 3, 4, 4},
                                   {2, 1, 3, 1, 4},
                                   {4, 3, 3, 4, 2}};
      connectivity = new int[][]{
            {1, 1, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {1, 0, 0, 0, 1},
            {0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0}};
      distance = new double[][]{
      {0., 98.3569, 0., 0., 0.},
      {0., 0., 72.6373, 0., 0.},
      {44.2516, 0., 0., 0., 99.4693},
      {0., 0., 87.9929, 0., 0.},
      {0., 0., 0., 78.212, 0.}};
      machineLocation = new double[][][]{
      {{0, 0, 0, 0, 1},
      {0, 1, 0, 0, 0},
      {0, 0, 0, 0, 1}},
      
      {{0, 0, 0, 0.5, 0.5},   
      {0, 0, 0, 1, 0},
      {0, 1, 0, 0, 0}},
      
      {{0, 0, 1, 0, 0},
      {0, 0, 0.5, 0, 0.5},
      {0.5, 0, 0.5, 0, 0}},
      
      {{0, 0.5, 0.5, 0, 0},
      {0, 0, 0, 0, 1},
      {0.5, 0, 0, 0.5, 0}},
      
      {{0, 0, 0, 0, 1},
      {0, 0, 0.5, 0.5, 0},
      {0, 0, 0.5, 0, 0.5}},
      
      {{0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0},
      {0, 0, 1, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   static void largeInstance(){
      type = InstanceType.LARGE;
      /*******************************************************************
       * Problem parameters
       */
      T = 10;   //time horizon
      M = 3;    //machines
      N = 10;   //nodes
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{10, 10, 10};
      fuelConsumption = new int[][]{{4, 4, 2, 1, 3, 1, 4, 4, 3, 3},
                                    {4, 2, 3, 4, 3, 1, 4, 2, 4, 4},
                                    {2, 4, 1, 2, 2, 4, 1, 1, 2, 2}};
      connectivity = new int[][]{
               {1, 1, 0, 0, 1, 0, 0, 0, 0, 0},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 1, 0, 1, 1, 0, 0, 1, 0, 1},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
               {0, 0, 0, 0, 0, 1, 0, 0, 1, 0},
               {0, 0, 1, 0, 1, 0, 1, 0, 0, 0},
               {0, 0, 0, 0, 0, 1, 0, 1, 0, 0},
               {0, 1, 0, 0, 0, 0, 0, 0, 1, 1},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0}};
      distance = new double[][]{
            {0., 96., 0., 0., 107., 0., 0., 0., 0., 0.},
            {121.0, 0., 0., 0., 0., 0., 0., 0., 0., 0.},
            {0., 92., 0., 103., 103., 0., 0., 92., 0., 77.},
            {0., 90., 0., 0., 0., 0., 0., 0., 0., 91.},
            {0., 0., 0., 0., 0., 102., 0., 0., 126., 0.},
            {0., 0., 72., 0., 139., 0., 89., 0., 0., 0.},
            {0., 0., 0., 0., 0., 80., 0., 83., 0., 0.},
            {0., 119.8, 0., 0., 0., 0., 0., 0., 90., 91.},
            {83., 0., 0., 0., 0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 79., 0., 0., 0., 0., 0.}};
      machineLocation = new double[][][]{
               {{0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0}},
               {{1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
               {0, 0, 0, 0, 0, 0, 1, 0, 0, 0}},
               {{1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0}},
               {{0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
               {0, 0, 0, 0, 0, 1, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   public static void main(String args[]){
      /*******************************************************************
       * Problem parameters
       */
      tinyInstance();
      //smallInstance();
      //largeInstance();
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      int minBowserTankLevel = 0;
      int maxBowserTankLevel = 10;
      int[] minMachineTankLevel = Arrays.stream(fuelConsumption).mapToInt(m -> -Arrays.stream(m).max().getAsInt()).toArray();
      int[] maxMachineTankLevel = Arrays.copyOf(tankCapacity, tankCapacity.length);
      int networkSize = N;
      
      BRL_State.setStateBoundaries(minBowserTankLevel, 
                                  maxBowserTankLevel,
                                  minMachineTankLevel,
                                  maxMachineTankLevel,
                                  networkSize);
      
      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         BRL_State state = (BRL_State) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         for(int i = 0; i < N; i++){
            if(connectivity[state.getBowserLocation()][i] == 1){
               final int bowserNewLocation = i;
               if(state.getBowserLocation() == 0){
                  for(int j = 0; j <= BRL_State.getMaxBowserTankLevel() - state.getBowserTankLevel(); j+=1){
                     final int bowserRefuelQty = j;
                     BRL_Action.computeMachineRefuelQtys(state, j).parallelStream().forEach(action -> 
                        feasibleActions.add(new BRL_Action(state, bowserNewLocation, bowserRefuelQty, action))
                     );
                  }
               }else{
                  final int bowserRefuelQty = 0;
                  BRL_Action.computeMachineRefuelQtys(state, 0).parallelStream().forEach(action -> 
                     feasibleActions.add(new BRL_Action(state, bowserNewLocation, bowserRefuelQty, action))
                  );
               }
            }
         }
         return feasibleActions;
      };
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         BRL_State is = (BRL_State)initialState;
         BRL_Action a = (BRL_Action)action;
         BRL_State fs = (BRL_State)finalState;
         double travelCost = finalState.getPeriod() >= T ? 0 : distance[is.getBowserLocation()][a.bowserNewLocation];
         double penaltyCost = Arrays.stream(fs.getMachineTankLevel()).map(l -> Math.max(-l, 0)).sum()*fuelStockOutPenaltyCost;
         return travelCost + penaltyCost;
      };
      
      /**
       * THashMap
       */
      int stateSpaceSizeLowerBound = 10000000;
      float loadFactor = 0.8F;
      double discountFactor = 1.0;
      BRL_ForwardRecursion recursion = new BRL_ForwardRecursion(T, 
                                                              machineLocation, 
                                                              fuelConsumption, 
                                                              immediateValueFunction, 
                                                              buildActionList,
                                                              discountFactor,
                                                              stateSpaceSizeLowerBound,
                                                              loadFactor);
      
      int period = 0;
      int bowserInitialTankLevel = 0;
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BRL_StateDescriptor initialState = new BRL_StateDescriptor(period, 
                                                               bowserInitialTankLevel, 
                                                               bowserInitialLocation,
                                                               machinesInitialTankLevel,
                                                               machinesInitialLocation);

      StopWatch timer = new StopWatch();
      OperatingSystemMXBean osMBean;
      try {
         osMBean = ManagementFactory.newPlatformMXBeanProxy(
               ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
         long nanoBefore = System.nanoTime();
         long cpuBefore = osMBean.getProcessCpuTime();
         
         timer.start();
         recursion.runForwardRecursion(((BRL_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
         timer.stop();
         
         long cpuAfter = osMBean.getProcessCpuTime();
         long nanoAfter = System.nanoTime();
         
         long percent;
         if (nanoAfter > nanoBefore)
          percent = ((cpuAfter-cpuBefore)*100L)/
            (nanoAfter-nanoBefore);
         else percent = 0;

         System.out.println("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      System.out.println();
      double ETC = recursion.getExpectedCost(initialState);
      System.out.println("Expected total cost: "+ETC);
      System.out.println("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
      System.out.println("Time elapsed: "+timer);
      System.out.println();
      
      
      if(type == InstanceType.TINY){  
         /* This set of realisations is valid for tinyInstance */
         machineLocation = new double[][][]{
            {{0, 0, 0, 1, 0},
             {0, 1, 0, 0, 0},
             {0, 0, 0, 0, 1}},
             
             {{0, 1, 0, 0, 0},   
             {0, 1, 0, 0, 0},
             {1, 0, 0, 0, 0}},
             
             {{0, 1, 0, 0, 0},
             {0, 0, 0, 1, 0},
             {0, 1, 0, 0, 0}},
             
             {{0, 0, 0, 0, 1},
             {0, 0, 0, 1, 0},
             {1, 0, 0, 0, 0}}};
         
         for(int t = 1; t < T; t++){
            bowserInitialLocation = ((BRL_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation();
            bowserInitialTankLevel += ((BRL_Action)recursion.getOptimalAction(initialState)).getBowserRefuelQty() - Arrays.stream(((BRL_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()).sum();
            machinesInitialLocation = getMachineLocationArray(M, machineLocation[t]);
            for(int i = 0; i < M; i++){
               machinesInitialTankLevel[i] = Math.max(0, machinesInitialTankLevel[i]) + ((BRL_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()[i] - fuelConsumption[i][t-1];
            }
            initialState = new BRL_StateDescriptor(period + t, 
                  bowserInitialTankLevel, 
                  bowserInitialLocation,
                  machinesInitialTankLevel,
                  machinesInitialLocation);
            System.out.println(initialState.toString());
            System.out.println("Expected total cost: "+recursion.getExpectedCost(initialState));
            System.out.println("Optimal action: "+recursion.getOptimalAction(initialState).toString());
         }
      }
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

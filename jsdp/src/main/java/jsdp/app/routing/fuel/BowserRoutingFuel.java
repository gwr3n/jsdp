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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;

import com.sun.management.OperatingSystemMXBean;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;
import jsdp.sdp.impl.univariate.SamplingScheme;
import jsdp.utilities.probdist.DiscreteDistributionFactory;
import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.PoissonDist;

/**
 * Stochastic Dynamic Bowser Routing Problem under Asset Fuel Consumption Uncertainty
 * 
 * Run with VM arguments -d64 -Xms512m -Xmx4g
 * 
 * @author Roberto Rossi
 *
 */

public class BowserRoutingFuel {
   
   static int T, M, N;
   static int bowserInitialTankLevel;
   static int maxBowserTankLevel;
   static int minRefuelingQty;
   static int[] tankCapacity;
   static int[] initialTankLevel;
   static DiscreteDistribution[][] fuelConsumptionProb;
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
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 10;
      minRefuelingQty = 1;
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{2, 0, 2};
      
      final int minFuelConsumption = 0;
      final int maxFuelConsumption = 4;
      int[][] fuelConsumption = new int[][]{{1, 1, 1},
                                            {1, 1, 1},
                                            {1, 1, 1}};
                                         
      fuelConsumptionProb = new DiscreteDistribution[fuelConsumption.length][fuelConsumption[0].length];
      for(int i = 0; i < fuelConsumption.length; i++){
         final int[] array = fuelConsumption[i];
         fuelConsumptionProb[i] = Arrays.stream(array)
                                        .mapToObj(k -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                                          new PoissonDist(k), minFuelConsumption, maxFuelConsumption, 1.0))
                                        .toArray(DiscreteDistribution[]::new);
      }    
                                    
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
            
            {{0, 0, 0, 0, 1},   
            {0, 1, 0, 0, 0},
            {1, 0, 0, 0, 0}},
            
            {{0, 0, 0, 0, 1},
            {0, 0, 0, 1, 0},
            {0, 1, 0, 0, 0}},
            
            {{0, 0, 0, 0, 1},
            {0, 0, 0, 1, 0},
            {0, 0, 1, 0, 0}}};
      
      fuelStockOutPenaltyCost = 100;
   }
   
   static void smallInstance(){
      type = InstanceType.SMALL;
      /*******************************************************************
       * Problem parameters
       */
      T = 5;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 10;
      minRefuelingQty = 5;
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{0, 0, 0};
      
      final int minFuelConsumption = 0;
      final int maxFuelConsumption = 10;
      int[][] fuelConsumption = new int[][]{{2, 4, 3, 4, 4},
                                            {2, 1, 3, 1, 4},
                                            {4, 3, 3, 4, 2}};
      fuelConsumptionProb = new DiscreteDistribution[fuelConsumption.length][fuelConsumption[0].length];
      for(int i = 0; i < fuelConsumption.length; i++){
         final int[] array = fuelConsumption[i];
         fuelConsumptionProb[i] = Arrays.stream(array)
                                        .mapToObj(k -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                                  new PoissonDist(k), minFuelConsumption, maxFuelConsumption, 1.0))
                                        .toArray(DiscreteDistribution[]::new);
      }                             
                                   
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
      
      {{0, 0, 0, 0, 1},   
      {0, 0, 0, 1, 0},
      {0, 1, 0, 0, 0}},
      
      {{0, 0, 1, 0, 0},
      {0, 0, 1, 0, 0},
      {0, 0, 1, 0, 0}},
      
      {{0, 1, 0, 0, 0},
      {0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0}},
      
      {{0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0},
      {0, 0, 1, 0, 0}},
      
      {{0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0},
      {0, 0, 1, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   static void mediumInstance(){
      type = InstanceType.LARGE;
      /*******************************************************************
       * Problem parameters
       */
      T = 5;   //time horizon
      M = 3;    //machines
      N = 10;   //nodes
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 20;
      minRefuelingQty = 1;
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{10, 10, 10};
      final int minFuelConsumption = 0;
      final int maxFuelConsumption = 5;
      int[][] fuelConsumption = new int[][]{{4, 4, 2, 1, 3},
                                            {4, 2, 3, 4, 3},
                                            {2, 4, 1, 2, 2}};
      fuelConsumptionProb = new DiscreteDistribution[fuelConsumption.length][fuelConsumption[0].length];
      for(int i = 0; i < fuelConsumption.length; i++){
         final int[] array = fuelConsumption[i];
         fuelConsumptionProb[i] = Arrays.stream(array)
                                        .mapToObj(k -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                                          new PoissonDist(k), minFuelConsumption, maxFuelConsumption, 1.0))
                                        .toArray(DiscreteDistribution[]::new);
      }    
                     
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
               {{0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}}};
      
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
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 300;
      minRefuelingQty = 5;
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{10, 10, 10};
      final int minFuelConsumption = 0;
      final int maxFuelConsumption = 5;
      int[][] fuelConsumption = new int[][]{{4, 4, 2, 1, 3, 1, 4, 4, 3, 3},
                                            {4, 2, 3, 4, 3, 1, 4, 2, 4, 4},
                                            {2, 4, 1, 2, 2, 4, 1, 1, 2, 2}};
      fuelConsumptionProb = new DiscreteDistribution[fuelConsumption.length][fuelConsumption[0].length];
      for(int i = 0; i < fuelConsumption.length; i++){
         final int[] array = fuelConsumption[i];
         fuelConsumptionProb[i] = Arrays.stream(array)
                                        .mapToObj(k -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                                          new PoissonDist(k), minFuelConsumption, maxFuelConsumption, 1.0))
                                        .toArray(DiscreteDistribution[]::new);
      }    
                     
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
      //tinyInstance();
      //smallInstance();
      mediumInstance();
      //largeInstance();
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      int minBowserTankLevel = 0;
      int[] minMachineTankLevel = new int[M];
      int[] maxMachineTankLevel = Arrays.copyOf(tankCapacity, tankCapacity.length);
      int networkSize = N;
      
      BRF_State.setStateBoundaries(minBowserTankLevel, 
                                  maxBowserTankLevel,
                                  minMachineTankLevel,
                                  maxMachineTankLevel,
                                  networkSize);
      
      // Actions
      
      Function<State, ArrayList<Action>> buildActionList = s -> {
         BRF_State state = (BRF_State) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>(); 
         for(int i = 0; i < N; i++){
            if(connectivity[state.getBowserLocation()][i] == 1){
               final int bowserNewLocation = i;
               if(state.getBowserLocation() == 0){
                  for(int j = 0; j <= BRF_State.getMaxBowserTankLevel() - state.getBowserTankLevel(); j+= minRefuelingQty){
                     final int bowserRefuelQty = j;
                     feasibleActions.addAll(
                           BRF_Action.computeMachineRefuelQtys(state, j, minRefuelingQty).parallelStream().map(action -> 
                              new BRF_Action(state, bowserNewLocation, bowserRefuelQty, action)).collect(Collectors.toList())
                           );
                  }
               }else{
                  final int bowserRefuelQty = 0;
                  feasibleActions.addAll(
                        BRF_Action.computeMachineRefuelQtys(state, 0, minRefuelingQty).parallelStream().map(action -> 
                           new BRF_Action(state, bowserNewLocation, bowserRefuelQty, action)).collect(Collectors.toList())
                        );
               }
            }
         }
         return feasibleActions;
      };
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         BRF_State is = (BRF_State)initialState;
         BRF_Action a = (BRF_Action)action;
         BRF_State fs = (BRF_State)finalState;
         double travelCost = finalState.getPeriod() >= T ? 0 : distance[is.getBowserLocation()][a.bowserNewLocation];
         double penaltyCost = Arrays.stream(fs.getMachineTankLevel()).map(l -> Math.max(-l, 0)).sum()*fuelStockOutPenaltyCost;
         return travelCost + penaltyCost;
      };
      
      /**
       * Sampling scheme
       */
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int sampleSize = 2;                                     // This is the sample size used to determine a state value function
      
      /**
       * THashMap
       */
      int stateSpaceSizeLowerBound = 10000000;
      float loadFactor = 0.8F;
      
      double discountFactor = 1.0;
      BRF_ForwardRecursion recursion = new BRF_ForwardRecursion(T, 
                                                              machineLocation, 
                                                              fuelConsumptionProb, 
                                                              immediateValueFunction, 
                                                              buildActionList,
                                                              discountFactor,
                                                              HashType.HASHTABLE,
                                                              stateSpaceSizeLowerBound,
                                                              loadFactor,
                                                              samplingScheme,
                                                              sampleSize);
      
      int period = 0;
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BRF_StateDescriptor initialState = new BRF_StateDescriptor(period, 
                                                                 bowserInitialTankLevel, 
                                                                 bowserInitialLocation,
                                                                 machinesInitialTankLevel,
                                                                 machinesInitialLocation);

      recursion.runForwardRecursionMonitoring(((BRF_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
      long percent = recursion.getMonitoringInterfaceForward().getPercentCPU();
      System.out.println("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      System.out.println();
      double ETC = recursion.getExpectedCost(initialState);
      System.out.println("Expected total cost: "+ETC);
      System.out.println("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
      System.out.println("Time elapsed: "+recursion.getMonitoringInterfaceForward().getTime());
      System.out.println();
      
      
      if(type == InstanceType.TINY && samplingScheme == SamplingScheme.NONE){  
         /* This set of realisations is valid for tinyInstance */
         int[][] fuelConsumption = new int[][]{{1, 2, 1},
                                               {2, 1, 2},
                                               {1, 2, 1}};
         
         for(int t = 1; t < T; t++){
            bowserInitialLocation = ((BRF_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation();
            bowserInitialTankLevel += ((BRF_Action)recursion.getOptimalAction(initialState)).getBowserRefuelQty() - Arrays.stream(((BRF_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()).sum();
            machinesInitialLocation = getMachineLocationArray(M, machineLocation[t]);
            for(int i = 0; i < M; i++){
               machinesInitialTankLevel[i] = Math.max(0, machinesInitialTankLevel[i]) + ((BRF_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()[i] - fuelConsumption[i][t-1];
            }
            initialState = new BRF_StateDescriptor(period + t, 
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

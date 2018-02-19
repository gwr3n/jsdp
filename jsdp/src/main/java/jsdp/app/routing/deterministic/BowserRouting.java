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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;

/**
 * Dynamic Bowser Routing Problem
 * 
 * Run with VM arguments -d64 -Xms512m -Xmx4g
 * 
 * @author Roberto Rossi
 *
 */

public class BowserRouting {
   
   static final Logger logger = LogManager.getLogger(BowserRouting.class.getName());
   
   int T, M, N;
   int bowserInitialTankLevel;
   int maxBowserTankLevel;
   int minRefuelingQty;
   int[] tankCapacity;
   int[] initialTankLevel;
   int[][] fuelConsumption;
   int[][] connectivity;
   double[][] distance;
   double[][][] machineLocation;
   int fuelStockOutPenaltyCost;
   
   BR_ForwardRecursion recursion;
   
   enum InstanceType {
      TINY,
      SMALL,
      MEDIUM,
      LARGE
   }
   
   public BowserRouting(InstanceType type){
      switch(type){
      case TINY:
         this.tinyInstance();
         break;
      case SMALL:
         this.smallInstance();
         break;
      case MEDIUM:
         this.mediumInstance();
         break;
      case LARGE:
         this.largeInstance();
         break;
      default:
         throw new NullPointerException("Instance type undefined");
      }
   }
   
   public BowserRouting(int T, int M, int N,
                        int bowserInitialTankLevel,
                        int maxBowserTankLevel,
                        int minRefuelingQty,
                        int[] tankCapacity,
                        int[] initialTankLevel,
                        int[][] fuelConsumption,
                        int[][] connectivity,
                        double[][] distance,
                        double[][][] machineLocation,
                        int fuelStockOutPenaltyCost){
      this.T = T;
      this.M = M;
      this.N = N;
      this.maxBowserTankLevel = maxBowserTankLevel;
      this.minRefuelingQty = minRefuelingQty;
      this.tankCapacity = tankCapacity.clone();
      this.initialTankLevel = initialTankLevel.clone();
      this.fuelConsumption = fuelConsumption.clone();
      this.connectivity = connectivity.clone();
      this.distance = distance.clone();
      this.machineLocation = machineLocation.clone();
      this.fuelStockOutPenaltyCost = fuelStockOutPenaltyCost;
   }

   private void tinyInstance(){
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
      {{0, 0, 0, 0, 1},
      {0, 1, 0, 0, 0},
      {0, 0, 0, 0, 1}},
      
      {{0, 0, 0, 0, 1},   
      {0, 1, 0, 0, 0},
      {1, 0, 0, 0, 0}},
      
      {{0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0},
      {0, 0, 1, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   private void smallInstance(){
      /*******************************************************************
       * Problem parameters
       */
      T = 5;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      bowserInitialTankLevel = 10;
      maxBowserTankLevel = 10;
      minRefuelingQty = 1;
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
      {0, 0, 1, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   private void mediumInstance(){
      /*******************************************************************
       * Problem parameters
       */
      T = 8;   //time horizon
      M = 3;    //machines
      N = 10;   //nodes
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 20;
      minRefuelingQty = 1;
      tankCapacity = new int[]{10, 10, 10};
      initialTankLevel = new int[]{10, 10, 10};
      fuelConsumption = new int[][]{{4, 4, 2, 1, 3, 1, 4, 4},
                                    {4, 2, 3, 4, 3, 1, 4, 2},
                                    {2, 4, 1, 2, 2, 4, 1, 1}};
                     
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
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}}};
      
      fuelStockOutPenaltyCost = 100;
   }
   
   private void largeInstance(){
      /*******************************************************************
       * Problem parameters
       */
      T = 10;   //time horizon
      M = 3;    //machines
      N = 10;   //nodes
      bowserInitialTankLevel = 10;
      maxBowserTankLevel = 300;
      minRefuelingQty = 5;
      tankCapacity = new int[]{20, 20, 20};
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
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}}};
      
      fuelStockOutPenaltyCost = 100;
   }
   
   private BR_ForwardRecursion buildModel(){
      /*******************************************************************
       * Model definition
       */
      
      // State space
      int minBowserTankLevel = 0;
      int[] minMachineTankLevel = Arrays.stream(fuelConsumption).mapToInt(m -> -Arrays.stream(m).max().getAsInt()).toArray();
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
                  for(int j = 0; j <= BR_State.getMaxBowserTankLevel() - state.getBowserTankLevel(); j += minRefuelingQty){
                     final int bowserRefuelQty = j;
                     feasibleActions.addAll(
                           BR_Action.computeMachineRefuelQtys(state, j, minRefuelingQty).parallelStream().map(action ->
                              new BR_Action(state, bowserNewLocation, bowserRefuelQty, action)).collect(Collectors.toList())
                           );
                  }
               }else{
                  final int bowserRefuelQty = 0;
                  feasibleActions.addAll(
                        BR_Action.computeMachineRefuelQtys(state, 0, minRefuelingQty).parallelStream().map(action ->
                           new BR_Action(state, bowserNewLocation, bowserRefuelQty, action)).collect(Collectors.toList())
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
      BR_ForwardRecursion recursion = new BR_ForwardRecursion(T, 
                                                              machineLocation, 
                                                              fuelConsumption, 
                                                              immediateValueFunction, 
                                                              buildActionList,
                                                              discountFactor,
                                                              HashType.THASHMAP,
                                                              stateSpaceSizeLowerBound,
                                                              loadFactor);
      
      return recursion;
   }
   
   public static void main(String args[]){
      BowserRouting bowserRouting = new BowserRouting(InstanceType.SMALL);
      bowserRouting.runInstance();
      bowserRouting.printPolicy();
   }
   
   public void runInstance(){

      recursion = buildModel();
      
      int period = 0;
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BR_StateDescriptor initialState = new BR_StateDescriptor(period, 
                                                               bowserInitialTankLevel, 
                                                               bowserInitialLocation,
                                                               machinesInitialTankLevel,
                                                               machinesInitialLocation);

      recursion.runForwardRecursionMonitoring(((BR_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
      double ETC = recursion.getExpectedCost(initialState);
      long percent = recursion.getMonitoringInterfaceForward().getPercentCPU();
      logger.info("---");
      logger.info("Expected total cost: "+ETC);
      logger.info("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
      logger.info("Time elapsed: "+recursion.getMonitoringInterfaceForward().getTime());
      logger.info("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      logger.info("---");
   }
   
   public String toString(){
      String stats ="";
      
      if(recursion == null)
         return stats;
      
      int period = 0;
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BR_StateDescriptor initialState = new BR_StateDescriptor(period, 
                                                               bowserInitialTankLevel, 
                                                               bowserInitialLocation,
                                                               machinesInitialTankLevel,
                                                               machinesInitialLocation);
      
      double ETC = recursion.getExpectedCost(initialState);
      long time = recursion.getMonitoringInterfaceForward().getTime();
      long percent = recursion.getMonitoringInterfaceForward().getPercentCPU();
      long processors = Runtime.getRuntime().availableProcessors();
      long generatedStates = recursion.getMonitoringInterfaceForward().getGeneratedStates();
      long reusedStates = recursion.getMonitoringInterfaceForward().getReusedStates();
      
      return ETC + ", " + time + ", " + percent + ", " + processors + ", " + generatedStates + ", " + reusedStates; 
   }
   
   public static String getHeadersString(){
      return "ETC, Time, CPU, Cores, Generated States, Reused States";
   }
   
   public void printPolicy(){
      int period = 0;
      int bowserInitialLocation = 0;
      int bowserInitialTankLevel = this.bowserInitialTankLevel;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      BR_StateDescriptor initialState = new BR_StateDescriptor(period, 
            bowserInitialTankLevel, 
            bowserInitialLocation,
            machinesInitialTankLevel,
            machinesInitialLocation);
      for(int t = 1; t < T; t++){
         bowserInitialLocation = ((BR_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation();
         bowserInitialTankLevel += ((BR_Action)recursion.getOptimalAction(initialState)).getBowserRefuelQty() - Arrays.stream(((BR_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()).sum();
         machinesInitialLocation = getMachineLocationArray(M, machineLocation[t]);
         for(int i = 0; i < M; i++){
            machinesInitialTankLevel[i] = Math.max(0, machinesInitialTankLevel[i]) + ((BR_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()[i] - fuelConsumption[i][t-1];
         }
         initialState = new BR_StateDescriptor(period + t, 
                                               bowserInitialTankLevel, 
                                               bowserInitialLocation,
                                               machinesInitialTankLevel,
                                               machinesInitialLocation);
         logger.info(initialState.toString());
         logger.info("Expected total cost: "+recursion.getExpectedCost(initialState));
         logger.info("Optimal action: "+recursion.getOptimalAction(initialState).toString());
      }
   }
   
   private static int[] getMachineLocationArray(int M, double[][] machineLocationMatrix){
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

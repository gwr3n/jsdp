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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;
import jsdp.sdp.impl.univariate.SamplingScheme;

import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.rng.MRG32k3a;

/**
 * Stochastic Dynamic Bowser Routing Problem under Asset Location Uncertainty
 * 
 * Run with VM arguments -d64 -Xms512m -Xmx4g
 * 
 * @author Roberto Rossi
 *
 */

public class BowserRoutingLocation {
   
   static final Logger logger = LogManager.getLogger(BowserRoutingLocation.class.getName());
   
   int T, M, N;
   int bowserInitialTankLevel;
   int maxBowserTankLevel;
   int minRefuelingQty;   
   int[] tankCapacity;
   int[] initialTankLevel;
   int[][] fuelConsumption;
   int[][] connectivity;
   double[][] distance;
   double[][][] machineLocationProb;
   int fuelStockOutPenaltyCost;
   
   BRL_ForwardRecursion recursion;
   
   SamplingScheme samplingScheme;
   int sampleSize;
   double reductionFactorPerStage;
   
   static MRG32k3a rng = new MRG32k3a();
   
   enum InstanceType {
      TINY,
      SMALL,
      MEDIUM,
      LARGE,
      CUSTOM
   }
   
   InstanceType type = null;
   ProblemInstance instance = null;
   
   public BowserRoutingLocation(InstanceType type,
                                SamplingScheme samplingScheme,
                                int sampleSize,
                                double reductionFactorPerStage){
      this.type = type;
      this.resetInstance();
      
      this.samplingScheme = samplingScheme;
      this.sampleSize = sampleSize;
      this.reductionFactorPerStage = reductionFactorPerStage;
   }
   
   public BowserRoutingLocation(int T, int M, int N,
                                int bowserInitialTankLevel,
                                int maxBowserTankLevel,
                                int minRefuelingQty,
                                int[] tankCapacity,
                                int[] initialTankLevel,
                                int[][] fuelConsumption,
                                int[][] connectivity,
                                double[][] distance,
                                double[][][] machineLocationProb,
                                int fuelStockOutPenaltyCost,
                                SamplingScheme samplingScheme,
                                int sampleSize,
                                double reductionFactorPerStage){
      this.type = InstanceType.CUSTOM;
      this.instance = new ProblemInstance(T, M, N,
                                          bowserInitialTankLevel,
                                          maxBowserTankLevel,
                                          minRefuelingQty,
                                          tankCapacity,
                                          initialTankLevel,
                                          fuelConsumption,
                                          connectivity,
                                          distance,
                                          machineLocationProb,
                                          fuelStockOutPenaltyCost);
      this.resetInstance();
      
      this.samplingScheme = samplingScheme;
      this.sampleSize = sampleSize;
      this.reductionFactorPerStage = reductionFactorPerStage;
   }
   
   class ProblemInstance{
      int T, M, N;
      int bowserInitialTankLevel;
      int maxBowserTankLevel;
      int minRefuelingQty;   
      int[] tankCapacity;
      int[] initialTankLevel;
      int[][] fuelConsumption;
      int[][] connectivity;
      double[][] distance;
      double[][][] machineLocationProb;
      int fuelStockOutPenaltyCost;
      
      public ProblemInstance(int T, int M, int N,
                             int bowserInitialTankLevel,
                             int maxBowserTankLevel,
                             int minRefuelingQty,
                             int[] tankCapacity,
                             int[] initialTankLevel,
                             int[][] fuelConsumption,
                             int[][] connectivity,
                             double[][] distance,
                             double[][][] machineLocationProb,
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
         this.machineLocationProb = machineLocationProb.clone();
         this.fuelStockOutPenaltyCost = fuelStockOutPenaltyCost;
      }
   }
   
   private void resetInstance(){
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
      case CUSTOM:
         this.T = instance.T;
         this.M = instance.M;
         this.N = instance.N;
         this.maxBowserTankLevel = instance.maxBowserTankLevel;
         this.minRefuelingQty = instance.minRefuelingQty;
         this.tankCapacity = instance.tankCapacity.clone();
         this.initialTankLevel = instance.initialTankLevel.clone();
         this.fuelConsumption = instance.fuelConsumption.clone();
         this.connectivity = instance.connectivity.clone();
         this.distance = instance.distance.clone();
         this.machineLocationProb = instance.machineLocationProb.clone();
         this.fuelStockOutPenaltyCost = instance.fuelStockOutPenaltyCost;
         break;
      default:
         throw new NullPointerException("Instance type undefined");
      }
   }
   
   void tinyInstance(){
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
      machineLocationProb = new double[][][]{
           {{0, 0, 0, 1, 0},
            {0, 1, 0, 0, 0},
            {0, 0, 0, 0, 1}},
            
            {{0, 0.5, 0, 0, 0.5},   
            {0, 0.5, 0, 0.5, 0},
            {0.5, 0, 0, 0, 0.5}},
            
            {{0, 0.5, 0, 0, 0.5},
            {0, 0, 0.5, 0.5, 0},
            {0, 0.5, 0, 0, 0.5}}};
      
      fuelStockOutPenaltyCost = 100;
   }
   
   void smallInstance(){
      /*******************************************************************
       * Problem parameters
       */
      T = 5;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      bowserInitialTankLevel = 0;
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
      machineLocationProb = new double[][][]{
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
      {0, 0, 0.5, 0, 0.5}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   void mediumInstance(){
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
      fuelConsumption = new int[][]{{4, 4, 2, 1, 3},
                                    {4, 2, 3, 4, 3},
                                    {2, 4, 1, 2, 2}};
                     
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
      machineLocationProb = new double[][][]{
               {{0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{0, 0, 0, 0.5, 0, 0, 0, 0, 0, 0.5},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0}},
               {{0.5, 0, 0, 0, 0, 0, 0, 0.5, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
               {0, 0, 0, 0, 0, 0, 1, 0, 0, 0}},
               {{0.5, 0, 0, 0, 0, 0, 0.5, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0.5, 0.5, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 0.5, 0, 0, 0, 0, 0.5, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   void largeInstance(){
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
      machineLocationProb = new double[][][]{
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
      
      fuelStockOutPenaltyCost = 20;
   }
   
   public BRL_ForwardRecursion buildModel(){
      
      /*******************************************************************
       * Model definition
       */
      
      // State space
      
      int minBowserTankLevel = 0;
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
                  for(int j = 0; j <= BRL_State.getMaxBowserTankLevel() - state.getBowserTankLevel(); j+= minRefuelingQty){
                     final int bowserRefuelQty = j;
                     feasibleActions.addAll(
                           BRL_Action.computeMachineRefuelQtys(state, j ,minRefuelingQty).parallelStream().map(action -> 
                           new BRL_Action(state, bowserNewLocation, bowserRefuelQty, action)).collect(Collectors.toList())
                           );
                  }
               }else{
                  final int bowserRefuelQty = 0;
                  feasibleActions.addAll(
                        BRL_Action.computeMachineRefuelQtys(state, 0, minRefuelingQty).parallelStream().map(action -> 
                           new BRL_Action(state, bowserNewLocation, bowserRefuelQty, action)).collect(Collectors.toList())
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
                                                              machineLocationProb, 
                                                              fuelConsumption, 
                                                              immediateValueFunction, 
                                                              buildActionList,
                                                              discountFactor,
                                                              HashType.HASHTABLE,
                                                              stateSpaceSizeLowerBound,
                                                              loadFactor,
                                                              samplingScheme,
                                                              sampleSize,
                                                              reductionFactorPerStage);
      
      return recursion;
   }
   
   public static void main(String args[]){
      /**
       * Sampling scheme
       */
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int sampleSize = 10;                                     // This is the sample size used to determine a state value function
      double reductionFactorPerStage = 5;
      
      BowserRoutingLocation bowserRoutingLocation = new BowserRoutingLocation(InstanceType.TINY,
                                                                              samplingScheme,
                                                                              sampleSize,
                                                                              reductionFactorPerStage);
      
      //bowserRoutingLocation.runInstance();
      //bowserRoutingLocation.printPolicy();
      
      int replications = 20;
      bowserRoutingLocation.simulateInstanceReplanning(replications);
   }
   
   private void simulateInstanceReplanning(int replications) {
      rng.setSeed(new long[]{12345,12345,12345,12345,12345,12345});
      double cost = 0;
      for(int i = 0; i < replications; i++)
         cost += runInstanceReplanning();
      logger.info("---");
      logger.info("Simulated expected total cost: "+cost/replications);
      logger.info("---");
   }
   
   public void runInstance(){
      
      recursion = buildModel();
      
      int period = 0;      
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocationProb[0]);
      
      BRL_StateDescriptor initialState = new BRL_StateDescriptor(period, 
                                                                 bowserInitialTankLevel, 
                                                                 bowserInitialLocation,
                                                                 machinesInitialTankLevel,
                                                                 machinesInitialLocation);

      recursion.runForwardRecursionMonitoring(((BRL_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
      long percent = recursion.getMonitoringInterfaceForward().getPercentCPU();   
      double ETC = recursion.getExpectedCost(initialState);
      logger.info("---");
      logger.info("Expected total cost: "+ETC);
      logger.info("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
      logger.info("Time elapsed: "+recursion.getMonitoringInterfaceForward().getTime());
      logger.info("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)"); 
      logger.info("---");
   }
   
   public void printPolicy(){
      int period = 0;      
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocationProb[0]);
      
      BRL_StateDescriptor initialState = new BRL_StateDescriptor(period, 
                                                                 bowserInitialTankLevel, 
                                                                 bowserInitialLocation,
                                                                 machinesInitialTankLevel,
                                                                 machinesInitialLocation);
      
      if(type == InstanceType.TINY  && samplingScheme == SamplingScheme.NONE){  
         /* This set of realisations is valid for tinyInstance */
         double[][][] machineLocation = new double[][][]{
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
            logger.info("--- t = " +t+ " ---");
            logger.info(initialState.toString());
            logger.info("Expected total cost: "+recursion.getExpectedCost(initialState));
            logger.info("Optimal action: "+recursion.getOptimalAction(initialState).toString());
            logger.info("---");
         }
      }
   }
   
   private double runInstanceReplanning(){
      
      resetInstance(); 
      
      int period = 0;      
      int bowserInitialLocation = 0;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocationProb[0]);
      
      double[][][] machineLocation = new double[T][M][N];
      
      for(int m = 0; m < M; m++) 
         System.arraycopy(machineLocationProb[0][m], 0, machineLocation[0][m], 0, machineLocationProb[0][m].length); 
      for(int t = 1; t < T; t++){
         for(int m = 0; m < M; m++){
            DiscreteDistribution dist = new DiscreteDistribution(IntStream.iterate(0, i -> i + 1)
                                                                          .limit(machineLocationProb[t][m].length)
                                                                          .toArray(), 
                                                                 machineLocationProb[t][m], 
                                                                 machineLocationProb[t][m].length);
            machineLocation[t][m][(int) dist.inverseF(rng.nextDouble())] = 1;
         }
      }
      
      BRL_StateDescriptor initialState = new BRL_StateDescriptor(period, 
            bowserInitialTankLevel, 
            bowserInitialLocation,
            machinesInitialTankLevel,
            machinesInitialLocation);
      
      double cost = 0;
      int timeHorizon = T;
      for(int t = 0; t < timeHorizon; t++){
         initialState = new BRL_StateDescriptor(period, 
                                                                    bowserInitialTankLevel, 
                                                                    bowserInitialLocation,
                                                                    machinesInitialTankLevel,
                                                                    machinesInitialLocation);
         
         recursion = buildModel();
         recursion.runForwardRecursionMonitoring(((BRL_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
         
         double ETC = recursion.getExpectedCost(initialState);
         long percent = recursion.getMonitoringInterfaceForward().getPercentCPU();
         logger.info("---");
         logger.info("Expected total cost: "+ETC);
         logger.info("Initial state: "+initialState);
         logger.info("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
         logger.info("Time elapsed: "+recursion.getMonitoringInterfaceForward().getTime());
         logger.info("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)"); 
         logger.info("---");
         
         if(t < timeHorizon - 1)
            cost += distance[bowserInitialLocation][((BRL_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation()];
         bowserInitialLocation = ((BRL_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation();
         bowserInitialTankLevel += ((BRL_Action)recursion.getOptimalAction(initialState)).getBowserRefuelQty() - Arrays.stream(((BRL_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()).sum();
         if(t < timeHorizon - 1)
            machinesInitialLocation = getMachineLocationArray(M, machineLocation[t+1]);
         for(int i = 0; i < M; i++){
            machinesInitialTankLevel[i] = Math.max(0, machinesInitialTankLevel[i]) + ((BRL_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()[i] - fuelConsumption[i][0];
            cost += Math.max(-machinesInitialTankLevel[i], 0)*fuelStockOutPenaltyCost;
         }
         
         T -= 1;
         for(int m = 0; m < M; m++){
            final int machine = m;
            fuelConsumption[machine] = IntStream.iterate(1, i -> i + 1)
                                                .limit(fuelConsumption[machine].length - 1)
                                                .map(i -> fuelConsumption[machine][i])
                                                .toArray();
         }
         machineLocationProb = IntStream.iterate(1, i -> i + 1)
                                        .limit(machineLocationProb.length - 1)
                                        .mapToObj(i -> machineLocationProb[i])
                                        .toArray(double[][][]::new);
         if(t < timeHorizon - 1){
            for(int m = 0; m < M; m++) 
               System.arraycopy(machineLocationProb[0][m], 0, machineLocation[0][m], 0, machineLocationProb[0][m].length);
         }
      }
      logger.info("---");
      logger.info("Expected total cost: "+cost);
      logger.info("---");
      return cost;
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

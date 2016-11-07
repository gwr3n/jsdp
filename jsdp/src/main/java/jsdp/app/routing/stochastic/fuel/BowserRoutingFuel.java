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

package jsdp.app.routing.stochastic.fuel;

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
import jsdp.utilities.probdist.DiscreteDistributionFactory;
import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.stat.Tally;

/**
 * Stochastic Dynamic Bowser Routing Problem under Asset Fuel Consumption Uncertainty
 * 
 * Run with VM arguments -d64 -Xms512m -Xmx4g
 * 
 * @author Roberto Rossi
 *
 */

public class BowserRoutingFuel {
   
   static final Logger logger = LogManager.getLogger(BowserRoutingFuel.class.getName());
   
   int T, M, N;
   int bowserInitialTankLevel;
   int maxBowserTankLevel;
   int minRefuelingQty;
   int[] tankCapacity;
   int[] initialTankLevel;
   DiscreteDistribution[][] fuelConsumptionProb;
   int[][] connectivity;
   double[][] distance;
   double[][][] machineLocation;
   int fuelStockOutPenaltyCost;
   
   MRG32k3a rng = new MRG32k3a();
   
   SamplingScheme samplingScheme;
   int sampleSize;                                     
   double reductionFactorPerStage;
   BRF_ForwardRecursion simulatedRecursion;
   Tally simulatedExpectedTotalCost;
   
   BRF_ForwardRecursion recursion;
   
   enum InstanceType {
      TINY,
      SMALL,
      MEDIUM,
      LARGE,
      CUSTOM
   }
   
   InstanceType type = null;
   ProblemInstance instance = null;
   
   public BowserRoutingFuel(InstanceType type,
                            SamplingScheme samplingScheme,
                            int sampleSize,
                            double reductionFactorPerStage){
      this.type = type;
      this.resetInstance();
      
      this.samplingScheme = samplingScheme;
      this.sampleSize = sampleSize;
      this.reductionFactorPerStage = reductionFactorPerStage;
   }
   
   public BowserRoutingFuel(int T, int M, int N,
                            int bowserInitialTankLevel,
                            int maxBowserTankLevel,
                            int minRefuelingQty,
                            int[] tankCapacity,
                            int[] initialTankLevel,
                            DiscreteDistribution[][] fuelConsumptionProb,
                            int[][] connectivity,
                            double[][] distance,
                            double[][][] machineLocation,
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
                                          fuelConsumptionProb,
                                          connectivity,
                                          distance,
                                          machineLocation,
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
      DiscreteDistribution[][] fuelConsumptionProb;
      int[][] connectivity;
      double[][] distance;
      double[][][] machineLocation;
      int fuelStockOutPenaltyCost;
      
      public ProblemInstance(int T, int M, int N,
                             int bowserInitialTankLevel,
                             int maxBowserTankLevel,
                             int minRefuelingQty,
                             int[] tankCapacity,
                             int[] initialTankLevel,
                             DiscreteDistribution[][] fuelConsumptionProb,
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
         this.fuelConsumptionProb = fuelConsumptionProb.clone();
         this.connectivity = connectivity.clone();
         this.distance = distance.clone();
         this.machineLocation = machineLocation.clone();
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
         this.fuelConsumptionProb = instance.fuelConsumptionProb.clone();
         this.connectivity = instance.connectivity.clone();
         this.distance = instance.distance.clone();
         this.machineLocation = instance.machineLocation.clone();
         this.fuelStockOutPenaltyCost = instance.fuelStockOutPenaltyCost;
         break;
      default:
         throw new NullPointerException("Instance type undefined");
      }
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
      minRefuelingQty = 5;
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
            {0, 1, 0, 0, 0}}};
      
      fuelStockOutPenaltyCost = 100;
   }
   
   private void smallInstance(){
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
      {0, 0, 1, 0, 0}}};
      
      fuelStockOutPenaltyCost = 100;
   }
   
   private void mediumInstance(){
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
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   private void largeInstance(){
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
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}}};
      
      fuelStockOutPenaltyCost = 20;
   }
   
   private BRF_ForwardRecursion buildModel(){
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
                                                              sampleSize,
                                                              reductionFactorPerStage);
      
      return recursion;
   }
   
   public static void main(String args[]){
      /**
       * Sampling scheme
       */
      SamplingScheme samplingScheme = SamplingScheme.SIMPLE_RANDOM_SAMPLING;
      int sampleSize = 50;                                     // This is the sample size used to determine a state value function
      double reductionFactorPerStage = 5;
      
      BowserRoutingFuel bowserRoutingFuel = new BowserRoutingFuel(InstanceType.SMALL,
                                                                  samplingScheme,
                                                                  sampleSize,
                                                                  reductionFactorPerStage);
      
      bowserRoutingFuel.runInstance();
      bowserRoutingFuel.printPolicy();
      
      //int replications = 20;
      //bowserRoutingFuel.simulateInstanceReplanning(replications);
   }
   
   public void simulateInstanceReplanning(int replications) {
      Tally tally = new Tally();
      rng.setSeed(new long[]{12345,12345,12345,12345,12345,12345});
      for(int i = 0; i < replications; i++){
         logger.info("---");
         logger.info("Replication "+i+" ETC:"+(tally.numberObs() > 1 ? tally.average() + " " + tally.formatCIStudent(0.95) : Double.NaN));
         logger.info("---");
         tally.add(runInstanceReplanning());
      }
      logger.info("---");
      logger.info("Simulated expected total cost: "+tally.formatCIStudent(0.95));
      logger.info("---");
      this.simulatedExpectedTotalCost = tally;
   }

   public void runInstance(){      
      
      resetInstance(); 
      
      recursion = buildModel();
      
      int period = 0;
      int bowserInitialLocation = 0;
      int bowserInitialTankLevel = this.bowserInitialTankLevel;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BRF_StateDescriptor initialState = new BRF_StateDescriptor(period, 
                                                                 bowserInitialTankLevel, 
                                                                 bowserInitialLocation,
                                                                 machinesInitialTankLevel,
                                                                 machinesInitialLocation);

      recursion.runForwardRecursionMonitoring(((BRF_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
      long percent = recursion.getMonitoringInterfaceForward().getPercentCPU();
      double ETC = recursion.getExpectedCost(initialState);
      logger.info("---");
      logger.info("Expected total cost: "+ETC);
      logger.info("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
      logger.info("Time elapsed: "+recursion.getMonitoringInterfaceForward().getTime());
      logger.info("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      logger.info("---");
   }
   
   public String toString(){
      String stats = "";
      
      if(recursion == null)
         return stats;
      
      int period = 0;
      int bowserInitialLocation = 0;
      int bowserInitialTankLevel = this.bowserInitialTankLevel;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BRF_StateDescriptor initialState = new BRF_StateDescriptor(period, 
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
   
   public String toStringSimulation(){
      
      resetInstance();
      
      String stats = "";
      
      if(simulatedRecursion == null)
         return stats;
      
      int period = 0;
      int bowserInitialLocation = 0;
      int bowserInitialTankLevel = this.bowserInitialTankLevel;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BRF_StateDescriptor initialState = new BRF_StateDescriptor(period, 
                                                                 bowserInitialTankLevel, 
                                                                 bowserInitialLocation,
                                                                 machinesInitialTankLevel,
                                                                 machinesInitialLocation);
      
      double ETC = simulatedRecursion.getExpectedCost(initialState);
      long time = simulatedRecursion.getMonitoringInterfaceForward().getTime();
      long percent = simulatedRecursion.getMonitoringInterfaceForward().getPercentCPU();
      long processors = Runtime.getRuntime().availableProcessors();
      long generatedStates = simulatedRecursion.getMonitoringInterfaceForward().getGeneratedStates();
      long reusedStates = simulatedRecursion.getMonitoringInterfaceForward().getReusedStates();
      
      double[] centerAndRadius = new double[2];
      this.simulatedExpectedTotalCost.confidenceIntervalStudent(0.95, centerAndRadius);
      return centerAndRadius[0] + ", " + centerAndRadius[1] + ", " + ETC + ", " + time + ", " + percent + ", " + processors + ", " + generatedStates + ", " + reusedStates; 
   }
   
   public static String getSimulationHeadersString(){
      return "Simulated ETC mean, Simulated ETC Confidence Interval radius, ETC, Time, CPU, Cores, Generated States, Reused States\n";
   }
   
   public void printPolicy(){
      int period = 0;
      int bowserInitialLocation = 0;
      int bowserInitialTankLevel = this.bowserInitialTankLevel;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
      
      BRF_StateDescriptor initialState = new BRF_StateDescriptor(period, 
                                                                 bowserInitialTankLevel, 
                                                                 bowserInitialLocation,
                                                                 machinesInitialTankLevel,
                                                                 machinesInitialLocation);
      
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
            logger.info("---");
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
      int bowserInitialTankLevel = this.bowserInitialTankLevel;
      int[] machinesInitialTankLevel = Arrays.copyOf(initialTankLevel, initialTankLevel.length);
      int[] machinesInitialLocation = getMachineLocationArray(M, machineLocation[0]);
   
      int[][] fuelConsumption = new int[M][T];
      
      for(int m = 0; m < M; m++){
         for(int t = 0; t < T; t++){
            fuelConsumption[m][t] = (int) fuelConsumptionProb[m][t].inverseF(rng.nextDouble());
         }
      }
         
      double cost = 0;
      int timeHorizon = T;
      for(int t = 0; t < timeHorizon; t++){
         BRF_StateDescriptor initialState = new BRF_StateDescriptor(period, 
                                                                    bowserInitialTankLevel, 
                                                                    bowserInitialLocation,
                                                                    machinesInitialTankLevel,
                                                                    machinesInitialLocation);
         
         recursion = buildModel(); 
         recursion.runForwardRecursionMonitoring(((BRF_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
         if(t == 0)
            this.simulatedRecursion = recursion;
         
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
            cost += distance[bowserInitialLocation][((BRF_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation()];
         bowserInitialLocation = ((BRF_Action)recursion.getOptimalAction(initialState)).getBowserNewLocation();
         bowserInitialTankLevel += ((BRF_Action)recursion.getOptimalAction(initialState)).getBowserRefuelQty() - Arrays.stream(((BRF_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()).sum();
         if(t < timeHorizon - 1)
            machinesInitialLocation = getMachineLocationArray(M, machineLocation[1]);
         for(int i = 0; i < M; i++){
            machinesInitialTankLevel[i] = Math.max(0, machinesInitialTankLevel[i]) + ((BRF_Action)recursion.getOptimalAction(initialState)).getMachineRefuelQty()[i] - fuelConsumption[i][t];
            cost += Math.max(-machinesInitialTankLevel[i], 0)*fuelStockOutPenaltyCost;
         }
         
         T -= 1;
         for(int m = 0; m < M; m++){
            final int machine = m;
            fuelConsumptionProb[machine] = IntStream.iterate(1, i -> i + 1)
                                              .limit(fuelConsumptionProb[machine].length - 1)
                                              .mapToObj(i -> fuelConsumptionProb[machine][i])
                                              .toArray(DiscreteDistribution[]::new);
         }
         machineLocation = IntStream.iterate(1, i -> i + 1)
                                     .limit(machineLocation.length - 1)
                                     .mapToObj(i -> machineLocation[i])
                                     .toArray(double[][][]::new);
                                     
      }
      logger.info("---");
      logger.info("Expected total cost: "+cost);
      logger.info("---");
      return cost;
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

package jsdp.app.routing.stochastic.fuel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

import jsdp.app.routing.topologies.Location;
import jsdp.app.routing.topologies.Topology;
import jsdp.sdp.impl.univariate.SamplingScheme;
import jsdp.utilities.probdist.DiscreteDistributionFactory;
import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.PoissonDist;

public class BowserRoutingFuelBatch {
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
   
   public static void writeToFile(String fileName, String str){
      File results = new File(fileName);
      try {
         FileOutputStream fos = new FileOutputStream(results, true);
         OutputStreamWriter osw = new OutputStreamWriter(fos);
         osw.write(str+"\n");
         osw.close();
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   public static void main(String args[]){
      /**
       * Sampling scheme
       */
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int sampleSize = 30;                                     // This is the sample size used to determine a state value function
      double reductionFactorPerStage = 5;
      int replications = 500;
      
      /**
       * Fixed parameters
       */
      T = 5;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 20;
      minRefuelingQty = 1;
      tankCapacity = new int[]{6,6,6};
      
      final int minFuelConsumption = 0;
      final int maxFuelConsumption = 7;
      
      /**
       * Variable parameters
       */
      int topologies = 6;
      int[][] initialTankLevelArray = new int[][]{{0,0,0},{3,0,5},{5,5,5}};
      int[][][] fuelConsumptionArray = new int[][][]{
         {{3,3,3,3,3},
          {3,3,3,3,3},
          {3,3,3,3,3}
         },
         {{2,2,2,2,2},
          {1,1,1,1,1},
          {3,3,3,3,3}
         },
         {{1,2,3,4,5},
          {5,4,3,2,1},
          {3,3,1,1,2}
         }
      };
      int[] fuelStockOutPenaltyCosts = {50,100};
      
      if(samplingScheme == SamplingScheme.NONE){
         writeToFile("./"+BowserRoutingFuelBatch.class.getSimpleName() + "_results.csv", BowserRoutingFuel.getHeadersString());
      }else{
         writeToFile("./"+BowserRoutingFuelBatch.class.getSimpleName() + "_results_sim.csv", BowserRoutingFuel.getSimulationHeadersString());
      }
      
      for(int topology = 0; topology < topologies; topology++){
         for(int initialTankLevelIndex = 0; initialTankLevelIndex < initialTankLevelArray.length; initialTankLevelIndex++){
            for(int fuelConsumptionIndex = 0; fuelConsumptionIndex < fuelConsumptionArray.length; fuelConsumptionIndex++){
               for(int fuelStockOutPenaltyCostIndex = 0; fuelStockOutPenaltyCostIndex < fuelStockOutPenaltyCosts.length; fuelStockOutPenaltyCostIndex++){
      
                  /**
                   * Instance
                   */
                  initialTankLevel = initialTankLevelArray[initialTankLevelIndex];
                  int[][] fuelConsumption = fuelConsumptionArray[fuelConsumptionIndex];
                  fuelConsumptionProb = new DiscreteDistribution[fuelConsumption.length][fuelConsumption[fuelConsumptionIndex].length];
                  for(int i = 0; i < fuelConsumption.length; i++){
                     final int[] array = fuelConsumption[i];
                     fuelConsumptionProb[i] = Arrays.stream(array)
                                                    .mapToObj(k -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                                                      new PoissonDist(k), minFuelConsumption, maxFuelConsumption, 1.0))
                                                    .toArray(DiscreteDistribution[]::new);
                  } 
                  N = Topology.getTopology(topology).getN();
                  connectivity = Topology.getTopology(topology).getConnectivity().clone();
                  distance = Topology.getTopology(topology).getDistance().clone();
                  machineLocation = Location.getMachineLocation(topology).getMachineLocation().clone();
                  fuelStockOutPenaltyCost = fuelStockOutPenaltyCosts[fuelStockOutPenaltyCostIndex];
                  
                  BowserRoutingFuel bowserRoutingFuel = new BowserRoutingFuel(T, M, N, 
                                                                              bowserInitialTankLevel,
                                                                              maxBowserTankLevel,
                                                                              minRefuelingQty,
                                                                              tankCapacity,
                                                                              initialTankLevel,
                                                                              fuelConsumptionProb,
                                                                              connectivity,
                                                                              distance,
                                                                              machineLocation,
                                                                              fuelStockOutPenaltyCost,
                                                                              samplingScheme,
                                                                              sampleSize,
                                                                              reductionFactorPerStage);
                  
                  if(samplingScheme == SamplingScheme.NONE){
                     bowserRoutingFuel.runInstance();
                     writeToFile("./"+BowserRoutingFuelBatch.class.getSimpleName() + "_results.csv", bowserRoutingFuel.toString());
                  }else{
                     bowserRoutingFuel.simulateInstanceReplanning(replications);
                     writeToFile("./"+BowserRoutingFuelBatch.class.getSimpleName() + "_results_sim.csv", bowserRoutingFuel.toStringSimulation());
                  }
               }
            }
         }
      }
   }
}

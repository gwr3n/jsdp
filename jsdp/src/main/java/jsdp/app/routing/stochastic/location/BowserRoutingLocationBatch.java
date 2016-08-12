package jsdp.app.routing.stochastic.location;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import jsdp.app.routing.topologies.Location;
import jsdp.app.routing.topologies.Topology;
import jsdp.sdp.impl.univariate.SamplingScheme;

public class BowserRoutingLocationBatch {
   static int T, M, N;
   static int bowserInitialTankLevel;
   static int maxBowserTankLevel;
   static int minRefuelingQty;   
   static int[] tankCapacity;
   static int[] initialTankLevel;
   static int[][] fuelConsumption;
   static int[][] connectivity;
   static double[][] distance;
   static double[][][] machineLocationProb;
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
      int sampleSize = 10;                                     // This is the sample size used to determine a state value function
      double reductionFactorPerStage = 5;
      int replications = 20;
      
      /**
       * Fixed parameters
       */
      T = 5;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 20;
      minRefuelingQty = 1;
      tankCapacity = new int[]{10,10,10};
      
      /**
       * Variable parameters
       */
      int topologies = 6;
      int[][] initialTankLevelArray = new int[][]{{0,0,0},{10,0,5},{10,10,10}};
      int[][][] fuelConsumptionArray = new int[][][]{
         {{3,3,3,3,3},
          {3,3,3,3,3},
          {3,3,3,3,3},
         },
         {{2,2,2,2,2},
          {5,5,5,5,5},
          {8,8,8,8,8},
         },
         {{1,2,3,4,5},
          {5,4,3,2,1},
          {5,5,0,0,3},
         }
      };
      int[] fuelStockOutPenaltyCosts = {100,500};
      
      if(samplingScheme == SamplingScheme.NONE){
         writeToFile("./"+BowserRoutingLocationBatch.class.getName() + "_results.csv", BowserRoutingLocation.getHeadersString());
      }else{
         writeToFile("./"+BowserRoutingLocationBatch.class.getName() + "_results_sim.csv", BowserRoutingLocation.getSimulationHeadersString());
      }
      
      for(int topology = 0; topology < topologies; topology++){
         for(int initialTankLevelIndex = 0; initialTankLevelIndex < initialTankLevelArray.length; initialTankLevelIndex++){
            for(int fuelConsumptionIndex = 0; fuelConsumptionIndex < fuelConsumptionArray.length; fuelConsumptionIndex++){
               for(int fuelStockOutPenaltyCostIndex = 0; fuelStockOutPenaltyCostIndex < fuelStockOutPenaltyCosts.length; fuelStockOutPenaltyCostIndex++){
      
                  /**
                   * Instance
                   */
                  initialTankLevel = initialTankLevelArray[initialTankLevelIndex];
                  fuelConsumption = fuelConsumptionArray[fuelConsumptionIndex];
                  N = Topology.getTopology(topology).getN();
                  connectivity = Topology.getTopology(topology).getConnectivity().clone();
                  distance = Topology.getTopology(topology).getDistance().clone();
                  machineLocationProb = Location.getProbabilisticMachineLocation(topology).getMachineLocation().clone();
                  fuelStockOutPenaltyCost = fuelStockOutPenaltyCosts[fuelStockOutPenaltyCostIndex];
                  
                  BowserRoutingLocation bowserRoutingLocation = new BowserRoutingLocation(T, M, N, 
                                                                                          bowserInitialTankLevel,
                                                                                          maxBowserTankLevel,
                                                                                          minRefuelingQty,
                                                                                          tankCapacity,
                                                                                          initialTankLevel,
                                                                                          fuelConsumption,
                                                                                          connectivity,
                                                                                          distance,
                                                                                          machineLocationProb,
                                                                                          fuelStockOutPenaltyCost,
                                                                                          samplingScheme,
                                                                                          sampleSize,
                                                                                          reductionFactorPerStage);
                  
                  if(samplingScheme == SamplingScheme.NONE){
                     bowserRoutingLocation.runInstance();                
                     writeToFile("./"+BowserRoutingLocationBatch.class.getSimpleName() + "_results.csv", bowserRoutingLocation.toString());
                  }else{
                     bowserRoutingLocation.simulateInstanceReplanning(replications);
                     writeToFile("./"+BowserRoutingLocationBatch.class.getSimpleName() + "_results_sim.csv", bowserRoutingLocation.toStringSimulation());
                  }
               }
            }
         }
      }
   }
}

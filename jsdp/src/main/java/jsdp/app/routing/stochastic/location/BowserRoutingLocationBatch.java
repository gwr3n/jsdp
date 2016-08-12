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
         osw.write(str);
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
      T=5;
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 20;
      minRefuelingQty = 1;
      
      M=3;
      tankCapacity = new int[]{10,10,10};
      
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
      int topologies = 5;
      int[] fuelStockOutPenaltyCosts = {100,500};
      
      /**
       * Instance
       */
      initialTankLevel = initialTankLevelArray[0];
      fuelConsumption = fuelConsumptionArray[0];
      N = Topology.getTopology(0).getN();
      connectivity = Topology.getTopology(0).getConnectivity().clone();
      distance = Topology.getTopology(0).getDistance().clone();
      machineLocationProb = Location.getProbabilisticMachineLocation(0).getMachineLocation().clone();
      fuelStockOutPenaltyCost = fuelStockOutPenaltyCosts[0];
      
      /**
       * Sampling scheme
       */
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int sampleSize = 10;                                     // This is the sample size used to determine a state value function
      double reductionFactorPerStage = 5;
      
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
      
    bowserRoutingLocation.runInstance();
    
    //int replications = 20;
    //bowserRoutingLocation.simulateInstanceReplanning(replications);
   }
}

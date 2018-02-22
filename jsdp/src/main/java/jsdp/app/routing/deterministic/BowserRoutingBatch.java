package jsdp.app.routing.deterministic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import jsdp.app.routing.topologies.Location;
import jsdp.app.routing.topologies.Topology;

public class BowserRoutingBatch {
   static int T, M, N;
   static int bowserInitialTankLevel;
   static int maxBowserTankLevel;
   static int minRefuelingQty;
   static int[] tankCapacity;
   static int[] initialTankLevel;
   static int[][] fuelConsumption;
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
       * Fixed parameters
       */
      T = 5;   //time horizon
      M = 3;   //machines
      N = 5;   //nodes
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 20;
      minRefuelingQty = 5;
      tankCapacity = new int[]{10,10,10};
      
      /**
       * Variable parameters
       */
      int topologies = 6;
      int[][] initialTankLevelArray = {{0,0,0},{3,0,5},{5,5,5}};
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
      int[] fuelStockOutPenaltyCosts = {10,50};
      
      writeToFile("./"+BowserRoutingBatch.class.getSimpleName() + "_results.csv", BowserRouting.getHeadersString());
      
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
                  machineLocation = Location.getMachineLocation(topology).getMachineLocation().clone();
                  fuelStockOutPenaltyCost = fuelStockOutPenaltyCosts[fuelStockOutPenaltyCostIndex];
                  
                  BowserRouting bowserRouting = new BowserRouting(T, M, N, 
                        bowserInitialTankLevel,
                        maxBowserTankLevel,
                        minRefuelingQty,
                        tankCapacity,
                        initialTankLevel,
                        fuelConsumption,
                        connectivity,
                        distance,
                        machineLocation,
                        fuelStockOutPenaltyCost);
                  bowserRouting.runInstance();
                  writeToFile("./"+BowserRoutingBatch.class.getSimpleName() + "_results.csv", bowserRouting.toString());
               }
            }
         }
      }
   }
}

package jsdp.app.routing.deterministic;

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
      machineLocation = Location.getMachineLocation(0).getMachineLocation().clone();
      fuelStockOutPenaltyCost = fuelStockOutPenaltyCosts[0];
      
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
      bowserRouting.printPolicy();
   }
}

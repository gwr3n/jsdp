package jsdp.app.routing.stochastic.fuel;

import java.util.Arrays;

import jsdp.app.routing.stochastic.fuel.BowserRoutingFuel.InstanceType;
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
   
   public static void main(String args[]){
      T=5;
      bowserInitialTankLevel = 0;
      maxBowserTankLevel = 20;
      minRefuelingQty = 5;
      
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
      
      final int minFuelConsumption = 0;
      final int maxFuelConsumption = 4;
      int[][] fuelConsumption = fuelConsumptionArray[0];
      fuelConsumptionProb = new DiscreteDistribution[fuelConsumption.length][fuelConsumption[0].length];
      for(int i = 0; i < fuelConsumption.length; i++){
         final int[] array = fuelConsumption[i];
         fuelConsumptionProb[i] = Arrays.stream(array)
                                        .mapToObj(k -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(
                                                          new PoissonDist(k), minFuelConsumption, maxFuelConsumption, 1.0))
                                        .toArray(DiscreteDistribution[]::new);
      } 
      
      N = Topology.getTopology(0).getN();
      connectivity = Topology.getTopology(0).getConnectivity().clone();
      distance = Topology.getTopology(0).getDistance().clone();
      machineLocation = Location.getMachineLocation(0).getMachineLocation().clone();
      fuelStockOutPenaltyCost = fuelStockOutPenaltyCosts[0];
      
      /**
       * Sampling scheme
       */
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int sampleSize = 10;                                     // This is the sample size used to determine a state value function
      double reductionFactorPerStage = 5;
      
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

      bowserRoutingFuel.runInstance();

      //int replications = 20;
      //bowserRoutingFuel.simulateInstanceReplanning(replications);
   }
}

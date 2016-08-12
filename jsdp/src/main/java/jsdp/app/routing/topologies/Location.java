package jsdp.app.routing.topologies;

import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

public class Location {
   
   double[][][] machineLocation;
   
   public Location(double[][][] machineLocation){
      this.machineLocation = machineLocation;
   }
   
   public double[][][] getMachineLocation(){
      return this.machineLocation;
   }
   
   public static Location getMachineLocation(int configurationNumber){
      switch(configurationNumber){
      case 0:
         return machineLocation_0();
      case 1:
         return machineLocation_1();
      case 2:
         return machineLocation_2();
      case 3: 
         return machineLocation_3();
      case 4:
         return machineLocation_4();
      case 5:
         return machineLocation_5();   
      default:
         throw new NullPointerException("Inexistent machine location configuration");
      }
   }
   
   public static Location getProbabilisticMachineLocation(int configurationNumber){
      MRG32k3a rng = new MRG32k3a();
      rng.setSeed(new long[]{12345,12345,12345,12345,12345,12345});
      rng.resetStartStream();
      switch(configurationNumber){
      case 0:
         return probabilisticMachineLocation(rng);
      case 1:
         rng.resetNextSubstream();
         return probabilisticMachineLocation(rng);
      case 2:
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         return probabilisticMachineLocation(rng);
      case 3: 
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         return probabilisticMachineLocation(rng);
      case 4:
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         return probabilisticMachineLocation(rng);
      case 5:
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         rng.resetNextSubstream();
         return probabilisticMachineLocation(rng);   
      default:
         throw new NullPointerException("Inexistent machine location configuration");
      }
   }
   
   private static Location probabilisticMachineLocation(RandomStream rng) {
      int T = 5, M = 3, N = 10, S = 2;
      double[][][] machineLocation = new double[T][M][N];
      for(int m = 0; m < M; m++){
         machineLocation[0][m][rng.nextInt(0, N-1)] = 1;
      }
      for(int t = 1; t < T; t++){
         for(int m = 0; m < M; m++){
            for(int s = 0; s < S; s++){
               machineLocation[t][m][rng.nextInt(0, N-1)] += 1;
            }
            for(int n = 0; n < N; n++)
               machineLocation[t][m][n] /= S;
         }
      }
      return new Location(machineLocation);
   }

   private static Location machineLocation_0() {
      double[][][] machineLocation = new double[][][]
            {{{0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 1, 0, 0, 0, 0}},
            {{0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0}},
            {{0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}},
            {{0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
            {{0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 0}}};
      return new Location(machineLocation);
   }

   private static Location machineLocation_1() {
      double[][][] machineLocation = new double[][][]
            {{{0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0}},
            {{0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}},
            {{0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0}},
            {{0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0}},
            {{0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 0, 1, 0, 0, 0, 0}}};
      return new Location(machineLocation);
   }

   private static Location machineLocation_2() {
      double[][][] machineLocation = new double[][][]
            {{{0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 1, 0, 0, 0, 0}},
               {{1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 1, 0}},
               {{0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
               {0, 0, 0, 0, 0, 1, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0}}};
      return new Location(machineLocation);
   }

   private static Location machineLocation_3() {
      double[][][] machineLocation = new double[][][]
            {{{0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 1, 0, 0}},
               {{0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 1, 0, 0}},
               {{0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0}}};
      return new Location(machineLocation);
   }
   
   private static Location machineLocation_4() {
      double[][][] machineLocation = new double[][][]
            {{{0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
               {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 1, 0}},
               {{0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0}},
               {{1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
               {0, 0, 0, 1, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
               {0, 0, 0, 0, 0, 1, 0, 0, 0, 0}}};
      return new Location(machineLocation);
   }   
   
   private static Location machineLocation_5() {
      double[][][] machineLocation = new double[][][]
            {{{0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
               {0, 0, 0, 1, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
               {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 1, 0, 0, 0, 0, 0, 0, 0, 0}},
               {{0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 0, 1}},
               {{0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
               {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
               {0, 0, 0, 0, 0, 0, 0, 1, 0, 0}}};
      return new Location(machineLocation);
   }   
}

package jsdp.app.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class BR_State extends State {

   private static int minBowserTankLevel;
   private static int maxBowserTankLevel;
   private static int[] minMachineTankLevel;
   private static int[] maxMachineTankLevel;
   private static int networkSize;
   
   public static void setStateBoundaries(int minBowserTankLevel, 
                                         int maxBowserTankLevel,
                                         int[] minMachineTankLevel, 
                                         int[] maxMachineTankLevel,
                                         int networkSize){
      BR_State.minBowserTankLevel = minBowserTankLevel;
      BR_State.maxBowserTankLevel = maxBowserTankLevel;
      BR_State.minMachineTankLevel = Arrays.copyOf(minMachineTankLevel, minMachineTankLevel.length);
      BR_State.maxMachineTankLevel = Arrays.copyOf(maxMachineTankLevel, maxMachineTankLevel.length);
      BR_State.networkSize = networkSize;
   }
   
   public static int getMinBowserTankLevel(){
      return minBowserTankLevel;
   }
   
   public static int getMaxBowserTankLevel(){
      return maxBowserTankLevel;
   }
   
   public static int[] getMinMachineTankLevel(){
      return minMachineTankLevel;
   }
   
   public static int[] getMaxMachineTankLevel(){
      return maxMachineTankLevel;
   }
   
   public static int getNetworkSize(){
      return networkSize;
   }
   
   private int bowserTankLevel;
   private int bowserLocation;
   
   private int machineTankLevel[];
   private int machineLocation[];
   
   public BR_State(BR_StateDescriptor descriptor,
                   Function<State, ArrayList<Action>> buildActionList){
      super(descriptor.getPeriod());
      this.bowserTankLevel = descriptor.getBowserTankLevel();
      this.bowserLocation = descriptor.getBowserLocation();
      this.machineTankLevel = Arrays.copyOf(descriptor.getMachineTankLevel(), descriptor.getMachineTankLevel().length);
      this.machineLocation = Arrays.copyOf(descriptor.getMachineLocation(), descriptor.getMachineLocation().length);
      this.buildActionList(buildActionList);
   }
   
   public int getBowserTankLevel(){
      return this.bowserTankLevel;
   }
   
   public int getBowserLocation(){
      return this.bowserLocation;
   }
   
   public int[] getMachineTankLevel(){
      return this.machineTankLevel;
   }
   
   public int[] getMachineLocation(){
      return this.machineLocation;
   }
   
   @Override
   public boolean equals(Object state) {
      if(state instanceof BR_State)
         return this.period == ((BR_State)state).period && 
                this.bowserTankLevel == ((BR_State)state).bowserTankLevel &&
                this.bowserLocation == ((BR_State)state).bowserLocation &&
                Arrays.equals(this.machineTankLevel, ((BR_State)state).machineTankLevel) &&
                Arrays.equals(this.machineLocation, ((BR_State)state).machineLocation);
      else 
         return false;
   }

   @Override
   public int hashCode() {
      String hash = "S";
      hash = (hash + period) + "_" + 
             this.bowserTankLevel + "_" +
             this.bowserLocation +
             Arrays.toString(this.machineTankLevel);
             Arrays.toString(this.machineLocation);
      return hash.hashCode();
   }
   
   @Override
   protected void buildActionList(){
      throw new NullPointerException("Method not implemented");
   }
   
   protected void buildActionList(
         Function<State, ArrayList<Action>> buildActionList){
      this.feasibleActions = buildActionList.apply(this);
   }
   
   @Override
   public String toString(){
      String out = "";
      out = "Period: " + period + "\t" + 
            "Bowser fuel: " + this.bowserTankLevel + "\t" +
            "Bowser location: " + this.bowserLocation + "\t" +
            "Machine tank: " +Arrays.toString(this.machineTankLevel) + "\t" +
            "Machine location: " + Arrays.toString(this.machineLocation);
      return out;
   }
}

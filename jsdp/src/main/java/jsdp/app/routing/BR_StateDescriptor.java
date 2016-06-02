package jsdp.app.routing;

import java.util.Arrays;

import jsdp.sdp.StateDescriptor;

public class BR_StateDescriptor extends StateDescriptor {

   private int bowserTankLevel;
   private int bowserLocation;
   
   private int machineTankLevel[];
   private int machineLocation[];
   
   public BR_StateDescriptor(int period,
                             int bowserTankLevel,
                             int bowserLocation,
                             int machineTankLevel[],
                             int machineLocation[]){
      super(period);
      this.bowserTankLevel = bowserTankLevel;
      this.bowserLocation = bowserLocation;
      this.machineTankLevel = Arrays.copyOf(machineTankLevel, machineTankLevel.length);
      this.machineLocation = Arrays.copyOf(machineLocation, machineLocation.length);
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
      if(state instanceof BR_StateDescriptor)
         return this.period == ((BR_StateDescriptor)state).period && 
                this.bowserTankLevel == ((BR_StateDescriptor)state).bowserTankLevel &&
                this.bowserLocation == ((BR_StateDescriptor)state).bowserLocation &&
                Arrays.equals(this.machineTankLevel, ((BR_StateDescriptor)state).machineTankLevel) &&
                Arrays.equals(this.machineLocation, ((BR_StateDescriptor)state).machineLocation);
      else 
         return false;
   }

   @Override
   public int hashCode() {
      String hash = "SD";
      hash = (hash + period) + "_" + 
             this.bowserTankLevel + "_" +
             this.bowserLocation +
             Arrays.toString(this.machineTankLevel);
             Arrays.toString(this.machineLocation);
      return hash.hashCode();
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

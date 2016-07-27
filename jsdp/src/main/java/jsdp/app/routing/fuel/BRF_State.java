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

package jsdp.app.routing.fuel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class BRF_State extends State {

   private static final long serialVersionUID = 1L;
   
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
      BRF_State.minBowserTankLevel = minBowserTankLevel;
      BRF_State.maxBowserTankLevel = maxBowserTankLevel;
      BRF_State.minMachineTankLevel = Arrays.copyOf(minMachineTankLevel, minMachineTankLevel.length);
      BRF_State.maxMachineTankLevel = Arrays.copyOf(maxMachineTankLevel, maxMachineTankLevel.length);
      BRF_State.networkSize = networkSize;
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
   
   public BRF_State(BRF_StateDescriptor descriptor){
      super(descriptor.getPeriod());
      this.bowserTankLevel = descriptor.getBowserTankLevel();
      this.bowserLocation = descriptor.getBowserLocation();
      this.machineTankLevel = Arrays.copyOf(descriptor.getMachineTankLevel(), descriptor.getMachineTankLevel().length);
      this.machineLocation = Arrays.copyOf(descriptor.getMachineLocation(), descriptor.getMachineLocation().length);
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
      if(state instanceof BRF_State)
         return this.period == ((BRF_State)state).period && 
                this.bowserTankLevel == ((BRF_State)state).bowserTankLevel &&
                this.bowserLocation == ((BRF_State)state).bowserLocation &&
                Arrays.equals(this.machineTankLevel, ((BRF_State)state).machineTankLevel) &&
                Arrays.equals(this.machineLocation, ((BRF_State)state).machineLocation);
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

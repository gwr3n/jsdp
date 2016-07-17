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

package jsdp.app.routing.location;

import java.util.Arrays;

import jsdp.sdp.StateDescriptor;

public class BRL_StateDescriptor extends StateDescriptor {

   private int bowserTankLevel;
   private int bowserLocation;
   
   private int machineTankLevel[];
   private int machineLocation[];
   
   public BRL_StateDescriptor(int period,
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
      if(state instanceof BRL_StateDescriptor)
         return this.period == ((BRL_StateDescriptor)state).period && 
                this.bowserTankLevel == ((BRL_StateDescriptor)state).bowserTankLevel &&
                this.bowserLocation == ((BRL_StateDescriptor)state).bowserLocation &&
                Arrays.equals(this.machineTankLevel, ((BRL_StateDescriptor)state).machineTankLevel) &&
                Arrays.equals(this.machineLocation, ((BRL_StateDescriptor)state).machineLocation);
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

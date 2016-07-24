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

package jsdp.app.routing;

import java.util.ArrayList;
import java.util.Arrays;

import jsdp.sdp.State;
import jsdp.sdp.Action;

public class BR_Action extends Action {
   
   int bowserNewLocation;
   int bowserRefuelQty;
   int[] machineRefuelQty;
   
   public BR_Action(State state, 
                    int bowserNewLocation,
                    int bowserRefuelQty,
                    int[] machineRefuelQty){
      super(state);
      this.bowserNewLocation = bowserNewLocation;
      this.bowserRefuelQty = bowserRefuelQty;
      this.machineRefuelQty = Arrays.copyOf(machineRefuelQty, machineRefuelQty.length);
   }
   
   public int getBowserNewLocation(){
      return this.bowserNewLocation;
   }
   
   public int getBowserRefuelQty(){
      return this.bowserRefuelQty;
   }
   
   public int[] getMachineRefuelQty(){
      return this.machineRefuelQty;
   }

   @Override
   public boolean equals(Object action) {
      if(action instanceof BR_Action)
         return this.bowserNewLocation == ((BR_Action)action).bowserNewLocation &&
                this.bowserRefuelQty == ((BR_Action)action).bowserRefuelQty &&
                Arrays.equals(this.machineRefuelQty, ((BR_Action)action).machineRefuelQty);
      else
         return false;
   }

   @Override
   public int hashCode() {
      String hash = "A";
      hash = (hash + state.getPeriod()) + "_" + 
             this.bowserNewLocation + "_" +
             this.bowserRefuelQty + "_" +
             Arrays.toString(this.machineRefuelQty);
      return hash.hashCode();
   }
   
   private static void refuelMachine(int[] currentPlan, int machine, BR_State state, int availableFuel, ArrayList<int[]> qtys){
      if(machine == state.getMachineTankLevel().length - 1){
         qtys.add(Arrays.copyOf(currentPlan, currentPlan.length));
         for(int i = 1; i <= Math.min(availableFuel, BR_State.getMaxMachineTankLevel()[machine] - Math.max(state.getMachineTankLevel()[machine], 0)) &&
                        state.getMachineLocation()[machine] == state.getBowserLocation(); i+= 1){
            currentPlan[machine] = i;
            qtys.add(Arrays.copyOf(currentPlan, currentPlan.length));
         }
      }else{
         refuelMachine(Arrays.copyOf(currentPlan, currentPlan.length), machine + 1, state, availableFuel, qtys);
         for(int i = 1; i <= Math.min(availableFuel, BR_State.getMaxMachineTankLevel()[machine] - Math.max(state.getMachineTankLevel()[machine], 0)) &&
                        state.getMachineLocation()[machine] == state.getBowserLocation(); i+= 1){
            currentPlan[machine] = i;
            refuelMachine(Arrays.copyOf(currentPlan, currentPlan.length), machine + 1, state, availableFuel - i, qtys);
         }
      }
   }
   
   public static ArrayList<int[]> computeMachineRefuelQtys(BR_State state, int bowserRefuelingQty){
      ArrayList<int[]> qtys = new ArrayList<int[]>();
      refuelMachine(new int[state.getMachineTankLevel().length], 0, state, state.getBowserTankLevel() + bowserRefuelingQty, qtys);
      return qtys;
   }
   
   public String toString(){
      String out = "";
      out += "Period: " + state.getPeriod()  + "\t";
      out += "Bowser location: " + bowserNewLocation + "\t";
      out += "Bowser refuel: " + bowserRefuelQty + "\t";
      out += "Machines refuel: " + Arrays.toString(machineRefuelQty);
      return out;
   }

}

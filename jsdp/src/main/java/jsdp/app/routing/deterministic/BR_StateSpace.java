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

package jsdp.app.routing.deterministic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.State;
import jsdp.sdp.StateSpace;

public class BR_StateSpace extends StateSpace<BR_StateDescriptor> {
      
   public BR_StateSpace(int period,
                        Function<State, ArrayList<Action>> buildActionList,
                        HashType hashType,
                        int stateSpaceSizeLowerBound, 
                        float loadFactor){
      super(period, hashType, stateSpaceSizeLowerBound, loadFactor);
      BR_StateSpace.buildActionList = buildActionList;
   }
   
   public BR_StateSpace(int period,
                        Function<State, ArrayList<Action>> buildActionList,
                        HashType hashType){
      super(period, hashType);
      BR_StateSpace.buildActionList = buildActionList;
   }

   public boolean exists (BR_StateDescriptor descriptor){
      return states.get(descriptor) != null;
   }
   
   public State getState(BR_StateDescriptor descriptor){
      State value = states.get(descriptor);
      if(value == null){
         State state = new BR_State(descriptor);
         this.states.put(descriptor, state);
         return state;
      }else
         return (BR_State) value;
   }

   public Iterator<State> iterator() {
      throw new NullPointerException("Method not implemented");
   }
}

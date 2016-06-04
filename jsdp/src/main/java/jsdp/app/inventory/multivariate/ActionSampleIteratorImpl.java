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

package jsdp.app.inventory.multivariate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import jsdp.impl.multivariate.ActionImpl;
import jsdp.impl.multivariate.StateImpl;
import jsdp.sdp.Action;
import jsdp.sdp.ActionIterator;
import umontreal.ssj.rng.MRG32k3aL;
import umontreal.ssj.rng.RandomStream;

/**
 * A concrete implementation of {@code StateSpaceIterator}.
 * 
 * @author Roberto Rossi
 *
 */
public class ActionSampleIteratorImpl extends ActionIterator{
   
   private static RandomStream stream = new MRG32k3aL();

   StateImpl state;
   ActionImpl currentAction;
   
   int[] sampledActions;
   
   int pointer;
   
   public ActionSampleIteratorImpl(StateImpl state, int maxSamples){
      this.state = state;
      
      stream.resetStartStream();
      
      sampledActions = this.getNextJensensSample(maxSamples);
      Arrays.sort(sampledActions); 
      pointer = sampledActions.length - 1;
      
      currentAction = new ActionImpl(this.state, getIntAction(sampledActions[pointer], this.state));
   }
   
   private static int getActionSpaceCardinality(StateImpl state){
      int cardinality = 1;
      for(int i = 0; i < StateImpl.getStateDimension(); i++){
         cardinality *= StateImpl.getMaxIntState()[i] - state.getInitialIntState()[i] + 1;
      }
      return cardinality;
   }
   
   private static int getResidualStateSpaceCardinality(int j, StateImpl state){
      int cardinality = 1;
      for(int i = j; i < StateImpl.getStateDimension(); i++){
         cardinality *= StateImpl.getMaxIntState()[i] - state.getInitialIntState()[i] + 1;
      }
      return cardinality;
   }
   
   private static int[] getIntAction(int actionPointer, StateImpl state){
      int[] intState = new int[StateImpl.getStateDimension()];
      for(int i = 0; i < StateImpl.getStateDimension(); i++){
         intState[i] = Math.floorDiv(Math.floorMod(actionPointer, getResidualStateSpaceCardinality(i, state)), i < StateImpl.getStateDimension() - 1 ? getResidualStateSpaceCardinality(i+1, state) : 1);
      }
      return intState;
   }
   
   public int[] getNextSample(int samples){
      throw new NullPointerException("Method not implemented");
      /*int x[] = new int[samples];
      x = IntStream.iterate(0, i -> i + 1)
                   .limit(samples)
                   .map(i -> UniformIntGen.nextInt(stream, 0, getActionSpaceCardinality(this.state)))
                   .toArray();  
      Set<Integer> set = new HashSet<Integer>();
      for(int i : x){
         set.add(i);
      }
      Integer[] array = new Integer[set.size()];
      set.toArray(array);
      return Arrays.stream(array).mapToInt(i -> i).toArray();*/
   }
   
   public int[] getNextStratifiedSample(int samples){
      throw new NullPointerException("Method not implemented");
      /*int x[] = new int[samples];
      int stateSpaceSize = getActionSpaceCardinality(this.state);
      if(samples > stateSpaceSize) throw new NullPointerException("Samples larger than state space");
      x = IntStream.iterate(0, i -> i + stateSpaceSize/samples)
                   .limit(samples)
                   .map(i -> UniformIntGen.nextInt(stream, i, i + stateSpaceSize/samples))
                   .toArray();  
      Set<Integer> set = new HashSet<Integer>();
      for(int i : x){
         set.add(i);
      }
      Integer[] array = new Integer[set.size()];
      set.toArray(array);
      return Arrays.stream(array).mapToInt(i -> i).toArray();*/
   }
   
   public int[] getNextJensensSample(int samples){
      int x[] = new int[samples];
      int stateSpaceSize = getActionSpaceCardinality(this.state);
      //if(samples > stateSpaceSize) throw new NullPointerException("Samples larger than state space");
      x = IntStream.iterate(0, i -> i + Math.max(stateSpaceSize/samples,1))
                   .limit(Math.min(samples, stateSpaceSize))
                   .map(i -> i + Math.max(stateSpaceSize/(2*samples),1))
                   .toArray();  
      Set<Integer> set = new HashSet<Integer>();
      for(int i : x){
         set.add(i);
      }
      Integer[] array = new Integer[set.size()];
      set.toArray(array);
      return Arrays.stream(array).mapToInt(i -> i).toArray();
   }
   
   public boolean hasNext() {
      if(pointer >= 0)
         return true;
      else
         return false;
   }
   
   public Action next() {
      if(pointer >= 0){
         Action action = currentAction;
         currentAction = pointer - 1 >= 0 ? new ActionImpl(this.state, getIntAction(sampledActions[pointer - 1], this.state)) : null;
         pointer--;
         return action;
      }else{
         return null;
      }
   }
}

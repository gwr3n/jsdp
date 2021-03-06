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

package jsdp.app.lotsizing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import jsdp.sdp.State;
import jsdp.sdp.StateSpaceIterator;

import umontreal.ssj.randvar.UniformIntGen;
import umontreal.ssj.rng.MRG32k3aL;
import umontreal.ssj.rng.RandomStream;

public class sS_StateSpaceSampleIterator extends StateSpaceIterator {

   RandomStream stream;
   
   sS_StateSpace stateSpace;
   sS_StateDescriptor currentStateDescriptor;
   
   int[] sampledStates;
   
   int maxSamples;
   int pointer;
   
   enum SamplingScheme {
         NONE,
         SIMPLE_RANDOM_SAMPLING, 
         STRATIFIED_SAMPLING, 
         JENSENS_PARTITIONING 
   }

   public sS_StateSpaceSampleIterator(sS_StateSpace stateSpace, int maxSamples){
      this.stateSpace = stateSpace;
      this.maxSamples = maxSamples;
      
      stream = new MRG32k3aL();
      stream.resetStartStream();
      for(int i = 0; i < stateSpace.getPeriod(); i++){
         stream.resetNextSubstream();
      }
      
      switch(stateSpace.getSamplingScheme()){
      case SIMPLE_RANDOM_SAMPLING:
         sampledStates = this.getNextSample(maxSamples);
         break;
      case STRATIFIED_SAMPLING:
         sampledStates = this.getNextStratifiedSample(maxSamples);
         break;
      case JENSENS_PARTITIONING:
      default:
         sampledStates = this.getNextJensensSample(maxSamples);
      }
      
      Arrays.sort(sampledStates); 
      
      pointer = sampledStates.length - 1;
      
      currentStateDescriptor = new sS_StateDescriptor(this.stateSpace.getPeriod(), sampledStates[pointer]);
   }
   
   public int[] getNextSample(int samples){
      int x[] = new int[samples];
      x = IntStream.iterate(0, i -> i + 1)
                   .limit(samples)
                   .map(i -> UniformIntGen.nextInt(stream, sS_State.getMinIntState(), sS_State.getMaxIntState()))
                   .toArray();  
      Set<Integer> set = new HashSet<Integer>();
      for(int i : x){
         set.add(i);
      }
      Integer[] array = new Integer[set.size()];
      set.toArray(array);
      return Arrays.stream(array).mapToInt(i -> i).toArray();
   }
   
   public int[] getNextStratifiedSample(int samples){
      int x[] = new int[samples];
      int stateSpaceSize = sS_State.getMaxIntState() - sS_State.getMinIntState() + 1;
      if(samples > stateSpaceSize) throw new NullPointerException("Samples larger than state space");
      x = IntStream.iterate(0, i -> i + stateSpaceSize/samples)
                   .limit(samples)
                   .map(i -> UniformIntGen.nextInt(stream, i, i + stateSpaceSize/samples) + sS_State.getMinIntState())
                   .toArray();  
      Set<Integer> set = new HashSet<Integer>();
      for(int i : x){
         set.add(i);
      }
      Integer[] array = new Integer[set.size()];
      set.toArray(array);
      return Arrays.stream(array).mapToInt(i -> i).toArray();
   }
   
   public int[] getNextJensensSample(int samples){
      int x[] = new int[samples];
      int stateSpaceSize = sS_State.getMaxIntState() - sS_State.getMinIntState() + 1;
      if(samples > stateSpaceSize) throw new NullPointerException("Samples larger than state space");
      x = IntStream.iterate(0, i -> i + stateSpaceSize/samples)
                   .limit(samples)
                   .map(i -> i + stateSpaceSize/(2*samples) + sS_State.getMinIntState())
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

   public State next() {
      if(pointer >= 0){
         State state = stateSpace.getState(currentStateDescriptor);
         currentStateDescriptor = new sS_StateDescriptor(this.stateSpace.getPeriod(), sampledStates[pointer]);
         pointer--;
         return state;
      }else{
         return null;
      }
   }
}

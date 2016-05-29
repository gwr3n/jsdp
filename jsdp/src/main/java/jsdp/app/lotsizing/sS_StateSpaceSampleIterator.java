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
         LATIN_HYPERCUBE_SAMPLING, 
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
      
      //sampledStates = this.getNextSample(maxSamples);
      //sampledStates = this.getNextStratifiedSample(maxSamples);
      sampledStates = this.getNextJensensSample(maxSamples);
      
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

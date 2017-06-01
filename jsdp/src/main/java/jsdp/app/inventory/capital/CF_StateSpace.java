package jsdp.app.inventory.capital;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.State;
import jsdp.sdp.StateSpace;

public class CF_StateSpace extends StateSpace<CF_StateDescriptor> {
   
   public CF_StateSpace(int period,
                        Function<State, ArrayList<Action>> buildActionList,
                        HashType hashType,
                        int stateSpaceSizeLowerBound, 
                        float loadFactor){
      super(period, hashType, stateSpaceSizeLowerBound, loadFactor);
      CF_StateSpace.buildActionList = buildActionList;
   }
   
   public CF_StateSpace(int period,
                        Function<State, ArrayList<Action>> buildActionList,
                        HashType hashType){
      super(period, hashType);
      CF_StateSpace.buildActionList = buildActionList;
   }
   
   public boolean exists(CF_StateDescriptor descriptor){
      return states.get(descriptor) != null;
   }
   
   public State getState(CF_StateDescriptor descriptor){
      State value = states.get(descriptor);
      if(value == null){
         State state = new CF_State(descriptor);
         this.states.put(descriptor, state);
         return state;
      }else
         return (CF_State) value;
   }
   
   public Iterator<State> iterator() {
      throw new NullPointerException("Method not implemented");
   }
}

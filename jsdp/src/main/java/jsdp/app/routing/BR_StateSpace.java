package jsdp.app.routing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.StateSpace;

public class BR_StateSpace extends StateSpace<BR_StateDescriptor> {
   
   public BR_StateSpace(int period,
         Function<State, ArrayList<Action>> buildActionList, int stateSpaceSizeLowerBound, float loadFactor){
      super(period, stateSpaceSizeLowerBound, loadFactor);
      this.buildActionList = buildActionList;
   }

   public boolean exists (BR_StateDescriptor descriptor){
      return states.get(descriptor) != null;
   }
   
   public State getState(BR_StateDescriptor descriptor){
      State value = states.get(descriptor);
      if(value == null){
         State state = new BR_State(descriptor, this.buildActionList);
         this.states.put(descriptor, state);
         return state;
      }else
         return (BR_State) value;
   }

   public Iterator<State> iterator() {
      throw new NullPointerException("Method not implemented");
   }
}

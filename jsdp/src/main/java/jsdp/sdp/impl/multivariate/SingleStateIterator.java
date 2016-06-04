package jsdp.sdp.impl.multivariate;

import jsdp.sdp.State;
import jsdp.sdp.StateSpaceIterator;

public class SingleStateIterator extends StateSpaceIterator {
   
   boolean hasNext = true;
   State state;
   
   public SingleStateIterator(State state){
      this.state = state;
   }
   
   @Override
   public boolean hasNext() {
      return this.hasNext;
   }

   @Override
   public State next() {
      this.hasNext = false;
      return this.state;
   }

}

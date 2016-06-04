package jsdp.app.inventory.univariate.simulation;

import java.util.Iterator;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.impl.univariate.ActionImpl;
import jsdp.sdp.impl.univariate.BackwardRecursionImpl;
import jsdp.sdp.impl.univariate.StateDescriptorImpl;
import jsdp.sdp.impl.univariate.StateImpl;
import jsdp.sdp.impl.univariate.StateSpaceImpl;

public class sS_Policy {
   
   BackwardRecursionImpl recursion;
   int horizonLength;
   
   public sS_Policy(BackwardRecursionImpl recursion, int horizonLength){
      this.recursion = recursion;
      this.horizonLength = horizonLength;
   }
   
   public double[][] getOptimalPolicy(double initialInventory){
      double[][] optimalPolicy = new double[2][];
      double[] S = new double[this.horizonLength];
      double[] s = new double[this.horizonLength];
      for(int i = 0; i < this.horizonLength; i++){
         if(i == 0) {
            StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(0, StateImpl.stateToIntState(initialInventory));
            s[i] = StateImpl.intStateToState(this.find_s(i).getInitialIntState());
            S[i] = ActionImpl.intActionToAction(recursion.getOptimalAction(stateDescriptor).getIntAction())+initialInventory;
         }
         else{
            s[i] = StateImpl.intStateToState(this.find_s(i).getInitialIntState());
            S[i] = StateImpl.intStateToState(this.find_S(i).getInitialIntState());
         }
      }
      optimalPolicy[0] = s;
      optimalPolicy[1] = S;
      return optimalPolicy;
   }
   
   public StateImpl find_S(int period){
      StateImpl s = this.find_s(period);
      int i = ((ActionImpl)recursion.getValueRepository().getOptimalAction(s)).getIntAction()+s.getInitialIntState();
      StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(period, i);
      StateImpl state = (StateImpl) ((StateSpaceImpl)recursion.getStateSpace()[period]).getState(stateDescriptor);
      return state;
   }
   
   public StateImpl find_s(int period){
      Iterator<State> iterator = recursion.getStateSpace()[period].iterator();
      StateImpl state = null;
      do{
         state = (StateImpl) iterator.next();
         Action action = recursion.getValueRepository().getOptimalAction(state);
         if(action == null)
            continue;
         if(((ActionImpl)action).getIntAction() > 0){
            return state;
         }
      }while(iterator.hasNext());
      return state;
   }
}

package jsdp.app.inventory.univariate.simulation;

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
            StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(0, initialInventory);
            s[i] = ((StateImpl) this.find_s(i)).getInitialState();
            S[i] = recursion.getOptimalAction(stateDescriptor).getAction()+initialInventory;
         }
         else{
            s[i] = ((StateImpl) this.find_s(i)).getInitialState();
            S[i] = ((StateImpl) this.find_S(i)).getInitialState();
         }
      }
      optimalPolicy[0] = s;
      optimalPolicy[1] = S;
      return optimalPolicy;
   }
   
   public State find_S(int period){
      StateImpl s = (StateImpl) this.find_s(period);
      double i = ((ActionImpl)recursion.getValueRepository().getOptimalAction(s)).getAction()+s.getInitialState();
      StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(period, i);
      return ((StateSpaceImpl)recursion.getStateSpace()[period]).getState(stateDescriptor);
   }
   
   public State find_s(int period){
      for(double i = StateImpl.getMaxState(); i >= StateImpl.getMinState(); i -= StateImpl.getStepSize()){
         StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(period, i);
         Action action = recursion.getOptimalAction(stateDescriptor);
         if(action == null)
            continue;
         if(((ActionImpl)action).getAction() > 0){
            return ((StateSpaceImpl)recursion.getStateSpace(period)).getState(stateDescriptor);
         }
      }
      StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(period, StateImpl.getMinState());
      return ((StateSpaceImpl)recursion.getStateSpace(period)).getState(stateDescriptor);
   }
}

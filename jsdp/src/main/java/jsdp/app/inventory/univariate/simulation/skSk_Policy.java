package jsdp.app.inventory.univariate.simulation;

import java.util.ArrayList;
import java.util.Arrays;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.impl.univariate.ActionImpl;
import jsdp.sdp.impl.univariate.BackwardRecursionImpl;
import jsdp.sdp.impl.univariate.StateDescriptorImpl;
import jsdp.sdp.impl.univariate.StateImpl;
import jsdp.sdp.impl.univariate.StateSpaceImpl;

/**
 * This class extracts an (sk,Sk) policy from a {@code BackwardRecursionImpl} object
 * 
 * @author Roberto Rossi
 *
 */
public class skSk_Policy {
   
   BackwardRecursionImpl recursion;
   int horizonLength;
   
   public skSk_Policy(BackwardRecursionImpl recursion, int horizonLength){
      this.recursion = recursion;
      this.horizonLength = horizonLength;
   }
   
   /**
    * Extracts the optimal policy for a given initial inventory level
    * 
    * @param initialInventory the initial inventory level
    * @return the optimal policy array
    */
   public double[][][] getOptimalPolicy(double initialInventory, int thresholdNumberLimit){
      double[][][] optimalPolicy = new double[2][][];
      double[][] S = new double[this.horizonLength][];
      double[][] s = new double[this.horizonLength][];
      for(int i = 0; i < this.horizonLength; i++){
         State[][] policy = find_skSk(i);
         s[i] = Arrays.stream(policy[0], Math.max(0, policy[0].length-thresholdNumberLimit), policy[0].length).mapToDouble(a -> ((StateImpl)a).getInitialState()).toArray();
         S[i] = Arrays.stream(policy[1], Math.max(0, policy[1].length-thresholdNumberLimit), policy[1].length).mapToDouble(a -> ((StateImpl)a).getInitialState()).toArray();
      }
      optimalPolicy[0] = s;
      optimalPolicy[1] = S;
      return optimalPolicy;
   }
   
   private State[][] find_skSk(int period){
      ArrayList<State> s = new ArrayList<State>();
      ArrayList<State> S = new ArrayList<State>();
      double nextAction = 0;
      boolean startedOrdering = false;
      for(double i = StateImpl.getMaxState(); i >= StateImpl.getMinState(); i -= StateImpl.getStepSize()){
         StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(period, i);
         Action action = recursion.getOptimalAction(stateDescriptor);
         if(action == null)
            continue;
         else if(
               (((ActionImpl)action).getAction() > 0 && nextAction == 0) ||
               ((ActionImpl)action).getAction() > 0 && nextAction > ((ActionImpl)action).getAction()
               ){
            if(!startedOrdering)
               startedOrdering = true;
            State reorder = ((StateSpaceImpl)recursion.getStateSpace(period)).getState(stateDescriptor);
            s.add(0,reorder);
            double inv = ((ActionImpl)recursion.getValueRepository().getOptimalAction(reorder)).getAction()+((StateImpl)reorder).getInitialState();
            StateDescriptorImpl descriptor = new StateDescriptorImpl(period, inv);
            S.add(0, ((StateSpaceImpl)recursion.getStateSpace()[period]).getState(descriptor));
         }else if(startedOrdering && ((ActionImpl)action).getAction() == 0) {
            System.err.println("Zero order after started ordering. Period: "+period+". State: "+i+". Next action: "+nextAction);
         }
         nextAction = ((ActionImpl)action).getAction();
      }
      if(s.size() == 0 || S.size() == 0) {
         System.err.println("State space boundaries probably too narrow.");
         StateDescriptorImpl stateDescriptor = new StateDescriptorImpl(period, StateImpl.getMinState());
         State[][] policy = new State[2][];
         policy[0] = new State[] {((StateSpaceImpl)recursion.getStateSpace(period)).getState(stateDescriptor)};
         policy[1] = new State[] {((StateSpaceImpl)recursion.getStateSpace(period)).getState(stateDescriptor)};    
         return policy;
      }else {
         State[][] policy = new State[2][];
         policy[0] = s.stream().toArray(State[]::new);
         policy[1] = S.stream().toArray(State[]::new);
         return policy;
      }
   }
}

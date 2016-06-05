package jsdp.app.routing;

import java.util.ArrayList;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.ForwardRecursion;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.State;
import jsdp.sdp.ValueRepository;

public class BR_ForwardRecursion extends ForwardRecursion {
   
   double[][][] transitionProbabilities; 
   int[][] fuelConsumption;
   
   public BR_ForwardRecursion(int horizonLength,
                              double[][][] transitionProbabilities, 
                              int[][] fuelConsumption,
                              ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                              Function<State, ArrayList<Action>> buildActionList,
                              double discountFactor){
      super(OptimisationDirection.MIN);
      this.horizonLength = horizonLength;
      this.transitionProbabilities = transitionProbabilities;
      this.fuelConsumption = fuelConsumption;
      
      this.stateSpace = new BR_StateSpace[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new BR_StateSpace(i, buildActionList);                                         //Hashtable
      this.transitionProbability = new BR_TransitionProbability(transitionProbabilities, 
                                                                fuelConsumption, 
                                                                (BR_StateSpace[])this.getStateSpace());
      this.valueRepository = new ValueRepository(immediateValueFunction, discountFactor);                    //Hashtable
   }
   
   public BR_ForwardRecursion(int horizonLength,
         double[][][] transitionProbabilities, 
         int[][] fuelConsumption,
         ImmediateValueFunction<State, Action, Double> immediateValueFunction,
         Function<State, ArrayList<Action>> buildActionList,
         double discountFactor,
         int stateSpaceSizeLowerBound,
         float loadFactor){
      super(OptimisationDirection.MIN);
      this.horizonLength = horizonLength;
      this.transitionProbabilities = transitionProbabilities;
      this.fuelConsumption = fuelConsumption;

      this.stateSpace = new BR_StateSpace[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new BR_StateSpace(i, buildActionList, stateSpaceSizeLowerBound, loadFactor);                        //THashMap
      this.transitionProbability = new BR_TransitionProbability(transitionProbabilities, 
                                                                fuelConsumption, 
                                                                (BR_StateSpace[])this.getStateSpace());
      this.valueRepository = new ValueRepository(immediateValueFunction, discountFactor, stateSpaceSizeLowerBound, loadFactor);   //THashMap
   }
   
   public double getExpectedCost(BR_StateDescriptor stateDescriptor){
      State state = ((BR_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return getExpectedValue(state);
   }
   
   public BR_Action getOptimalAction(BR_StateDescriptor stateDescriptor){
      State state = ((BR_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return (BR_Action) this.getValueRepository().getOptimalAction(state);
   }
}

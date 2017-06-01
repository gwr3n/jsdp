package jsdp.app.inventory.capital;

import java.util.ArrayList;
import java.util.function.Function;

import jsdp.sdp.Action;
import jsdp.sdp.ForwardRecursion;
import jsdp.sdp.HashType;
import jsdp.sdp.State;
import jsdp.sdp.ValueRepository;
import jsdp.sdp.impl.univariate.SamplingScheme;
import umontreal.ssj.probdist.Distribution;

public class CF_ForwardRecursion extends ForwardRecursion {
   
   Distribution[] demands;
   
   public CF_ForwardRecursion(Distribution[] demands,
                              jsdp.sdp.ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                              ImmediateValueFunction<State, Action, Integer, Double> immediateValueFunctionOutcome,
                              Function<State, ArrayList<Action>> buildActionList,
                              double discountFactor,
                              HashType hashType,
                              SamplingScheme samplingScheme,
                              int sampleSize,
                              double reductionFactorPerStage){
      super(OptimisationDirection.MAX);
      this.horizonLength = demands.length;
      this.demands = demands;
      
      this.stateSpace = new CF_StateSpace[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new CF_StateSpace(i, buildActionList, hashType);
      this.transitionProbability = new CF_TransitionProbability(this.demands,
                                                                immediateValueFunctionOutcome,
                                                                (CF_StateSpace[]) this.getStateSpace(),
                                                                samplingScheme,
                                                                sampleSize,
                                                                reductionFactorPerStage);
      this.valueRepository = new ValueRepository(immediateValueFunction, 
                                                 discountFactor, 
                                                 hashType);   
   }
   
   public CF_ForwardRecursion(Distribution[] demands,
                              jsdp.sdp.ImmediateValueFunction<State, Action, Double> immediateValueFunction,
                              ImmediateValueFunction<State, Action, Integer, Double> immediateValueFunctionOutcome,
                              Function<State, ArrayList<Action>> buildActionList,
                              double discountFactor,
                              HashType hashType,
                              int stateSpaceSizeLowerBound,
                              float loadFactor,
                              SamplingScheme samplingScheme,
                              int sampleSize,
                              double reductionFactorPerStage){
      super(OptimisationDirection.MAX);
      this.horizonLength = demands.length;
      this.demands = demands;

      this.stateSpace = new CF_StateSpace[this.horizonLength+1];
      for(int i = 0; i < this.horizonLength + 1; i++) 
         this.stateSpace[i] = new CF_StateSpace(i, buildActionList, hashType, stateSpaceSizeLowerBound, loadFactor);
      this.transitionProbability = new CF_TransitionProbability(this.demands,
                                                                immediateValueFunctionOutcome,
                                                                (CF_StateSpace[]) this.getStateSpace(),
                                                                samplingScheme,
                                                                sampleSize,
                                                                reductionFactorPerStage);
      this.valueRepository = new ValueRepository(immediateValueFunction, 
                                                 discountFactor, 
                                                 hashType);   
   }
   
   public double getExpectedCapital(CF_StateDescriptor stateDescriptor){
      State state = ((CF_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return getExpectedValue(state);
   }
   
   public CF_Action getOptimalAction(CF_StateDescriptor stateDescriptor){
      State state = ((CF_StateSpace)this.getStateSpace(stateDescriptor.getPeriod())).getState(stateDescriptor);
      return (CF_Action) this.getValueRepository().getOptimalAction(state);
   }

}

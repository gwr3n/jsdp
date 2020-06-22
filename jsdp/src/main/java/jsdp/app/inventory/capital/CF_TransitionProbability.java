package jsdp.app.inventory.capital;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;

import jsdp.sdp.Action;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;
import jsdp.sdp.impl.univariate.SamplingScheme;
import umontreal.ssj.probdist.Distribution;

public class CF_TransitionProbability extends TransitionProbability {
   
   Distribution[] distributions;
   double[] supportLB;
   double[] supportUB;
   double truncationQuantile = 0.99;
   
   SamplingScheme samplingScheme;
   int sampleSize;
   double reductionFactorPerStage;
   
   CF_StateSpace[] stateSpace;
   ImmediateValueFunction<State, Action, Integer, Double> immediateValueFunction;
   
   public CF_TransitionProbability(Distribution[] distributions,
                                   ImmediateValueFunction<State, Action, Integer, Double> immediateValueFunction,
                                   CF_StateSpace[] stateSpace,
                                   SamplingScheme samplingScheme,
                                   int sampleSize,
                                   double reductionFactorPerStage){
      this.distributions = distributions;
      this.supportLB = IntStream.iterate(0, i -> i + 1)
                                .limit(distributions.length)
                                .mapToDouble(i -> distributions[i].inverseF(1-truncationQuantile))
                                .toArray();
      this.supportUB = IntStream.iterate(0, i -> i + 1)
                                .limit(distributions.length)
                                .mapToDouble(i -> distributions[i].inverseF(truncationQuantile))
                                .toArray();
      this.immediateValueFunction = immediateValueFunction;
      this.stateSpace = stateSpace;
      this.samplingScheme = samplingScheme;
      this.sampleSize = sampleSize;
      this.reductionFactorPerStage = reductionFactorPerStage;
   }
   
   @Override
   public double getTransitionProbability(State initialState, Action action, State finalState) {
      int demand = ((CF_State) initialState).getInventory() + ((CF_Action) action).orderQuantity - ((CF_State) finalState).getInventory();
      double probability = this.distributions[initialState.getPeriod()].cdf(demand)-this.distributions[initialState.getPeriod()].cdf(demand-1);
      return probability;
   }
   
   @Override
   public ArrayList<State> generateFinalStates(State initialState, Action action) {
      ArrayList<State> finalStates = new ArrayList<State>();
      for(int demand = 0; demand < this.supportUB[initialState.getPeriod()]; demand++){
         CF_StateDescriptor descriptor = new CF_StateDescriptor(initialState.getPeriod() + 1,
                                                                ((CF_State) initialState).getInventory() + ((CF_Action) action).orderQuantity - demand,
                                                                ((CF_State) initialState).getCapital() + (int) Math.round(this.immediateValueFunction.apply(initialState, action, Integer.valueOf(demand))));
         finalStates.add(this.stateSpace[initialState.getPeriod() + 1].getState(descriptor));
      }
      
      if(this.samplingScheme == SamplingScheme.NONE)
         return finalStates;
      else if(this.samplingScheme == SamplingScheme.SIMPLE_RANDOM_SAMPLING){
         Random rnd = new Random(12345);
         Collections.shuffle(finalStates, rnd);
         //int reductionFactor = reductionFactorPerStage*initialState.getPeriod() == 0 ? 1 : reductionFactorPerStage*initialState.getPeriod();
         int reductionFactor = (int) Math.pow(reductionFactorPerStage, initialState.getPeriod());
         return new ArrayList<State>(finalStates.subList(0, this.sampleSize/reductionFactor < 1 ? 1 : Math.min(this.sampleSize/reductionFactor, finalStates.size())));
      }else{
         throw new NullPointerException("Method not implemented");
      }
   }
   
   @Override
   public ArrayList<State> getFinalStates(State initialState, Action action) {
      throw new NullPointerException("Method not implemented");
   }
}

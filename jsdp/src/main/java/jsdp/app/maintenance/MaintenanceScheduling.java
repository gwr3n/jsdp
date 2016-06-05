package jsdp.app.maintenance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.lang3.time.StopWatch;

import jsdp.sdp.Action;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.Recursion.OptimisationDirection;
import jsdp.sdp.impl.univariate.ActionImpl;
import jsdp.sdp.impl.univariate.BackwardRecursionImpl;
import jsdp.sdp.impl.univariate.SamplingScheme;
import jsdp.sdp.impl.univariate.StateDescriptorImpl;
import jsdp.sdp.impl.univariate.StateImpl;

import umontreal.ssj.probdist.DiscreteDistribution;

public class MaintenanceScheduling {

   public static void main(String[] args){
      /*******************************************************************
       * Problem parameters
       */
      
      int periods = 1000;
      double maintenanceCost[] = {0, 500+1500, 500+2500, 1000+3000}; 
      double lostProfit[] = {0, 1000, 1500, Double.NaN};

      double[] states = {0,1,2,3};
      double[][][] transitionProbabilities = {{{1.0/4,1.0/4,1.0/4,1.0/4},
                                               {0,1.0/3,1.0/3,1.0/3},
                                               {0,0,1.0/2,1.0/2},
                                               {0,0,0,1.0}},
                                             {{1.0,0,0,0},
                                              {1.0,0,0,0},
                                              {1.0,0,0,0},
                                              {1.0,0,0,0}}};
      
      DiscreteDistribution[][][] distributions = new DiscreteDistribution[periods][][];
      for(int t = 0; t < periods; t++){
         distributions[t] = new DiscreteDistribution[transitionProbabilities.length][];
         for(int a = 0; a < transitionProbabilities.length; a++){
            final int action = a;
            distributions[t][a] = Arrays.stream(states).mapToObj(s -> new DiscreteDistribution(states,transitionProbabilities[action][(int)s],states.length)).toArray(DiscreteDistribution[]::new);
         }
      }
      
      double initialMachineState = 3;

      /*******************************************************************
       * Model definition
       */

      // State space

      double stepSize = 1;       //Stepsize must be 1 for discrete distributions
      int minIntState = 0;
      int maxIntState = 3;
      StateImpl.setStateBoundaries(stepSize, minIntState, maxIntState);

      // Actions

      Function<State, ArrayList<Action>> buildActionList = s -> {
         StateImpl state = (StateImpl) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         if(state.getInitialState() < 3){
            feasibleActions.add(new ActionImpl(state, 0));
         }
         if(state.getInitialState() > 0){
            feasibleActions.add(new ActionImpl(state, 1));
         }
         return feasibleActions;
      };

      Function<State, Action> idempotentAction = s -> new ActionImpl(s, 0);

      // Immediate Value Function

      ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         StateImpl is = (StateImpl)initialState;
         ActionImpl a = (ActionImpl)action;
         StateImpl fs = (StateImpl)finalState;
         double cost = 0;
         if(a.getAction() == 1){
            cost += maintenanceCost[(int)is.getInitialState()];
         }
         if(a.getAction() == 0 && is.getInitialState() < 3){
            cost += lostProfit[(int)is.getInitialState()];
         }
         return cost;
      };

      // Random Outcome Function

      RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction = (initialState, action, finalState) -> {
         StateImpl is = (StateImpl)initialState;
         ActionImpl a = (ActionImpl)action;
         StateImpl fs = (StateImpl)finalState;
         if(a.getAction() == 1){
            return 0.0;
         }else{
            return (double)fs.getInitialIntState();
         }
      };

      /*******************************************************************
       * Solve
       */

      // Sampling scheme

      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int maxSampleSize = 5;


      // Value Function Processing Method: backward recursion
      double discountFactor = 0.95;
      BackwardRecursionImpl recursion = new BackwardRecursionImpl(OptimisationDirection.MIN,
                                                                  distributions,
                                                                  immediateValueFunction,
                                                                  randomOutcomeFunction,
                                                                  buildActionList,
                                                                  idempotentAction,
                                                                  discountFactor,
                                                                  samplingScheme,
                                                                  maxSampleSize);

      System.out.println("--------------Backward recursion--------------");
      StopWatch timer = new StopWatch();
      timer.start();
      recursion.runBackwardRecursion();
      timer.stop();
      System.out.println();
      double ETC = recursion.getExpectedCost(initialMachineState);
      System.out.println("Expected total cost (assuming initial state "+initialMachineState+"): "+ETC);
      for(int i = 0; i < states.length; i++){
         StateDescriptorImpl initialState = new StateDescriptorImpl(0, StateImpl.stateToIntState(states[i]));
         double action = StateImpl.intStateToState(recursion.getOptimalAction(initialState).getIntAction());
         System.out.println("Optimal action in state "+states[i]+": "+action);
      }
      System.out.println("Time elapsed: "+timer);
      System.out.println();
   }
}

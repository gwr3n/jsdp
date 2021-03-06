package jsdp.app.maintenance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.lang3.time.StopWatch;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
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

/**
 * Consider the problem of operating a machine that may be in any of the following 
 * possible states: good as new (0), minor fault (1), major fault (2), out of order (3). 
 * The transition probability from state i to state j during a given period is 
 * 
 *    1/4   1/4   1/4   1/4
 *    0     1/3   1/3   1/3
 *    0     0     1/2   1/2
 *    0     0     0     1
 *    
 * If a machine is in state 0 at the beginning of a period, no action should be taken;
 * costs associated with the production of faulty items over the coming period
 * when a machine is in state 0 are negligible. However, if the machine is in state
 * {@literal 1 < i < 2}, it is possible to take no action or to repair the machine for $500; once
 * the machine has been repaired, it returns to state 0 at the beginning of the next
 * period. If no action is taken in state 1, there will be a cost of $1000 associated
 * with the production of faulty items over the coming period. If the machine is
 * repaired in state 1, production will halt during the next period and there will
 * be a cost of $1500 associated with lost profits. Similarly, if the machine is in
 * state 2 and no action is taken, there will be a cost of $1500 associated with the
 * production of faulty items over the coming period. If the machine is repaired,
 * production will halt and there will be a cost of $2500 associated with lost profits
 * over the coming period. Finally, if the machine is in state 3, it must necessarily
 * be repaired to resume production at the beginning of the next period; in this
 * case repair costs amount to $1000 and lost profits amount to $3000
 * 
 * @author Roberto Rossi
 * 
 */

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
      double[][][] supportLB = new double[periods][][];
      double[][][] supportUB = new double[periods][][];
      for(int t = 0; t < periods; t++){
         distributions[t] = new DiscreteDistribution[transitionProbabilities.length][];
         supportLB[t] = new double[transitionProbabilities.length][];
         supportUB[t] = new double[transitionProbabilities.length][];      
         for(int a = 0; a < transitionProbabilities.length; a++){
            final int action = a;
            distributions[t][a] = Arrays.stream(states).mapToObj(s -> new DiscreteDistribution(states,transitionProbabilities[action][(int)s],states.length)).toArray(DiscreteDistribution[]::new);
            supportLB[t][a] = new double[states.length];
            Arrays.fill(supportLB[t][a], Arrays.stream(states).min().getAsDouble());
            supportUB[t][a] = new double[states.length];       
            Arrays.fill(supportUB[t][a], Arrays.stream(states).max().getAsDouble());
         }
      }
      
      double initialMachineState = 0;

      /*******************************************************************
       * Model definition
       */

      // State space

      double stepSize = 1;       //Stepsize must be 1 for discrete distributions
      double minState = 0;
      double maxState = 3;
      StateImpl.setStateBoundaries(stepSize, minState, maxState);

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
         //StateImpl fs = (StateImpl)finalState;
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
         //StateImpl is = (StateImpl)initialState;
         //ActionImpl a = (ActionImpl)action;
         StateImpl fs = (StateImpl)finalState;
         return (double)fs.getInitialIntState();
         /*if(a.getAction() == 1){
            return 0.0;
         }else{
            return (double)fs.getInitialIntState();
         }*/
      };

      /*******************************************************************
       * Solve
       */

      // Sampling scheme

      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int maxSampleSize = 5;
      double reductionFactorPerStage = 1;
      

      // Value Function Processing Method: backward recursion
      double discountFactor = 0.95;
      BackwardRecursionImpl recursion = new BackwardRecursionImpl(OptimisationDirection.MIN,
                                                                  distributions,
                                                                  supportLB,
                                                                  supportUB,
                                                                  immediateValueFunction,
                                                                  randomOutcomeFunction,
                                                                  buildActionList,
                                                                  idempotentAction,
                                                                  discountFactor,
                                                                  samplingScheme,
                                                                  maxSampleSize,
                                                                  reductionFactorPerStage,
                                                                  HashType.HASHTABLE);

      System.out.println("--------------Backward recursion--------------");
      StopWatch timer = new StopWatch();
      timer.start();
      recursion.runBackwardRecursion();
      timer.stop();
      System.out.println();
      double ETC = recursion.getExpectedCost(initialMachineState);
      System.out.println("Expected total cost (assuming initial state "+initialMachineState+"): "+ETC);
      for(int i = 0; i < states.length; i++){
         StateDescriptorImpl initialState = new StateDescriptorImpl(0, states[i]);
         double action = recursion.getOptimalAction(initialState).getAction();
         System.out.println("Optimal action in state "+states[i]+": "+action);
      }
      System.out.println("Time elapsed: "+timer);
      System.out.println();
   }
}

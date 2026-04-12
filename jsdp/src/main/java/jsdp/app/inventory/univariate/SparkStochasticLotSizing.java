/**
 * jsdp: A Java Stochastic Dynamic Programming Library
 *
 * MIT License
 *
 * Copyright (c) 2016 Roberto Rossi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jsdp.app.inventory.univariate;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.Recursion.OptimisationDirection;
import jsdp.sdp.SparkBackwardRecursion;
import jsdp.sdp.SparkValueRepository;
import jsdp.sdp.State;
import jsdp.sdp.TransitionProbability;
import jsdp.sdp.impl.univariate.ActionImpl;
import jsdp.sdp.impl.univariate.StateDescriptorImpl;
import jsdp.sdp.impl.univariate.StateImpl;
import jsdp.sdp.impl.univariate.StateSpaceImpl;
import jsdp.sdp.impl.univariate.TransitionProbabilityImpl;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.PoissonDist;

/**
 * Spark-native variant of {@link StochasticLotSizing}.
 *
 * This class keeps the same model structure but uses:
 * - Spark-backed state storage via {@link HashType#SPARK_RDD}
 * - Spark-backed value repository via {@link SparkValueRepository}
 * - Spark execution engine via {@link SparkBackwardRecursion}
 */
public class SparkStochasticLotSizing {

   enum DemandDistribution {
      NORMAL, POISSON
   }

   public static void main(String[] args) {

      SparkConf conf = new SparkConf()
            .setAppName("SparkStochasticLotSizing")
            .setMaster("local[*]");

      try (JavaSparkContext sc = new JavaSparkContext(conf)) {
         sc.setLogLevel("ERROR");

         /*******************************************************************
          * Problem parameters
          */
         double fixedOrderingCost = 10;
         double proportionalOrderingCost = 0;
         double holdingCost = 1;
         double penaltyCost = 10;

         double[] meanDemand = { 2, 4, 6, 4 };
         double coefficientOfVariation = 0.25;
         DemandDistribution demandDistribution = DemandDistribution.POISSON;
         double truncationQuantile = 0.999;

         Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
               .limit(meanDemand.length)
               .mapToObj(i -> (demandDistribution == DemandDistribution.NORMAL)
                     ? new NormalDist(meanDemand[i], meanDemand[i] * coefficientOfVariation)
                     : new PoissonDist(meanDemand[i]))
               .toArray(Distribution[]::new);

         double[] supportLB = IntStream.iterate(0, i -> i + 1)
               .limit(meanDemand.length)
               .mapToDouble(i -> distributions[i].inverseF(1 - truncationQuantile))
               .toArray();

         double[] supportUB = IntStream.iterate(0, i -> i + 1)
               .limit(meanDemand.length)
               .mapToDouble(i -> distributions[i].inverseF(truncationQuantile))
               .toArray();

         double initialInventory = 0;

         /*******************************************************************
          * Model definition
          */
         double stepSize = 1;
         double minState = -10;
         double maxState = 20;
         StateImpl.setStateBoundaries(stepSize, minState, maxState);

         Function<State, ArrayList<Action>> buildActionList = s -> {
            StateImpl state = (StateImpl) s;
            ArrayList<Action> feasibleActions = new ArrayList<Action>();
            for (double i = state.getInitialState(); i <= StateImpl.getMaxState(); i += StateImpl.getStepSize()) {
               feasibleActions.add(new ActionImpl(state, i - state.getInitialState()));
            }
            return feasibleActions;
         };

         Function<State, Action> idempotentAction = s -> new ActionImpl(s, 0.0);

         ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
            ActionImpl a = (ActionImpl) action;
            StateImpl fs = (StateImpl) finalState;
            double orderingCost = a.getAction() > 0
                  ? (fixedOrderingCost + a.getAction() * proportionalOrderingCost)
                  : 0;
            double holdingAndPenaltyCost =
                  holdingCost * Math.max(fs.getInitialState(), 0) + penaltyCost * Math.max(-fs.getInitialState(), 0);
            return orderingCost + holdingAndPenaltyCost;
         };

         RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction = (initialState, action, finalState) -> {
            return ((StateImpl) initialState).getInitialState()
                  + ((ActionImpl) action).getAction()
                  - ((StateImpl) finalState).getInitialState();
         };

         /*******************************************************************
          * Spark-native solve
          */
         int horizonLength = distributions.length;
         StateSpaceImpl[] stateSpace = new StateSpaceImpl[horizonLength + 1];
         for (int i = 0; i < horizonLength + 1; i++) {
            stateSpace[i] = new StateSpaceImpl(i, buildActionList, idempotentAction, HashType.SPARK_RDD, sc);
         }

         TransitionProbability transitionProbability = new TransitionProbabilityImpl(
               distributions,
               supportLB,
               supportUB,
               randomOutcomeFunction,
               stateSpace,
               StateImpl.getStepSize());

         double discountFactor = 1.0;
         SparkValueRepository valueRepository = new SparkValueRepository(
               immediateValueFunction,
               discountFactor,
               sc);

         SparkBackwardRecursion recursion = new SparkBackwardRecursion(
               OptimisationDirection.MIN,
               stateSpace,
               transitionProbability,
               valueRepository);

         System.out.println("--------------Spark backward recursion--------------");
                  recursion.runBackwardRecursionMonitoring();

         StateDescriptorImpl initialDescriptor = new StateDescriptorImpl(0, initialInventory);
         State initialState = stateSpace[0].getState(initialDescriptor);
         double expectedTotalCost = recursion.getExpectedValue(initialState);
         double action = ((ActionImpl) recursion.getValueRepository().getOptimalAction(initialState)).getAction();
                  long percent = recursion.getMonitoringInterfaceBackward().getPercentCPU();

         System.out.println("Expected total cost (initial inventory " + initialInventory + "): " + expectedTotalCost);
         System.out.println("Optimal initial action: " + action);
                  System.out.println("Time elapsed: " + recursion.getMonitoringInterfaceBackward().getTime());
                  System.out.println("Cpu usage: " + percent + "% (" + Runtime.getRuntime().availableProcessors() + " cores)");
      }
   }
}

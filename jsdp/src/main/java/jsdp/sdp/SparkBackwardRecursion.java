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

package jsdp.sdp;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Spark-enabled backward recursion engine.
 *
 * <p>This class is an optional execution engine that uses {@link SparkStateStore}
 * and {@link SparkValueRepository}. Existing engines and storage options remain unchanged.
 */
public class SparkBackwardRecursion extends BackwardRecursion {

   static final Logger logger = LogManager.getLogger(SparkBackwardRecursion.class.getName());
   private final AtomicLong generatedStates = new AtomicLong(0L);
   private final AtomicLong processedStates = new AtomicLong(0L);

   public SparkBackwardRecursion(OptimisationDirection direction,
                                 StateSpace<?>[] stateSpace,
                                 TransitionProbability transitionProbability,
                                 SparkValueRepository valueRepository) {
      super(direction);
      this.stateSpace = stateSpace;
      this.horizonLength = stateSpace.length - 1;
      this.transitionProbability = transitionProbability;
      this.valueRepository = valueRepository;
   }

   @Override
   public void runBackwardRecursion() {
      logger.info("Generating states (Spark engine)...");
      generateStates();
      for (int i = horizonLength - 1; i >= 0; i--) {
         logger.info("Processing period[" + i + "] (Spark engine)...");
         recurse(i);
      }
   }

   @Override
   public void runBackwardRecursion(int period) {
      logger.info("Generating states (Spark engine)...");
      generateStates();
      for (int i = horizonLength - 1; i > period; i--) {
         logger.info("Processing period[" + i + "] (Spark engine)...");
         recurse(i);
      }

      logger.info("Processing period[" + period + "] (Spark engine)...");
      this.getStateSpace(period).entrySet().parallelStream().forEach(entry -> {
         State state = entry.getValue();
         Action bestAction = state.getNoAction();
         double bestCost = this.getValueRepository().getExpectedValue(state, bestAction, this.getTransitionProbability());
         this.getValueRepository().setOptimalExpectedValue(state, bestCost);
         this.getValueRepository().setOptimalAction(state, bestAction);
         updateMonitoring(generatedStates.get(), processedStates.incrementAndGet(), period);
      });
   }

   @Override
   protected void generateStates() {
      for (int i = horizonLength; i >= 0; i--) {
         Iterator<State> iterator = this.getStateSpace(i).iterator();
         if (iterator == null)
            continue;
         while (iterator.hasNext()) {
            iterator.next();
            updateMonitoring(generatedStates.incrementAndGet(), processedStates.get(), i);
         }
      }

      this.getStateSpace(horizonLength).entrySet().parallelStream().forEach(entry -> {
         this.getValueRepository().setOptimalExpectedValue(entry.getValue(), 0);
         updateMonitoring(generatedStates.get(), processedStates.incrementAndGet(), horizonLength);
      });
   }

   @Override
   protected void recurse(int period) {
      this.getStateSpace(period).entrySet().parallelStream().forEach(entry -> {
         State state = entry.getValue();
         BestActionRepository repository = new BestActionRepository(direction);
         state.getFeasibleActions().parallelStream().forEach(action -> {
            double currentCost = this.getValueRepository().getExpectedValue(state, action, this.getTransitionProbability());
            repository.update(action, currentCost);
         });
         this.getValueRepository().setOptimalExpectedValue(state, repository.getBestValue());
         this.getValueRepository().setOptimalAction(state, repository.getBestAction());
         updateMonitoring(generatedStates.get(), processedStates.incrementAndGet(), period);
      });
   }
}

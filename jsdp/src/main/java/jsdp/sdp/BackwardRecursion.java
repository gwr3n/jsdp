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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Logger;

import jsdp.utilities.monitoring.MonitoringInterfaceBackward;

import org.apache.logging.log4j.LogManager;

/**
 * Implements a backward recursion algorithm to solve a stochastic dynamic program.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class BackwardRecursion extends Recursion{
	static final Logger logger = LogManager.getLogger(BackwardRecursion.class.getName());
	
	/**
    * States processed
    */
   private AtomicLong processedStates = new AtomicLong(0L);
   
   /**
    * States generated
    */
   private AtomicLong generatedStates = new AtomicLong(0L);
   
   /**
    * Monitor
    */
   private  MonitoringInterfaceBackward monitor;
	
	/**
	 * Creates an instance of {@code BackwardRecursion} with the given optimization direction.
	 * 
	 * @param direction the direction of optimization.
	 */
	public BackwardRecursion(OptimisationDirection direction){
		super(direction);
	}
	
	/**
    * Runs the backward recursion algorithm for the given stochastic dynamic program.
    */
   public void runBackwardRecursionMonitoring(){
      this.monitor = new MonitoringInterfaceBackward(this);
      this.monitor.startMonitoring();
      runBackwardRecursion();
      this.monitor.terminate();
   }
	
	/**
	 * Runs the backward recursion algorithm for the given stochastic dynamic program.
	 */
	public void runBackwardRecursion(){
		logger.info("Generating states...");
		generateStates();
		for(int i = horizonLength - 1; i >= 0; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
	}
	
	/**
    * Runs the backward recursion algorithm for the given stochastic dynamic program from period {@code period} 
    * up to the end of the planning horizon. This implementation of the backward recursion algorithm assumes 
    * that the idempotent action is selected in period {@code period}.
    * 
    * @param period the starting period. 
    */
   public void runBackwardRecursionMonitoring(int period){
      this.monitor = new MonitoringInterfaceBackward(this);
      this.monitor.startMonitoring();
      runBackwardRecursion(period);
      this.monitor.terminate();
   }
	
	/**
	 * Runs the backward recursion algorithm for the given stochastic dynamic program from period {@code period} 
	 * up to the end of the planning horizon. This implementation of the backward recursion algorithm assumes 
	 * that the idempotent action is selected in period {@code period}.
	 * 
	 * @param period the starting period. 
	 */
	public void runBackwardRecursion(int period){
		logger.info("Generating states...");
		generateStates();
		for(int i = horizonLength - 1; i > period; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
		
		logger.info("Processing period["+period+"]...");
		this.getStateSpace(period).entrySet()
		.parallelStream()
		.forEach(entry -> {
			State state = entry.getValue();
			Action bestAction = state.getNoAction();
			double bestCost = this.getValueRepository().getExpectedValue(state, bestAction, this.getTransitionProbability());
			this.getValueRepository().setOptimalExpectedValue(state, bestCost);
			this.getValueRepository().setOptimalAction(state, bestAction);
         if(stateMonitoring){
            monitor.setStates(generatedStates.get(), processedStates.addAndGet(1L), period);
         }
		});
	}
	
	  /**
    * Generates the complete state space for the discrete time, discrete space, stochastic dynamic program.
    */
   protected void generateStates(){
      CountDownLatch latch = new CountDownLatch(horizonLength);
      for(int i = horizonLength; i >= 0; i--){
         Iterator<State> iterator = this.getStateSpace(i).iterator();
         if(iterator != null){
            Runnable r = () -> {
               while(iterator.hasNext()){ 
                  if(stateMonitoring)
                     monitor.setStates(generatedStates.addAndGet(1L), processedStates.get(), horizonLength);
                  iterator.next();
               }
               latch.countDown();
               };
            new Thread(r).start();
         }else{
            logger.info("Skipping state generation for period "+i);
         }
      }
      try {
         latch.await();
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      this.getStateSpace(horizonLength).entrySet()
          .parallelStream()
          .forEach(entry -> {
             this.getValueRepository().setOptimalExpectedValue(entry.getValue(), 0);
             if(stateMonitoring)
                monitor.setStates(generatedStates.get(), processedStates.addAndGet(1L), horizonLength);
          });
   }
	
	/**
	 * Backward recursion step; in order to run the recursion step for period {@code period} 
	 * the recursion step must have been already run for all subsequent periods.  
	 * 
	 * @param period the target period for the step.
	 */
	protected void recurse(int period){
		this.getStateSpace(period).entrySet()
			.parallelStream()
			.forEach(entry -> {
				State state = entry.getValue();
				BestActionRepository repository = new BestActionRepository(direction);
				state.getFeasibleActions().parallelStream().forEach(action -> {
					double currentCost = this.getValueRepository().getExpectedValue(state, action, this.getTransitionProbability());
					repository.update(action, currentCost);
				});
				this.getValueRepository().setOptimalExpectedValue(state, repository.getBestValue());
				this.getValueRepository().setOptimalAction(state, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestValue());
				if(stateMonitoring)
				   monitor.setStates(generatedStates.get(), processedStates.addAndGet(1L), period);
			});
	}
}

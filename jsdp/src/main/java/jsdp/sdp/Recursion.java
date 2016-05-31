package jsdp.sdp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An abstraction for a recursive solution method for the stochastic dynamic program.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class Recursion {
	
   static final Logger logger = LogManager.getLogger(Recursion.class.getName());
   
	public enum OptimisationDirection {
		MIN,
		MAX
	};
	
	protected int horizonLength;
	protected StateSpace<?>[] stateSpace;
	protected TransitionProbability transitionProbability;
	protected ValueRepository valueRepository;
	final protected OptimisationDirection direction;
	
	/**
	 * Creates an instance of {@code Recursion} with the given optimisatio direction.
	 * 
	 * @param direction the direction of optimisation.
	 */
	protected Recursion(OptimisationDirection direction){
		this.direction = direction; 
	}
	
	/**
	 * Returns the expected value associated with {@code state}.
	 * 
	 * @param state the target state.
	 * @return the expected value associated with {@code state}.
	 */
	public double getExpectedValue(State state){
		return this.getValueRepository().getOptimalExpectedValue(state);
	}
	
	/**
	 * Returns the {@code StateSpace} for period {@code period}.
	 * 
	 * @param period the target period.
	 * @return the {@code StateSpace} for period {@code period}.
	 */
	public StateSpace<?> getStateSpace(int period){
		return this.stateSpace[period];
	}
	
	/**
	 * Returns the {@code StateSpace} array for the stochastic process planning horizon.
	 * 
	 * @return the {@code StateSpace} array for the stochastic process planning horizon.
	 */
	public StateSpace<?>[] getStateSpace(){
		return this.stateSpace;
	}
	
	/**
	 * Returns the {@code TransitionProbability} of the stochastic process.
	 * 
	 * @return the {@code TransitionProbability} of the stochastic process.
	 */
	public TransitionProbability getTransitionProbability(){
		return this.transitionProbability; 
	}
	
	/**
	 * Returns the {@code ValueRepository} of the stochastic process.
	 * 
	 * @return the {@code ValueRepository} of the stochastic process.
	 */
	public ValueRepository getValueRepository(){
		return this.valueRepository;
	}
	
	/**
	 * Stores the best action and its value.
	 * 
	 * @author Roberto Rossi
	 *
	 */
	class BestActionRepository {
		Action bestAction = null;
		double bestValue = Double.MAX_VALUE;
		
		/**
		 * Compares {@code currentAction} and {@code currentValue} to the best action currently stored and updates
		 * values stored accordingly.
		 * 
		 * @param currentAction the action.
		 * @param currentValue the action expected value.
		 */
		public synchronized void update(Action currentAction, double currentValue){
		   if(currentAction == null)
		      throw new NullPointerException("Current action cannot be null");
			switch(direction){
			case MIN:
				if(bestAction == null || Double.isNaN(this.bestValue) || currentValue < bestValue){
					bestValue = currentValue;
					bestAction = currentAction;
				}
				return;
			case MAX:
				if(bestAction == null || Double.isNaN(this.bestValue) || currentValue > bestValue){
					bestValue = currentValue;
					bestAction = currentAction;
				}
				return;
			}
		}
		
		/**
		 * Returns the best action stored.
		 * 
		 * @return the best action stored.
		 */
		public Action getBestAction(){
		   if(Double.isNaN(this.bestValue))
            logger.error("Number of samples probably too low");
			return this.bestAction;
		}
		
		/**
		 * Returns the value associated with the best action stored.
		 * 
		 * @return the value associated with the best action stored.
		 */
		public double getBestValue(){
	       if(Double.isNaN(this.bestValue))
	            logger.error("Number of samples probably too low");
			return this.bestValue;
		}
	}
}

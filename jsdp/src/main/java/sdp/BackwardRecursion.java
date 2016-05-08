package sdp;

import java.util.Enumeration;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public abstract class BackwardRecursion {
	static final Logger logger = LogManager.getLogger(BackwardRecursion.class.getName());
	
	protected int horizonLength;
	protected StateSpace<?>[] stateSpace;
	protected TransitionProbability transitionProbability;
	protected CostRepository costRepository;
	
	public abstract double expectedCost(State state);
	
	public StateSpace<?> getStateSpace(int period){
		return this.stateSpace[period];
	}
	
	public TransitionProbability getTransitionProbability(){
		return this.transitionProbability; 
	}
	
	public CostRepository getCostRepository(){
		return this.costRepository;
	}
	
	public void runBackwardRecursion(){
		logger.info("Generating final states...");
		generateFinalStates();
		for(int i = horizonLength - 1; i >= 0; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
	}
	
	public void runBackwardRecursion(int period){
		logger.info("Generating final states...");
		generateFinalStates();
		for(int i = horizonLength - 1; i > period; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
		
		logger.info("Processing period["+period+"]...");
		Iterator<State> iterator = this.getStateSpace(period).iterator();
		while(iterator.hasNext()){
			State state = iterator.next();
			Action bestAction = state.getNoAction();
			double bestCost = this.getCostRepository().getExpectedCost(state, bestAction, this.getTransitionProbability());
			this.getCostRepository().setOptimalExpectedCost(state, bestCost);
			this.getCostRepository().setOptimalAction(state, bestAction);
		}
	}
	
	protected void recurse(int period){
		Iterator<State> iterator = this.getStateSpace(period).iterator();
		while(iterator.hasNext()){
			State state = iterator.next();
			Action bestAction = null;
			double bestCost = Double.MAX_VALUE;
			Enumeration<Action> actions = state.getPermissibleActions();
			while(actions.hasMoreElements()){
				Action currentAction = actions.nextElement();
				double currentCost = this.getCostRepository().getExpectedCost(state, currentAction, this.getTransitionProbability());
				if(currentCost < bestCost){
					bestCost = currentCost;
					bestAction = currentAction;
				}
			}
			this.getCostRepository().setOptimalExpectedCost(state, bestCost);
			this.getCostRepository().setOptimalAction(state, bestAction);
			
			logger.trace(bestAction+"\tCost: "+bestCost);
		}
	}
	
	protected void generateFinalStates(){
		Iterator<State> iterator = this.getStateSpace(horizonLength).iterator();
		while(iterator.hasNext()){
			State state = iterator.next();
			this.getCostRepository().setOptimalExpectedCost(state, 0);
		}
	}
}

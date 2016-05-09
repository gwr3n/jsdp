package sdp;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

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
		logger.info("Generating states...");
		generateStates();
		for(int i = horizonLength - 1; i >= 0; i--){
			logger.info("Processing period["+i+"]...");
			recurse(i);
		}
	}
	
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
			double bestCost = this.getCostRepository().getExpectedCost(state, bestAction, this.getTransitionProbability());
			this.getCostRepository().setOptimalExpectedCost(state, bestCost);
			this.getCostRepository().setOptimalAction(state, bestAction);
		});
	}
	
	/*protected void recurse(int period){
		this.getStateSpace(period).entrySet()
			.parallelStream()
			.forEach(entry -> {
				State state = entry.getValue();
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
			});
	}*/
	
	class BestActionRepository {
		Action bestAction = null;
		double bestCost = Double.MAX_VALUE;
		
		public synchronized void update(Action currentAction, double currentCost){
			if(currentCost < bestCost){
				bestCost = currentCost;
				bestAction = currentAction;
			}
		}
		
		public Action getBestAction(){
			return this.bestAction;
		}
		
		public double getBestCost(){
			return this.bestCost;
		}
	}
	
	protected void recurse(int period){
		this.getStateSpace(period).entrySet()
			.parallelStream()
			.forEach(entry -> {
				State state = entry.getValue();
				BestActionRepository repository = new BestActionRepository();
				state.getPermissibleActionsStream().forEach(action -> {
					double currentCost = this.getCostRepository().getExpectedCost(state, action, this.getTransitionProbability());
					repository.update(action, currentCost);
				});
				this.getCostRepository().setOptimalExpectedCost(state, repository.getBestCost());
				this.getCostRepository().setOptimalAction(state, repository.getBestAction());
				logger.trace(repository.getBestAction()+"\tCost: "+repository.getBestCost());
			});
	}
	
	protected void generateStates(){
		CountDownLatch latch = new CountDownLatch(horizonLength + 1);
		for(int i = horizonLength; i >= 0; i--){
			Iterator<State> iterator = this.getStateSpace(i).iterator();
			Runnable r = () -> {
				while(iterator.hasNext()) 
					iterator.next();
				latch.countDown();
				};
			new Thread(r).start();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.getStateSpace(horizonLength).entrySet()
			.parallelStream()
			.forEach(entry -> this.getCostRepository().setOptimalExpectedCost(entry.getValue(), 0));
	}
}

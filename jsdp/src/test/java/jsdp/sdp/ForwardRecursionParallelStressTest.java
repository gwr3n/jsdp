package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ForwardRecursionParallelStressTest {

    private static final class SyntheticState extends State {
        private static final long serialVersionUID = 1L;

        private final int node;

        SyntheticState(int period, int node) {
            super(period);
            this.node = node;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SyntheticState)) {
                return false;
            }
            SyntheticState other = (SyntheticState) obj;
            return this.period == other.period && this.node == other.node;
        }

        @Override
        public int hashCode() {
            return 31 * this.period + this.node;
        }
    }

    private static final class SyntheticAction extends Action {
        private static final long serialVersionUID = 1L;

        private final int id;

        SyntheticAction(State state, int id) {
            super(state);
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SyntheticAction)) {
                return false;
            }
            SyntheticAction other = (SyntheticAction) obj;
            return this.id == other.id && this.state.equals(other.state);
        }

        @Override
        public int hashCode() {
            return 31 * this.state.hashCode() + this.id;
        }
    }

    private static final class SyntheticTransitionProbability extends TransitionProbability {

        @Override
        public double getTransitionProbability(State initialState, Action action, State finalState) {
            SyntheticState ini = (SyntheticState) initialState;
            SyntheticState fin = (SyntheticState) finalState;
            if (fin.getPeriod() != ini.getPeriod() + 1) {
                return 0.0;
            }

            int left = ((SyntheticState) initialState).node * 2;
            int right = left + 1;
            if (fin.node == left || fin.node == right) {
                return 0.5;
            }
            return 0.0;
        }

        @Override
        public ArrayList<State> generateFinalStates(State initialState, Action action) {
            SyntheticState ini = (SyntheticState) initialState;
            int nextPeriod = ini.getPeriod() + 1;
            ArrayList<State> finalStates = new ArrayList<>();
            finalStates.add(new SyntheticState(nextPeriod, ini.node * 2));
            finalStates.add(new SyntheticState(nextPeriod, ini.node * 2 + 1));
            return finalStates;
        }

        @Override
        public ArrayList<State> getFinalStates(State initialState, Action action) {
            return generateFinalStates(initialState, action);
        }
    }

    @Test(timeout = 15000)
    public void stressForwardRecursionInParallelWithConcurrentHashMap() throws Exception {
        final int horizon = 6;
        final int threads = 8;
        final int runs = 200;

        ForwardRecursion recursion = new ForwardRecursion(Recursion.OptimisationDirection.MIN) {
        };
        recursion.horizonLength = horizon;
        recursion.transitionProbability = new SyntheticTransitionProbability();
        recursion.valueRepository = new ValueRepository((initialState, action, finalState) -> 1.0, 1.0,
                HashType.CONCURRENT_HASHMAP);

        StateSpace.buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new SyntheticAction(state, 1));
            return actions;
        };
        StateSpace.idempotentAction = state -> new SyntheticAction(state, 0);

        SyntheticState initialState = new SyntheticState(0, 1);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Double>> tasks = new ArrayList<>();
            for (int i = 0; i < runs; i++) {
                tasks.add(() -> recursion.runForwardRecursion(initialState));
            }

            List<Future<Double>> futures = executor.invokeAll(tasks);

            for (Future<Double> future : futures) {
                assertEquals(6.0, future.get().doubleValue(), 1e-9);
            }
            assertTrue(recursion.getValueRepository().containsOptimalExpectedValue(initialState));
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test(timeout = 15000)
    public void stressForwardRecursionInParallelAcrossMultipleInitialStates() throws Exception {
        final int horizon = 6;
        final int threads = 8;
        final int repeatsPerState = 50;

        ForwardRecursion recursion = new ForwardRecursion(Recursion.OptimisationDirection.MIN) {
        };
        recursion.horizonLength = horizon;
        recursion.transitionProbability = new SyntheticTransitionProbability();
        recursion.valueRepository = new ValueRepository((initialState, action, finalState) -> 1.0, 1.0,
                HashType.CONCURRENT_HASHMAP);

        StateSpace.buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new SyntheticAction(state, 1));
            return actions;
        };
        StateSpace.idempotentAction = state -> new SyntheticAction(state, 0);

        List<SyntheticState> initialStates = new ArrayList<>();
        initialStates.add(new SyntheticState(0, 1));
        initialStates.add(new SyntheticState(0, 2));
        initialStates.add(new SyntheticState(0, 3));
        initialStates.add(new SyntheticState(0, 4));

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Double>> tasks = new ArrayList<>();
            Map<Integer, Integer> taskNodeOrder = new HashMap<>();
            int taskId = 0;
            for (SyntheticState initialState : initialStates) {
                for (int i = 0; i < repeatsPerState; i++) {
                    final SyntheticState runState = initialState;
                    final int currentTaskId = taskId;
                    tasks.add(() -> recursion.runForwardRecursion(runState));
                    taskNodeOrder.put(currentTaskId, runState.node);
                    taskId++;
                }
            }

            List<Future<Double>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                assertEquals("Unexpected value for node " + taskNodeOrder.get(i),
                        6.0,
                        futures.get(i).get().doubleValue(),
                        1e-9);
            }

            for (SyntheticState state : initialStates) {
                assertTrue(recursion.getValueRepository().containsOptimalExpectedValue(state));
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}

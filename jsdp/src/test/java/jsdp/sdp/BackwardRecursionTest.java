package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class BackwardRecursionTest {

    private static final class MockState extends State {
        private static final long serialVersionUID = 1L;
        private final int id;

        MockState(int period, int id) {
            super(period);
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MockState)) {
                return false;
            }
            MockState other = (MockState) obj;
            return this.period == other.period && this.id == other.id;
        }

        @Override
        public int hashCode() {
            return 31 * this.period + this.id;
        }
    }

    private static final class MockAction extends Action {
        private static final long serialVersionUID = 1L;
        private final int id;

        MockAction(State state, int id) {
            super(state);
            this.id = id;
        }

        int getId() {
            return this.id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MockAction)) {
                return false;
            }
            MockAction other = (MockAction) obj;
            return this.id == other.id && this.state.equals(other.state);
        }

        @Override
        public int hashCode() {
            return 31 * this.state.hashCode() + this.id;
        }
    }

    private static final class MockStateSpace extends StateSpace<Integer> {

        MockStateSpace(int period) {
            super(period, HashType.HASHTABLE);
        }

        @Override
        public State getState(Integer descriptor) {
            State existing = this.states.get(descriptor);
            if (existing != null) {
                return existing;
            }
            MockState created = new MockState(this.period, descriptor.intValue());
            this.states.put(descriptor, created);
            return created;
        }

        @Override
        public Iterator<State> iterator() {
            return this.states.values().iterator();
        }
    }

    private static final class SingleStepTransitionProbability extends TransitionProbability {
        private final MockState terminal;

        SingleStepTransitionProbability(MockState terminal) {
            this.terminal = terminal;
        }

        @Override
        public double getTransitionProbability(State initialState, Action action, State finalState) {
            return terminal.equals(finalState) ? 1.0 : 0.0;
        }

        @Override
        public ArrayList<State> generateFinalStates(State initialState, Action action) {
            ArrayList<State> states = new ArrayList<>();
            states.add(this.terminal);
            return states;
        }

        @Override
        public ArrayList<State> getFinalStates(State initialState, Action action) {
            return generateFinalStates(initialState, action);
        }
    }

    private static final class LayeredTransitionProbability extends TransitionProbability {
        private final Map<Integer, MockState> terminalByPeriod;

        LayeredTransitionProbability(Map<Integer, MockState> terminalByPeriod) {
            this.terminalByPeriod = terminalByPeriod;
        }

        @Override
        public double getTransitionProbability(State initialState, Action action, State finalState) {
            MockState expected = this.terminalByPeriod.get(Integer.valueOf(initialState.getPeriod()));
            return expected != null && expected.equals(finalState) ? 1.0 : 0.0;
        }

        @Override
        public ArrayList<State> generateFinalStates(State initialState, Action action) {
            ArrayList<State> states = new ArrayList<>();
            MockState expected = this.terminalByPeriod.get(Integer.valueOf(initialState.getPeriod()));
            if (expected != null) {
                states.add(expected);
            }
            return states;
        }

        @Override
        public ArrayList<State> getFinalStates(State initialState, Action action) {
            return generateFinalStates(initialState, action);
        }
    }

    private MockStateSpace period0;
    private MockStateSpace period1;
    private MockState initial;
    private MockState terminal;
    private BackwardRecursion recursion;

    @Before
    public void setUp() {
        period0 = new MockStateSpace(0);
        period1 = new MockStateSpace(1);

        initial = (MockState) period0.getState(Integer.valueOf(10));
        terminal = (MockState) period1.getState(Integer.valueOf(20));

        StateSpace.buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new MockAction(state, 1));
            return actions;
        };
        StateSpace.idempotentAction = state -> new MockAction(state, 0);

        recursion = new BackwardRecursion(Recursion.OptimisationDirection.MIN) {
        };
        recursion.horizonLength = 1;
        recursion.stateSpace = new StateSpace<?>[] { period0, period1 };
        recursion.transitionProbability = new SingleStepTransitionProbability(terminal);
        recursion.valueRepository = new ValueRepository((initialState, action, finalState) -> {
            MockAction mockAction = (MockAction) action;
            return mockAction.getId() == 0 ? 2.0 : 5.0;
        }, 1.0, HashType.HASHTABLE);
    }

    @Test
    public void testRunBackwardRecursion() {
        recursion.runBackwardRecursion();

        assertEquals(0.0, recursion.getExpectedValue(terminal), 1e-9);
        assertEquals(5.0, recursion.getExpectedValue(initial), 1e-9);

        Action bestAction = recursion.getValueRepository().getOptimalAction(initial);
        assertNotNull(bestAction);
        assertSame(initial, bestAction.getState());
        assertEquals(1, ((MockAction) bestAction).getId());
    }

    @Test
    public void testRunBackwardRecursionFromSpecificPeriodUsesNoAction() {
        recursion.runBackwardRecursion(0);

        assertEquals(2.0, recursion.getExpectedValue(initial), 1e-9);
        Action chosenAction = recursion.getValueRepository().getOptimalAction(initial);
        assertEquals(0, ((MockAction) chosenAction).getId());
    }

    @Test
    public void testMonitoringGetterIsNullWhenNotStarted() {
        assertNull(recursion.getMonitoringInterfaceBackward());
    }

    @Test
    public void testRunBackwardRecursionFromIntermediatePeriod() {
        MockStateSpace p0 = new MockStateSpace(0);
        MockStateSpace p1 = new MockStateSpace(1);
        MockStateSpace p2 = new MockStateSpace(2);

        MockState s0 = (MockState) p0.getState(Integer.valueOf(1));
        MockState s1 = (MockState) p1.getState(Integer.valueOf(2));
        MockState s2 = (MockState) p2.getState(Integer.valueOf(3));

        Map<Integer, MockState> nextStateMap = new HashMap<>();
        nextStateMap.put(Integer.valueOf(0), s1);
        nextStateMap.put(Integer.valueOf(1), s2);

        BackwardRecursion layeredRecursion = new BackwardRecursion(Recursion.OptimisationDirection.MIN) {
        };
        layeredRecursion.horizonLength = 2;
        layeredRecursion.stateSpace = new StateSpace<?>[] { p0, p1, p2 };
        layeredRecursion.transitionProbability = new LayeredTransitionProbability(nextStateMap);
        layeredRecursion.valueRepository = new ValueRepository((initialState, action, finalState) -> {
            MockAction a = (MockAction) action;
            return a.getId() == 0 ? 2.0 : 5.0;
        }, 1.0, HashType.HASHTABLE);

        layeredRecursion.runBackwardRecursion(0);

        Action p1Action = layeredRecursion.getValueRepository().getOptimalAction(s1);
        assertNotNull(p1Action);
        assertEquals(1, ((MockAction) p1Action).getId());

        Action p0Action = layeredRecursion.getValueRepository().getOptimalAction(s0);
        assertNotNull(p0Action);
        assertEquals(0, ((MockAction) p0Action).getId());
    }

    @Test
    public void testMonitoringRunsWhenGraphicsAvailable() {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());

        recursion.runBackwardRecursionMonitoring();
        assertNotNull(recursion.getMonitoringInterfaceBackward());

        recursion.runBackwardRecursionMonitoring(0);
        assertNotNull(recursion.getMonitoringInterfaceBackward());
    }
}

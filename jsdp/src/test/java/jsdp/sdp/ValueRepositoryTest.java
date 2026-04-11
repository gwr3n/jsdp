package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ValueRepositoryTest {

    // Helper Mock State
    private static class MockState extends State {
        private static final long serialVersionUID = 1L;
        private final String id;

        public MockState(int period, String id) {
            super(period);
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            MockState state = (MockState) obj;
            return period == state.period && id.equals(state.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode() + period;
        }
    }

    // Helper Mock Action
    private static class MockAction extends Action {
        private static final long serialVersionUID = 1L;
        private final String actionId;

        public MockAction(State state, String actionId) {
            super(state);
            this.actionId = actionId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            MockAction action = (MockAction) obj;
            return state.equals(action.state) && actionId.equals(action.actionId);
        }

        @Override
        public int hashCode() {
            return actionId.hashCode() + state.hashCode();
        }
    }

    private ImmediateValueFunction<State, Action, Double> mockImmediateValueFunction;
    private ValueRepository repository;

    @AfterClass
    public static void cleanUpTablesDirectory() throws Exception {
        File tablesDir = new File("tables");
        if (tablesDir.exists() && tablesDir.listFiles() != null) {
            for (File f : tablesDir.listFiles()) {
                String name = f.getName();
                if (name.startsWith("valueHashTable")
                        || name.startsWith("optimalValueHashTable")
                        || name.startsWith("optimalActionHashTable")) {
                    Files.deleteIfExists(f.toPath());
                }
            }
            String[] remaining = tablesDir.list();
            if (remaining != null && remaining.length == 0) {
                tablesDir.delete();
            }
        }
    }

    @Before
    public void setUp() {
        mockImmediateValueFunction = (initialState, action, finalState) -> {
            // Simple mock immediate value: 10.0 for all steps
            return 10.0;
        };

        // Initialize repository with HASHTABLE (simplest mock environment) and a
        // discount factor of 0.95
        repository = new ValueRepository(mockImmediateValueFunction, 0.95, HashType.HASHTABLE);
    }

    @Test
    public void testOptimalValueAndActionStorage() {
        MockState state = new MockState(1, "S1");
        MockAction optimalAction = new MockAction(state, "A1");

        repository.setOptimalExpectedValue(state, 123.45);
        repository.setOptimalAction(state, optimalAction);

        assertEquals(123.45, repository.getOptimalExpectedValue(state), 1e-6);
        assertSame(optimalAction, repository.getOptimalAction(state));
    }

    @Test
    public void testExpectedValueCalculation() {
        MockState initialState = new MockState(1, "S1");
        MockState finalState1 = new MockState(2, "S2");
        MockState finalState2 = new MockState(2, "S3");
        MockAction action = new MockAction(initialState, "A1");

        // Set up the final states expected optimal value so they factor into Bellman
        // calculation
        repository.setOptimalExpectedValue(finalState1, 100.0);
        repository.setOptimalExpectedValue(finalState2, 200.0);

        // Dummy Transition Probability yielding 50/50 split to final states
        TransitionProbability mockProbability = new TransitionProbability() {
            @Override
            public double getTransitionProbability(State ini, Action a, State fin) {
                if (fin.equals(finalState1))
                    return 0.5;
                if (fin.equals(finalState2))
                    return 0.5;
                return 0.0;
            }

            @Override
            public ArrayList<State> generateFinalStates(State ini, Action a) {
                ArrayList<State> states = new ArrayList<>();
                states.add(finalState1);
                states.add(finalState2);
                return states;
            }

            @Override
            public ArrayList<State> getFinalStates(State ini, Action a) {
                ArrayList<State> states = new ArrayList<>();
                states.add(finalState1);
                states.add(finalState2);
                return states;
            }
        };

        double expectedValue = repository.getExpectedValue(initialState, action, mockProbability);

        // Expected Value = 0.5 * (10.0 + 0.95 * 100.0) + 0.5 * (10.0 + 0.95 * 200.0)
        // Expected Value = 0.5 * (105.0) + 0.5 * (200.0)
        // Expected Value = 52.5 + 100.0 = 152.5
        assertEquals(152.5, expectedValue, 1e-6);

        // Verify that the computed value is now stored and retrievable as a cached
        // expected value
        assertEquals(Double.valueOf(152.5), repository.getValueHashTable().get(new StateAction(initialState, action)));
    }

    @Test
    public void testContainsOptimalExpectedValue() {
        MockState state = new MockState(1, "S1");
        assertEquals(false, repository.containsOptimalExpectedValue(state));

        repository.setOptimalExpectedValue(state, 77.0);
        assertEquals(true, repository.containsOptimalExpectedValue(state));
    }

    @Test
    public void testConcurrentHashMapOptimalValuesAreStoredByPeriod() {
        ValueRepository concurrentRepository =
                new ValueRepository(mockImmediateValueFunction, 0.95, HashType.CONCURRENT_HASHMAP);

        MockState period0State = new MockState(0, "S");
        MockState period1State = new MockState(1, "S");

        concurrentRepository.setOptimalExpectedValue(period0State, 11.0);
        concurrentRepository.setOptimalExpectedValue(period1State, 22.0);

        assertEquals(11.0, concurrentRepository.getOptimalExpectedValue(period0State), 1e-6);
        assertEquals(22.0, concurrentRepository.getOptimalExpectedValue(period1State), 1e-6);
        assertEquals(true, concurrentRepository.containsOptimalExpectedValue(period0State));
        assertEquals(true, concurrentRepository.containsOptimalExpectedValue(period1State));

        assertEquals(2, concurrentRepository.optimalValueHashTableByPeriod.size());
    }

    @Test
    public void testConstructorsAndAccessorsAcrossHashTypes() {
        MockState state = new MockState(0, "S0");
        MockState finalState = new MockState(1, "S1");
        MockAction action = new MockAction(state, "A");

        for (HashType hashType : HashType.values()) {
            ValueRepository repositoryByType = new ValueRepository(mockImmediateValueFunction, 0.9, hashType);
            repositoryByType.setOptimalExpectedValue(state, 12.0);
            repositoryByType.setOptimalAction(state, action);

            assertEquals(12.0, repositoryByType.getOptimalExpectedValue(state), 1e-9);
            assertEquals(action, repositoryByType.getOptimalAction(state));
            assertTrue(repositoryByType.containsOptimalExpectedValue(state));
            assertEquals(0.9, repositoryByType.getDiscountFactor(), 1e-9);
            assertEquals(10.0, repositoryByType.getImmediateValue(state, action, finalState), 1e-9);
            assertEquals(Double.valueOf(12.0), repositoryByType.getOptimalValueHashTable().get(state));
         }
    }

    @Test
    public void testSizedConstructorAndImmediateValueOverride() {
        ValueRepository sizedRepository =
                new ValueRepository(mockImmediateValueFunction, 0.95, 64, 0.75f, HashType.THASHMAP);

        MockState state = new MockState(0, "S0");
        MockAction action = new MockAction(state, "A0");
        MockState finalState = new MockState(1, "S1");

        sizedRepository.setImmediateValue((initialState, a, fin) -> 42.0);
        sizedRepository.setOptimalExpectedValue(finalState, 5.0);

        assertEquals(42.0, sizedRepository.getImmediateValue(state, action, finalState), 1e-9);
        assertEquals(5.0, sizedRepository.getOptimalExpectedValue(finalState), 1e-9);
    }

    @Test
    public void testExpectedValueCacheHitPath() {
        MockState initialState = new MockState(0, "S0");
        MockState finalState = new MockState(1, "S1");
        MockAction action = new MockAction(initialState, "A0");

        repository.setOptimalExpectedValue(finalState, 20.0);

        TransitionProbability deterministicProbability = new TransitionProbability() {
            @Override
            public double getTransitionProbability(State ini, Action a, State fin) {
                return fin.equals(finalState) ? 1.0 : 0.0;
            }

            @Override
            public ArrayList<State> generateFinalStates(State ini, Action a) {
                ArrayList<State> states = new ArrayList<>();
                states.add(finalState);
                return states;
            }

            @Override
            public ArrayList<State> getFinalStates(State ini, Action a) {
                ArrayList<State> states = new ArrayList<>();
                states.add(finalState);
                return states;
            }
        };

        double first = repository.getExpectedValue(initialState, action, deterministicProbability);
        int cacheSize = repository.getValueHashTable().size();
        double second = repository.getExpectedValue(initialState, action, deterministicProbability);

        assertEquals(first, second, 1e-9);
        assertEquals(cacheSize, repository.getValueHashTable().size());
    }

    @Test
    public void testSizedConstructorAcrossAllHashTypes() {
        MockState state = new MockState(2, "S2");
        MockAction action = new MockAction(state, "A2");

        for (HashType hashType : HashType.values()) {
            ValueRepository sizedRepository =
                    new ValueRepository(mockImmediateValueFunction, 0.8, 32, 0.75f, hashType);
            sizedRepository.setOptimalExpectedValue(state, 9.0);
            sizedRepository.setOptimalAction(state, action);

            assertEquals(9.0, sizedRepository.getOptimalExpectedValue(state), 1e-9);
            assertEquals(action, sizedRepository.getOptimalAction(state));
            assertTrue(sizedRepository.containsOptimalExpectedValue(state));
            assertNotNull(sizedRepository.getOptimalActionHashTable());
            assertNotNull(sizedRepository.getOptimalValueHashTable());
            assertNotNull(sizedRepository.getValueHashTable());
        }
    }
}

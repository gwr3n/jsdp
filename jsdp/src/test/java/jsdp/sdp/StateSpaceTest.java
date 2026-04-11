package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.Test;

public class StateSpaceTest {

    @AfterClass
    public static void cleanUpTablesDirectory() throws Exception {
        File tablesDir = new File("tables");
        if (tablesDir.exists() && tablesDir.listFiles() != null) {
            for (File f : tablesDir.listFiles()) {
                if (f.getName().startsWith("states")) {
                    Files.deleteIfExists(f.toPath());
                }
            }
            String[] remaining = tablesDir.list();
            if (remaining != null && remaining.length == 0) {
                tablesDir.delete();
            }
        }
    }

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

        MockAction(State state) {
            super(state);
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
            return this.state.equals(other.state);
        }

        @Override
        public int hashCode() {
            return this.state.hashCode();
        }
    }

    private static final class MockStateSpace extends StateSpace<Integer> {

        MockStateSpace(int period, HashType hashType) {
            super(period, hashType);
        }

        MockStateSpace(int period, HashType hashType, int stateSpaceSizeLowerBound, float loadFactor) {
            super(period, hashType, stateSpaceSizeLowerBound, loadFactor);
        }

        @Override
        public State getState(Integer descriptor) {
            State existing = this.states.get(descriptor);
            if (existing != null) {
                return existing;
            }
            State created = new MockState(this.period, descriptor.intValue());
            this.states.put(descriptor, created);
            return created;
        }

        @Override
        public Iterator<State> iterator() {
            return this.states.values().iterator();
        }
    }

    @Test
    public void testGetStateAndEntrySetAndPeriod() {
        MockStateSpace stateSpace = new MockStateSpace(3, HashType.HASHTABLE);

        State state = stateSpace.getState(Integer.valueOf(42));
        State sameState = stateSpace.getState(Integer.valueOf(42));

        assertSame(state, sameState);
        assertEquals(3, stateSpace.getPeriod());
        assertEquals(1, stateSpace.entrySet().size());
    }

    @Test
    public void testStaticActionFunctionsAreUsedByState() {
        MockStateSpace stateSpace = new MockStateSpace(0, HashType.CONCURRENT_HASHMAP);
        MockState state = (MockState) stateSpace.getState(Integer.valueOf(1));

        StateSpace.buildActionList = s -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new MockAction(s));
            return actions;
        };
        StateSpace.idempotentAction = MockAction::new;

        assertNotNull(StateSpace.getBuildActionList());
        assertNotNull(StateSpace.getIdempotentAction());
        assertEquals(1, state.getFeasibleActions().size());
        assertNotNull(state.getNoAction());
    }

    @Test
    public void testConstructorsAcrossHashTypes() {
        for (HashType hashType : HashType.values()) {
            MockStateSpace plain = new MockStateSpace(1, hashType);
            MockStateSpace sized = new MockStateSpace(2, hashType, 64, 0.75f);

            State plainState = plain.getState(Integer.valueOf(7));
            State sizedState = sized.getState(Integer.valueOf(8));

            assertNotNull(plainState);
            assertNotNull(sizedState);
            assertEquals(1, plain.entrySet().size());
            assertEquals(1, sized.entrySet().size());
            assertTrue(plain.iterator().hasNext());
            assertTrue(sized.iterator().hasNext());
        }
    }

    @Test
    public void testEntrySetAndIteratorAcrossHashTypes() {
        for (HashType hashType : HashType.values()) {
            MockStateSpace stateSpace = new MockStateSpace(4, hashType);
            stateSpace.getState(Integer.valueOf(1));
            stateSpace.getState(Integer.valueOf(2));
            stateSpace.getState(Integer.valueOf(3));

            assertEquals(3, stateSpace.entrySet().size());

            int iterated = 0;
            Iterator<State> iterator = stateSpace.iterator();
            while (iterator.hasNext()) {
                State next = iterator.next();
                assertNotNull(next);
                iterated++;
            }
            assertEquals(3, iterated);
        }
    }
}

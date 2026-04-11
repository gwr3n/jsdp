package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StateActionTest {

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

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsAndHashCode() {
        MockState state1 = new MockState(1, "S1");
        MockState state2 = new MockState(2, "S2");

        MockAction action1 = new MockAction(state1, "A1");
        MockAction action2 = new MockAction(state1, "A2");

        StateAction sa1 = new StateAction(state1, action1);
        StateAction sa1Duplicate = new StateAction(state1, new MockAction(state1, "A1"));

        StateAction sa2 = new StateAction(state1, action2);
        StateAction sa3 = new StateAction(state2, new MockAction(state2, "A1"));

        // Test equality
        assertTrue(sa1.equals(sa1Duplicate));
        assertFalse(sa1.equals(sa2));
        assertFalse(sa1.equals(sa3));

        // Test hashCode consistency
        assertEquals(sa1.hashCode(), sa1Duplicate.hashCode());
        assertNotEquals(sa1.hashCode(), sa2.hashCode());

        // Additional equals branches
        assertTrue(sa1.equals(sa1));
        assertFalse(sa1.equals(null));
        assertFalse(sa1.equals("not-a-state-action"));
    }

    @Test(expected = NullPointerException.class)
    public void testEqualsWithNullInitialStateThrows() {
        MockState state1 = new MockState(1, "S1");
        MockAction action1 = new MockAction(state1, "A1");

        StateAction invalid = new StateAction(null, action1);
        StateAction other = new StateAction(state1, action1);

        invalid.equals(other);
    }

    @Test(expected = NullPointerException.class)
    public void testEqualsWithNullActionThrows() {
        MockState state1 = new MockState(1, "S1");
        MockAction action1 = new MockAction(state1, "A1");

        StateAction invalid = new StateAction(state1, null);
        StateAction other = new StateAction(state1, action1);

        invalid.equals(other);
    }
}

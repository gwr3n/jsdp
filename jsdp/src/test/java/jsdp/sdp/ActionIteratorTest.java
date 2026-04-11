package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ActionIteratorTest {

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

    private static final class ListActionIterator extends ActionIterator {
        private final List<Action> actions;
        private int pointer = 0;

        ListActionIterator(List<Action> actions) {
            this.actions = actions;
        }

        @Override
        public boolean hasNext() {
            return this.pointer < this.actions.size();
        }

        @Override
        public Action next() {
            if (!hasNext()) {
                return null;
            }
            Action action = this.actions.get(this.pointer);
            this.pointer++;
            return action;
        }
    }

    @Test
    public void testIteratesActionsInOrder() {
        MockState state = new MockState(0, 1);
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new MockAction(state, 10));
        actions.add(new MockAction(state, 20));

        ActionIterator iterator = new ListActionIterator(actions);

        assertTrue(iterator.hasNext());
        assertEquals(10, ((MockAction) iterator.next()).getId());
        assertTrue(iterator.hasNext());
        assertEquals(20, ((MockAction) iterator.next()).getId());
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }
}

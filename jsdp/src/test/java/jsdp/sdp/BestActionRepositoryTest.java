package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class BestActionRepositoryTest {

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

    @Test
    public void testUpdateWithMinDirection() {
        BestActionRepository repository = new BestActionRepository(Recursion.OptimisationDirection.MIN);
        MockState state = new MockState(0, 1);
        MockAction action1 = new MockAction(state, 1);
        MockAction action2 = new MockAction(state, 2);

        repository.update(action1, 10.0);
        repository.update(action2, 7.5);

        assertSame(action2, repository.getBestAction());
        assertEquals(7.5, repository.getBestValue(), 1e-9);
    }

    @Test
    public void testUpdateWithMaxDirection() {
        BestActionRepository repository = new BestActionRepository(Recursion.OptimisationDirection.MAX);
        MockState state = new MockState(0, 1);
        MockAction action1 = new MockAction(state, 1);
        MockAction action2 = new MockAction(state, 2);

        repository.update(action1, 4.0);
        repository.update(action2, 9.0);

        assertSame(action2, repository.getBestAction());
        assertEquals(9.0, repository.getBestValue(), 1e-9);
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateRejectsNullAction() {
        BestActionRepository repository = new BestActionRepository(Recursion.OptimisationDirection.MIN);
        repository.update(null, 1.0);
    }

    @Test
    public void testNanBestValueIsReplaced() {
        BestActionRepository repository = new BestActionRepository(Recursion.OptimisationDirection.MIN);
        MockState state = new MockState(0, 1);
        MockAction action = new MockAction(state, 1);

        repository.bestValue = Double.NaN;
        repository.update(action, 3.0);

        assertSame(action, repository.getBestAction());
        assertEquals(3.0, repository.getBestValue(), 1e-9);
    }
}

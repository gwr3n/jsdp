package jsdp.sdp.impl.multivariate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class StateImplAndDescriptorTest {

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(new double[] { 1.0, 2.0 }, new double[] { 0.0, -2.0 }, new double[] { 3.0, 4.0 });
    }

    @Test
    public void testGetMinAndMaxState() {
        assertArrayEquals(new double[] { 0.0, -2.0 }, StateImpl.getMinState(), 0.0);
        assertArrayEquals(new double[] { 3.0, 4.0 }, StateImpl.getMaxState(), 0.0);
    }

    @Test
    public void testStateToIntStateClamps() {
        assertArrayEquals(new int[] { 0, -1 }, StateImpl.stateToIntState(new double[] { -10.0, -10.0 }));
        assertArrayEquals(new int[] { 3, 2 }, StateImpl.stateToIntState(new double[] { 10.0, 10.0 }));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testStateConstructorEqualityAndHashCode() {
        StateImpl first = new StateImpl(new StateDescriptorImpl(1, new int[] { 1, 2 }));
        StateImpl same = new StateImpl(new StateDescriptorImpl(1, new double[] { 1.0, 4.0 }));
        StateImpl different = new StateImpl(new StateDescriptorImpl(0, new int[] { 1, 2 }));

        assertArrayEquals(new int[] { 1, 2 }, first.getInitialIntState());
        assertArrayEquals(new double[] { 1.0, 4.0 }, first.getInitialState(), 0.0);
        assertTrue(first.equals(same));
        assertTrue(first.hashCode() == same.hashCode());
        assertFalse(first.equals(different));
        assertFalse(first.equals("not a state"));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testDescriptorEqualityBranches() {
        StateDescriptorImpl first = new StateDescriptorImpl(1, new int[] { 1, 2 });
        StateDescriptorImpl same = new StateDescriptorImpl(1, new double[] { 1.0, 4.0 });

        assertTrue(first.equals(same));
        assertTrue(first.hashCode() == same.hashCode());
        assertFalse(first.equals(new StateDescriptorImpl(2, new int[] { 1, 2 })));
        assertFalse(first.equals(new StateDescriptorImpl(1, new int[] { 1, 1 })));
        assertFalse(first.equals("not a descriptor"));
    }

    @Test(expected = NullPointerException.class)
    public void testSetStateBoundariesRejectsMismatchedLengths() {
        StateImpl.setStateBoundaries(new double[] { 1.0 }, new double[] { 0.0, 1.0 }, new double[] { 2.0, 3.0 });
    }
}
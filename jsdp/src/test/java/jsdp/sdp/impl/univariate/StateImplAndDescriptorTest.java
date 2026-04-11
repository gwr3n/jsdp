package jsdp.sdp.impl.univariate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class StateImplAndDescriptorTest {

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(1.0, -2.0, 3.0);
    }

    // ---- StateImpl ----

    @Test
    public void testGetMinAndMaxState() {
        assertEquals(-2.0, StateImpl.getMinState(), 1e-9);
        assertEquals(3.0, StateImpl.getMaxState(), 1e-9);
    }

    @Test
    public void testStepSize() {
        assertEquals(1.0, StateImpl.getStepSize(), 1e-9);
    }

    @Test
    public void testStepToIntStateClampsBelowMin() {
        int result = StateImpl.stateToIntState(-100.0);
        assertEquals(StateImpl.getMinIntState(), result);
    }

    @Test
    public void testStepToIntStateClampsAboveMax() {
        int result = StateImpl.stateToIntState(100.0);
        assertEquals(StateImpl.getMaxIntState(), result);
    }

    @Test
    public void testIntStateToStateRoundTrip() {
        assertEquals(2.0, StateImpl.intStateToState(2), 1e-9);
        assertEquals(-1.0, StateImpl.intStateToState(-1), 1e-9);
    }

    // ---- StateDescriptorImpl ----

    @Test
    public void testEqualsIdentical() {
        StateDescriptorImpl d1 = new StateDescriptorImpl(0, 0);
        StateDescriptorImpl d2 = new StateDescriptorImpl(0, 0);
        assertTrue(d1.equals(d2));
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    public void testEqualsWithSamePeriodDifferentState() {
        StateDescriptorImpl d1 = new StateDescriptorImpl(0, 0);
        StateDescriptorImpl d2 = new StateDescriptorImpl(0, 1);
        assertFalse(d1.equals(d2));
    }

    @Test
    public void testEqualsWithDifferentPeriodSameState() {
        StateDescriptorImpl d1 = new StateDescriptorImpl(0, 0);
        StateDescriptorImpl d2 = new StateDescriptorImpl(1, 0);
        assertFalse(d1.equals(d2));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsWithNonDescriptor() {
        StateDescriptorImpl d = new StateDescriptorImpl(0, 0);
        assertFalse(d.equals("not-a-descriptor"));
    }

    @Test
    public void testDoubleConstructorClampsToMin() {
        StateDescriptorImpl d = new StateDescriptorImpl(0, -100.0);
        assertEquals(StateImpl.getMinIntState(), d.getInitialIntState());
    }

    @Test
    public void testDoubleConstructorNormalPath() {
        StateDescriptorImpl d = new StateDescriptorImpl(0, 1.0);
        assertEquals(1, d.getInitialIntState());
    }
}

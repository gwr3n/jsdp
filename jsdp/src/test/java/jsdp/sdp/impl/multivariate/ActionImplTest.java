package jsdp.sdp.impl.multivariate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import jsdp.sdp.State;

public class ActionImplTest {

    private State state;

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(new double[] { 1.0, 2.0 }, new double[] { 0.0, -2.0 }, new double[] { 3.0, 4.0 });
        state = new StateImpl(new StateDescriptorImpl(0, new int[] { 1, 1 }));
    }

    @Test
    public void testIntActionToActionAndBack() {
        assertArrayEquals(new double[] { 2.0, 2.0 }, ActionImpl.intActionToAction(new int[] { 2, 1 }), 0.0);
        assertArrayEquals(new int[] { 2, 1 }, ActionImpl.actionToIntAction(new double[] { 2.0, 2.0 }));
    }

    @Test(expected = NullPointerException.class)
    public void testIntActionToActionRejectsWrongSizedArray() {
        ActionImpl.intActionToAction(new int[] { 1 });
    }

    @Test(expected = NullPointerException.class)
    public void testActionToIntActionRejectsWrongSizedArray() {
        ActionImpl.actionToIntAction(new double[] { 1.0 });
    }

    @Test
    public void testDoubleConstructorAndGetters() {
        ActionImpl action = new ActionImpl(state, new double[] { 1.0, 2.0 });

        assertArrayEquals(new int[] { 1, 1 }, action.getIntAction());
        assertArrayEquals(new double[] { 1.0, 2.0 }, action.getAction(), 0.0);
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualityBranchesAndToString() {
        ActionImpl first = new ActionImpl(state, new int[] { 1, 1 });
        ActionImpl same = new ActionImpl(state, new int[] { 1, 1 });
        ActionImpl different = new ActionImpl(state, new int[] { 1, 0 });

        assertTrue(first.equals(first));
        assertTrue(first.equals(same));
        assertTrue(first.hashCode() == same.hashCode());
        assertFalse(first.equals(different));
        assertFalse(first.equals("not an action"));
        assertTrue(first.toString().contains("Action"));
    }
}
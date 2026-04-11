package jsdp.sdp.impl.univariate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.State;

public class ActionImplTest {

    private State state;

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(1.0, 0.0, 5.0);
        Function<State, ArrayList<Action>> buildActionList = s -> new ArrayList<>();
        Function<State, Action> idempotentAction = s -> new ActionImpl(s, 0);
        StateSpaceImpl stateSpace = new StateSpaceImpl(0, buildActionList, idempotentAction, HashType.HASHTABLE);
        state = stateSpace.getState(new StateDescriptorImpl(0, 0));
    }

    @Test
    public void testIntActionToActionAndActionToIntActionRoundTrip() {
        assertEquals(3.0, ActionImpl.intActionToAction(3), 1e-9);
        assertEquals(3, ActionImpl.actionToIntAction(3.0));
    }

    @Test
    public void testDoubleConstructorConvertsViaStepSize() {
        ActionImpl a = new ActionImpl(state, 2.0);
        assertEquals(2.0, a.getAction(), 1e-9);
        assertEquals(2, a.getIntAction());
    }

    @Test
    public void testEqualsReflexive() {
        ActionImpl a = new ActionImpl(state, 1);
        assertTrue(a.equals(a));
    }

    @Test
    public void testEqualsCopies() {
        ActionImpl a1 = new ActionImpl(state, 1);
        ActionImpl a2 = new ActionImpl(state, 1);
        assertTrue(a1.equals(a2));
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    public void testEqualsNotEqualIntAction() {
        ActionImpl a1 = new ActionImpl(state, 1);
        ActionImpl a2 = new ActionImpl(state, 2);
        assertFalse(a1.equals(a2));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsNonActionImpl() {
        ActionImpl a = new ActionImpl(state, 1);
        assertFalse(a.equals("not an action"));
    }

    @Test
    public void testToString() {
        ActionImpl a = new ActionImpl(state, 1);
        assertNotNull(a.toString());
        assertTrue(a.toString().contains("Action"));
    }
}

package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class SparkBackwardRecursionTest {

   private JavaSparkContext sc;

   @Before
   public void setUp() {
      try {
         SparkConf conf = new SparkConf().setAppName("SparkBackwardRecursionTest").setMaster("local");
         sc = new JavaSparkContext(conf);
         sc.setLogLevel("ERROR");
      } catch (UnsupportedOperationException e) {
         Assume.assumeTrue("Skipping SparkBackwardRecursionTest: Spark is incompatible with this JDK version: " + e.getMessage(), false);
      }
   }

   @After
   public void tearDown() {
      if (sc != null)
         sc.close();
   }

   private static final class MockState extends State {
      private static final long serialVersionUID = 1L;
      private final int id;

      MockState(int period, int id) {
         super(period);
         this.id = id;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o)
            return true;
         if (!(o instanceof MockState))
            return false;
         MockState other = (MockState) o;
         return this.period == other.period && this.id == other.id;
      }

      @Override
      public int hashCode() {
         return 31 * period + id;
      }
   }

   private static final class MockAction extends Action {
      private static final long serialVersionUID = 1L;
      private final String name;

      MockAction(State state, String name) {
         super(state);
         this.name = name;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o)
            return true;
         if (!(o instanceof MockAction))
            return false;
         MockAction other = (MockAction) o;
         return this.state.equals(other.state) && this.name.equals(other.name);
      }

      @Override
      public int hashCode() {
         return 31 * this.state.hashCode() + this.name.hashCode();
      }
   }

   private static final class MockStateSpace extends StateSpace<Integer> {

      private final int[] descriptors;

      MockStateSpace(int period, int[] descriptors, JavaSparkContext sc) {
         super(period, HashType.SPARK_RDD, sc);
         this.descriptors = descriptors;
      }

      @Override
      public State getState(Integer descriptor) {
         State existing = stateStore.get(descriptor);
         if (existing != null)
            return existing;
         State created = new MockState(period, descriptor.intValue());
         stateStore.put(descriptor, created);
         return created;
      }

      @Override
      public Iterator<State> iterator() {
         return Arrays.stream(descriptors).mapToObj(d -> getState(Integer.valueOf(d))).iterator();
      }
   }

   private static final class MockTransitionProbability extends TransitionProbability {

      private final State a;
      private final State b;

      MockTransitionProbability(State a, State b) {
         this.a = a;
         this.b = b;
      }

      @Override
      public double getTransitionProbability(State initialState, Action action, State finalState) {
         MockAction act = (MockAction) action;
         if (act.name.equals("a1"))
            return finalState.equals(a) ? 1.0 : 0.0;
         if (act.name.equals("a2"))
            return finalState.equals(b) ? 1.0 : 0.0;
         return finalState.equals(a) ? 1.0 : 0.0;
      }

      @Override
      public ArrayList<State> generateFinalStates(State initialState, Action action) {
         return getFinalStates(initialState, action);
      }

      @Override
      public ArrayList<State> getFinalStates(State initialState, Action action) {
         MockAction act = (MockAction) action;
         ArrayList<State> out = new ArrayList<>();
         if (act.name.equals("a1"))
            out.add(a);
         else if (act.name.equals("a2"))
            out.add(b);
         else
            out.add(a);
         return out;
      }
   }

   @Test
   public void testSparkBackwardRecursionComputesBestAction() {
      StateSpace.buildActionList = s -> {
         ArrayList<Action> actions = new ArrayList<>();
         if (s.getPeriod() == 0) {
            actions.add(new MockAction(s, "a1"));
            actions.add(new MockAction(s, "a2"));
         } else {
            actions.add(new MockAction(s, "noop"));
         }
         return actions;
      };
      StateSpace.idempotentAction = s -> new MockAction(s, "noop");

      MockStateSpace period0 = new MockStateSpace(0, new int[] { 0 }, sc);
      MockStateSpace period1 = new MockStateSpace(1, new int[] { 10, 20 }, sc);

      State s10 = period1.getState(Integer.valueOf(10));
      State s20 = period1.getState(Integer.valueOf(20));

      TransitionProbability tp = new MockTransitionProbability(s10, s20);
      SparkValueRepository repo = new SparkValueRepository((initialState, action, finalState) -> {
         MockAction a = (MockAction) action;
         if (a.name.equals("a1"))
            return 5.0;
         if (a.name.equals("a2"))
            return 2.0;
         return 0.0;
      }, 1.0, sc);

      SparkBackwardRecursion recursion = new SparkBackwardRecursion(
            Recursion.OptimisationDirection.MIN,
            new StateSpace<?>[] { period0, period1 },
            tp,
            repo);

      recursion.runBackwardRecursion();

      State initial = period0.getState(Integer.valueOf(0));
      Action bestAction = recursion.getValueRepository().getOptimalAction(initial);
      assertNotNull(bestAction);
      assertEquals(new MockAction(initial, "a2"), bestAction);
      assertEquals(2.0, recursion.getExpectedValue(initial), 1e-9);
   }

   @Test
   public void testSparkBackwardRecursionComputesBestActionForMaximisation() {
      StateSpace.buildActionList = s -> {
         ArrayList<Action> actions = new ArrayList<>();
         if (s.getPeriod() == 0) {
            actions.add(new MockAction(s, "a1"));
            actions.add(new MockAction(s, "a2"));
         } else {
            actions.add(new MockAction(s, "noop"));
         }
         return actions;
      };
      StateSpace.idempotentAction = s -> new MockAction(s, "noop");

      MockStateSpace period0 = new MockStateSpace(0, new int[] { 0 }, sc);
      MockStateSpace period1 = new MockStateSpace(1, new int[] { 10, 20 }, sc);

      State s10 = period1.getState(Integer.valueOf(10));
      State s20 = period1.getState(Integer.valueOf(20));

      TransitionProbability tp = new MockTransitionProbability(s10, s20);
      SparkValueRepository repo = new SparkValueRepository((initialState, action, finalState) -> {
         MockAction a = (MockAction) action;
         if (a.name.equals("a1"))
            return 5.0;
         if (a.name.equals("a2"))
            return 2.0;
         return 0.0;
      }, 1.0, sc);

      SparkBackwardRecursion recursion = new SparkBackwardRecursion(
            Recursion.OptimisationDirection.MAX,
            new StateSpace<?>[] { period0, period1 },
            tp,
            repo);

      recursion.runBackwardRecursion();

      State initial = period0.getState(Integer.valueOf(0));
      Action bestAction = recursion.getValueRepository().getOptimalAction(initial);
      assertNotNull(bestAction);
      assertEquals(new MockAction(initial, "a1"), bestAction);
      assertEquals(5.0, recursion.getExpectedValue(initial), 1e-9);
   }

   @Test
   public void testSparkBackwardRecursionFromPeriodUsesNoAction() {
      StateSpace.buildActionList = s -> {
         ArrayList<Action> actions = new ArrayList<>();
         if (s.getPeriod() == 0) {
            actions.add(new MockAction(s, "a1"));
            actions.add(new MockAction(s, "a2"));
         } else {
            actions.add(new MockAction(s, "noop"));
         }
         return actions;
      };
      StateSpace.idempotentAction = s -> new MockAction(s, "noop");

      MockStateSpace period0 = new MockStateSpace(0, new int[] { 0 }, sc);
      MockStateSpace period1 = new MockStateSpace(1, new int[] { 10, 20 }, sc);

      State s10 = period1.getState(Integer.valueOf(10));
      State s20 = period1.getState(Integer.valueOf(20));

      TransitionProbability tp = new MockTransitionProbability(s10, s20);
      SparkValueRepository repo = new SparkValueRepository((initialState, action, finalState) -> {
         MockAction a = (MockAction) action;
         if (a.name.equals("a1"))
            return 5.0;
         if (a.name.equals("a2"))
            return 2.0;
         return 0.0;
      }, 1.0, sc);

      SparkBackwardRecursion recursion = new SparkBackwardRecursion(
            Recursion.OptimisationDirection.MIN,
            new StateSpace<?>[] { period0, period1 },
            tp,
            repo);

      recursion.runBackwardRecursion(0);

      State initial = period0.getState(Integer.valueOf(0));
      Action bestAction = recursion.getValueRepository().getOptimalAction(initial);
      assertEquals(new MockAction(initial, "noop"), bestAction);
      assertEquals(0.0, recursion.getExpectedValue(initial), 1e-9);
   }
}

package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class SparkValueRepositoryTest {

   private JavaSparkContext sc;

   @Before
   public void setUp() {
      try {
         SparkConf conf = new SparkConf().setAppName("SparkValueRepositoryTest").setMaster("local");
         sc = new JavaSparkContext(conf);
         sc.setLogLevel("ERROR");
      } catch (UnsupportedOperationException e) {
         Assume.assumeTrue("Skipping SparkValueRepositoryTest: Spark is incompatible with this JDK version: " + e.getMessage(), false);
      }
   }

   @After
   public void tearDown() {
      if (sc != null) {
         sc.close();
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
         if (this == obj)
            return true;
         if (!(obj instanceof MockState))
            return false;
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
      private final String name;

      MockAction(State state, String name) {
         super(state);
         this.name = name;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (!(obj instanceof MockAction))
            return false;
         MockAction other = (MockAction) obj;
         return this.state.equals(other.state) && this.name.equals(other.name);
      }

      @Override
      public int hashCode() {
         return 31 * this.state.hashCode() + this.name.hashCode();
      }
   }

   private static final class OneStepTransitionProbability extends TransitionProbability {
      private final State finalState;

      OneStepTransitionProbability(State finalState) {
         this.finalState = finalState;
      }

      @Override
      public double getTransitionProbability(State initialState, Action action, State finalState) {
         return this.finalState.equals(finalState) ? 1.0 : 0.0;
      }

      @Override
      public ArrayList<State> generateFinalStates(State initialState, Action action) {
         ArrayList<State> states = new ArrayList<>();
         states.add(finalState);
         return states;
      }

      @Override
      public ArrayList<State> getFinalStates(State initialState, Action action) {
         ArrayList<State> states = new ArrayList<>();
         states.add(finalState);
         return states;
      }
   }

   @Test
   public void testOptimalValueAndActionRoundTrip() {
      SparkValueRepository repository = new SparkValueRepository((s, a, f) -> 1.0, 0.95, sc);
      State s0 = new MockState(0, 1);
      Action a0 = new MockAction(s0, "A0");

      repository.setOptimalExpectedValue(s0, 7.5);
      repository.setOptimalAction(s0, a0);

      assertTrue(repository.containsOptimalExpectedValue(s0));
      assertEquals(7.5, repository.getOptimalExpectedValue(s0), 1e-9);
      assertEquals(a0, repository.getOptimalAction(s0));
      assertEquals(1, repository.getOptimalValueHashTable().size());
      assertEquals(1, repository.getOptimalActionHashTable().size());
   }

   @Test
   public void testGetExpectedValueCachesComputedStateAction() {
      SparkValueRepository repository = new SparkValueRepository((s, a, f) -> 3.0, 0.5, sc);
      State s0 = new MockState(0, 1);
      State s1 = new MockState(1, 2);
      Action a0 = new MockAction(s0, "A0");
      repository.setOptimalExpectedValue(s1, 4.0);

      TransitionProbability tp = new OneStepTransitionProbability(s1);

      double first = repository.getExpectedValue(s0, a0, tp);
      int cacheSizeAfterFirst = repository.getValueHashTable().size();
      double second = repository.getExpectedValue(s0, a0, tp);

      assertEquals(5.0, first, 1e-9);
      assertEquals(first, second, 1e-9);
      assertEquals(cacheSizeAfterFirst, repository.getValueHashTable().size());
   }

   @Test
   public void testGetOptimalActionReturnsNullWhenUnset() {
      SparkValueRepository repository = new SparkValueRepository((s, a, f) -> 0.0, 1.0, sc);
      State state = new MockState(0, 99);
      assertNull(repository.getOptimalAction(state));
   }

   @Test
   public void testGetOptimalExpectedValueThrowsWhenMissing() {
      SparkValueRepository repository = new SparkValueRepository((s, a, f) -> 0.0, 1.0, sc);
      State state = new MockState(0, 77);
      try {
         repository.getOptimalExpectedValue(state);
      } catch (NullPointerException e) {
         assertNotNull(e.getMessage());
         return;
      }
      throw new AssertionError("Expected NullPointerException when value is not set");
   }
}

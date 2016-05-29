package jsdp.sdp;

/**
 * A functional interface that captures random outcomes.
 * 
 * @author Roberto Rossi
 *
 * @param <S> the generic type for a state
 * @param <A> the generic type for an action
 * @param <D> the generic type of the value returned
 */
@FunctionalInterface
public interface RandomOutcomeFunction <S, A, D> { 
   /**
    * 
    * @param initialState the initial state of the stochastic process.
    * @param action the chosen action.
    * @param finalState the final state of the stochastic process.
    * @return the random outcome associated with a transition from {@code initialState} to {@code finalState} under a chosen {@code action}.
    */
   public D apply (S initialState, A action, S finalState);
}
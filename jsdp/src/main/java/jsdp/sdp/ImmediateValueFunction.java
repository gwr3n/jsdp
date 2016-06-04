package jsdp.sdp;

/**
 * A functional interface that captures immediate value of a transition from an initial state to 
 * a final state under a chosen action.
 * 
 * @author Roberto Rossi
 *
 * @param <S> the generic type for a state
 * @param <A> the generic type for an action
 * @param <D> the generic type of the value returned
 */
@FunctionalInterface
public interface ImmediateValueFunction <S, A, D> { 
   /**
    * 
    * @param initialState the initial state of the stochastic process.
    * @param action the chosen action.
    * @param finalState the final state of the stochastic process.
    * @return the immediate value of a transition from {@code initialState} to {@code finalState} under a chosen {@code action}.
    */
   public D apply (S initialState, A action, S finalState);
}
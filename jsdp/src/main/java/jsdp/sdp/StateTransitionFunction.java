package jsdp.sdp;

@FunctionalInterface
public interface StateTransitionFunction <S, A, R> { 
   public S apply (S state, A action, R randomOutcome);
}

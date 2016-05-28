package jsdp.sdp;

@FunctionalInterface
public interface StateTransitionFunction <S, A, R> { 
   public S apply (S s, A a, R r);
}

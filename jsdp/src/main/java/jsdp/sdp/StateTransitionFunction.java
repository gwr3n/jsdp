package jsdp.sdp;

@FunctionalInterface
public interface StateTransitionFunction <S, A, R, T> { 
   public T apply (S s, A a, R r);
}

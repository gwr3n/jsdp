package jsdp.app.inventory.capital;

import java.util.stream.IntStream;

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class CF_Action extends Action{
   
   private static final long serialVersionUID = 1L;
   
   private static int maxOrderQuantity;
   
   public static void setMaxOrderQuantity(int maxOrderQuantity){
      CF_Action.maxOrderQuantity = maxOrderQuantity;
   }
   
   int orderQuantity;
   
   public CF_Action(State state,
                    int orderQuantity){
      super(state);
      this.orderQuantity = orderQuantity;
   }
   
   public int getOrderQuantity(){
      return this.orderQuantity;
   }

   @Override
   public boolean equals(Object action) {
      if(action instanceof CF_Action)
         return this.orderQuantity == ((CF_Action) action).orderQuantity; 
      else
         return false;
   }

   @Override
   public int hashCode() {
      String hash = "A";
      hash = (hash + state.getPeriod()) + "_" + 
            this.orderQuantity;
      return hash.hashCode();
   }
   
   @Override
   public String toString(){
      String out = "";
      out += "Period: " + state.getPeriod()  + "\t";
      out += "Order quantity: " + this.orderQuantity;
      return out;
   }
   
   public static IntStream generateOrderQuantities(CF_State state){
      return IntStream.iterate(0, i -> i + 1).limit(CF_Action.maxOrderQuantity + 1); 
   }
}

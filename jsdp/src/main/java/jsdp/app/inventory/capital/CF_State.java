package jsdp.app.inventory.capital;

import jsdp.sdp.State;

public class CF_State extends State {
   
   private static final long serialVersionUID = 1L;
   
   private int inventory;
   private int capital;
   
   public CF_State(CF_StateDescriptor descriptor) {
      super(descriptor.getPeriod());
      this.inventory = descriptor.getInventory();
      this.capital = descriptor.getCapital();
   }
   
   public int getInventory(){
      return this.inventory;
   }
   
   public int getCapital(){
      return this.capital;
   }

   @Override
   public boolean equals(Object state) {
      if(state instanceof CF_State)
         return this.period == ((CF_State) state).period &&
                this.inventory == ((CF_State) state).inventory &&
                this.capital == ((CF_State) state).capital;
      else 
         return false;
   }

   @Override
   public int hashCode() {
      String hash = "S";
      hash = (hash + period) + "_I" + 
             this.inventory + "_C" +
             this.capital;
      return hash.hashCode();
   }
   
   @Override
   public String toString(){
      String out = "";
      out = "Period: " + period + "\t" + 
            "Inventory: " + this.inventory + "\t" +
            "Capital: " + this.capital;
      return out;
   }
}

package jsdp.app.inventory.capital;

import jsdp.sdp.State;

public class CF_StateDescriptor extends State{

   private static final long serialVersionUID = 1L;
   
   private int inventory;
   private int capital;

   public CF_StateDescriptor(int period,
                             int inventory,
                             int capital) {
      super(period);
      this.inventory = inventory;
      this.capital = capital;
   }

   @Override
   public boolean equals(Object state) {
      if(state instanceof CF_StateDescriptor){
         return this.period == ((CF_StateDescriptor) state).period &&
                this.inventory == ((CF_StateDescriptor) state).inventory &&
                this.capital == ((CF_StateDescriptor) state).capital;
      }else
         return false;
   }

   @Override
   public int hashCode() {
      String hash = "SD";
      hash = (hash + period) + "_I" + 
             this.inventory + "C_" +
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
   
   public int getCapital(){
      return this.capital;
   }
   
   public int getInventory(){
      return this.inventory;
   }

}

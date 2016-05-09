package jsdp.app.stochasticlotsizing;

import jsdp.sdp.StateDescriptor;

public class sS_StateDescriptor extends StateDescriptor{
		
		int initialInventory;
		
		public sS_StateDescriptor(int period, int initialInventory){
			this.period = period;
			this.initialInventory = initialInventory;
		}
		
		public boolean equals(Object descriptor){
			if(descriptor instanceof sS_StateDescriptor)
				return this.period == ((sS_StateDescriptor)descriptor).period &&
				 	   this.initialInventory == ((sS_StateDescriptor)descriptor).initialInventory;
			else
				return false;
		}
		
		public int hashCode(){
			String hash = "";
	        hash = (hash + period) + "_" + initialInventory;
	        return hash.hashCode();
		}
		
		public int getInitialInventory(){
			return initialInventory;
		}
	}
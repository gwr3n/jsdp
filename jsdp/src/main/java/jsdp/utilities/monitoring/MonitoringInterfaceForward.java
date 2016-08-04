package jsdp.utilities.monitoring;

import jsdp.sdp.Recursion;

public class MonitoringInterfaceForward extends MonitoringInterface{
   
   private static final long serialVersionUID = 1L;
   
   protected long reusedStates;
   
   public MonitoringInterfaceForward(Recursion recursion){
      recursion.setStateMonitoring(true);
      this.setTitle("jsdp statistics");
      this.text.setEditable(false);
      this.getContentPane().add(text);
      this.setSize(300, 120);
      this.setVisible(true);
   }
   
   public void setStates(long generatedStates, long reusedStates) {
      this.generatedStates = generatedStates;
      this.reusedStates = reusedStates;
   }
   
   @Override
   public void run() {
      this.cpuAfter = this.osMBean.getProcessCpuTime();
      this.nanoAfter = System.nanoTime();
      while(!terminate){
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         
         this.cpuAfter = this.osMBean.getProcessCpuTime();
         this.nanoAfter = System.nanoTime();
            
         setText("Time: " + this.getTime() +"\n"
               + "CPU: "  + this.getPercentCPU() +"%" +" ("+Runtime.getRuntime().availableProcessors()+" cores)\n"
               + "States processed per second: "+ getProcessedStatesPerSecond() +"\n"
               + "Generated states: " + this.getGeneratedStates() +"\n"
               + "Reused states: " + this.getReusedStates());
      }
   }
   
   public long getReusedStates(){
      return this.reusedStates;
   }
   
   public double getProcessedStatesPerSecond(){
      return (int) Math.ceil((generatedStates+reusedStates)/((this.nanoAfter-this.nanoBefore)*Math.pow(10, -9)));
   }
   
   public String toString(){
      return "Time: " + this.getTime() +"\n"
            + "CPU: "  + this.getPercentCPU() +"%" +" ("+Runtime.getRuntime().availableProcessors()+" cores)\n"
            + "States processed per second: "+ getProcessedStatesPerSecond() +"\n"
            + "Generated states: " + this.getGeneratedStates() +"\n"
            + "Reused states: " + this.getReusedStates();
   }
}

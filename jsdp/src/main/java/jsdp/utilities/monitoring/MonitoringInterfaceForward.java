package jsdp.utilities.monitoring;

import jsdp.sdp.Recursion;

public class MonitoringInterfaceForward extends MonitoringInterface{
   
   private static final long serialVersionUID = 1L;
   
   protected long generatedStates;
   protected long reusedStates;
   
   public MonitoringInterfaceForward(Recursion recursion){
      recursion.setStateMonitoring(true);
      this.setTitle("jsdp statistics");
      this.text.setEditable(false);
      this.getContentPane().add(text);
      this.setSize(300, 120);
      this.setVisible(true);
   }
   
   public synchronized void setStates(long generatedStates, long reusedStates) {
      this.generatedStates = generatedStates;
      this.reusedStates = reusedStates;
   }
   
   @Override
   public void run() {
      while(!terminate){
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         long cpuAfter = this.osMBean.getProcessCpuTime();
         long nanoAfter = System.nanoTime();
            
         long percent;
         if (nanoAfter > this.nanoBefore)
            percent = ((cpuAfter-this.cpuBefore)*100L)/
               (nanoAfter-this.nanoBefore);
         else percent = 0;   
            
         setText("Time: " + (int) Math.ceil(((nanoAfter-this.nanoBefore)*Math.pow(10, -9))) +"\n"
               + "CPU: "  +percent+"%" +" ("+Runtime.getRuntime().availableProcessors()+" cores)\n"
               + "States processed per second: "+ (int) Math.ceil((generatedStates+reusedStates)/((nanoAfter-this.nanoBefore)*Math.pow(10, -9))) +"\n"
               + "Generated states: " + generatedStates +"\n"
               + "Reused states: " + reusedStates);
      }
   }
}

package jsdp.utilities.monitoring;

import jsdp.sdp.Recursion;

public class MonitoringInterfaceBackward extends MonitoringInterface{
   
   private static final long serialVersionUID = 1L;
   
   protected long generatedStates;
   protected long processedStates;
   protected int currentStage;
   
   public MonitoringInterfaceBackward(Recursion recursion){
      recursion.setStateMonitoring(true);
      this.setTitle("jsdp statistics");
      this.text.setEditable(false);
      this.getContentPane().add(text);
      this.setSize(300, 150);
      this.setVisible(true);
   }
   
   public synchronized void setStates(long generatedStates, long processedStates, int currentStage) {
      this.generatedStates = generatedStates;
      this.processedStates = processedStates;
      this.currentStage = currentStage;
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
               + "Generated states: " + generatedStates +"\n"
               + "States processed: "+ processedStates +"\n"
               + "States processed per second: "+ (int) Math.ceil((processedStates)/((nanoAfter-this.nanoBefore)*Math.pow(10, -9))) +"\n"
               + "Percent completed: "+ (int) Math.floor(processedStates*100.0/generatedStates) +"%\n"
               + "Current stage: " + currentStage);
      }
   }
}

package jsdp.utilities.monitoring;

import jsdp.sdp.Recursion;

/**
 * Monitoring interface for backward recursion
 *  
 * @author gwren
 *
 */
public class MonitoringInterfaceBackward extends MonitoringInterface{
   
   private static final long serialVersionUID = 1L;
   
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
   
   /**
    * Set states status
    * 
    * @param generatedStates number of generated states
    * @param processedStates number of processed states
    * @param currentStage current stage
    */
   public void setStates(long generatedStates, long processedStates, int currentStage) {
      this.generatedStates = generatedStates;
      this.processedStates = processedStates;
      this.currentStage = currentStage;
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
            
         setText("Time: " + getTime() +"\n"
               + "CPU: " +this.getPercentCPU()+ "%" +" ("+Runtime.getRuntime().availableProcessors()+" cores)\n"
               + "Generated states: " + this.getGeneratedStates() +"\n"
               + "States processed: "+ this.getProcessedStates() +"\n"
               + "States processed per second: "+ this.getProcessedStatesPerSecond() +"\n"
               + "Percent completed: "+ (int) Math.floor(processedStates*100.0/generatedStates) +"%\n"
               + "Current stage: " + currentStage);
      }
   }
   
   /**
    * Get the number of processed states
    * 
    * @return the number of processed states
    */
   public long getProcessedStates(){
      return this.processedStates;
   }
   
   @Override
   public double getProcessedStatesPerSecond(){
      return (int) Math.ceil((this.processedStates)/((this.nanoAfter-this.nanoBefore)*Math.pow(10, -9)));
   }
   
   @Override
   public String toString(){
      return "Time: " + getTime() +"\n"
            + "CPU: " +this.getPercentCPU()+ "%" +" ("+Runtime.getRuntime().availableProcessors()+" cores)\n"
            + "Generated states: " + this.getGeneratedStates() +"\n"
            + "States processed: "+ this.getProcessedStates() +"\n"
            + "States processed per second: "+ this.getProcessedStatesPerSecond() +"\n"
            + "Percent completed: "+ (int) Math.floor(processedStates*100.0/generatedStates) +"%\n"
            + "Current stage: " + currentStage;
   }
}

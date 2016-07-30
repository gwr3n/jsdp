package jsdp.utilities.monitoring;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.swing.*;

import com.sun.management.OperatingSystemMXBean;

import jsdp.sdp.Recursion;

public class MonitoringInterface extends JFrame implements Runnable{
   
   private static final long serialVersionUID = 1L;

   static MonitoringInterface instance = null;
   
   JTextArea text = new JTextArea();

   long generatedStates;
   long reusedStates;
   
   OperatingSystemMXBean osMBean;
   long nanoBefore;
   long cpuBefore;
   
   boolean terminate = false;
   
   public MonitoringInterface(Recursion recursion){
      recursion.setStateMonitoring(true);
      this.setTitle("jsdp statistics");
      this.text.setEditable(false);
      this.getContentPane().add(text);
      this.setSize(300, 120);
      this.setVisible(true);
   }
   
   public synchronized void setStates(long generatedStates, long reusedStates){
      this.generatedStates = generatedStates;
      this.reusedStates = reusedStates;
   }
   
   private synchronized void setText(String text){
      this.text.setText(text);
   }
   
   public void startMonitoring(){
      try {
         osMBean = ManagementFactory.newPlatformMXBeanProxy(
               ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
         nanoBefore = System.nanoTime();
         cpuBefore = osMBean.getProcessCpuTime();
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      Thread runner = new Thread(this);
      runner.start();
   }

   public void terminate(){
      this.terminate = true;
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

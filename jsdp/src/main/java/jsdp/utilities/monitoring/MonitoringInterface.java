package jsdp.utilities.monitoring;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.swing.*;

import com.sun.management.OperatingSystemMXBean;

public class MonitoringInterface extends JFrame{
   
   private static final long serialVersionUID = 1L;

   static MonitoringInterface instance = null;
   
   JTextArea text = new JTextArea();
   long startTime;
   long generatedStates;
   long reusedStates;
   
   OperatingSystemMXBean osMBean;
   long nanoBefore;
   long cpuBefore;
   
   public MonitoringInterface(){
      try {
         osMBean = ManagementFactory.newPlatformMXBeanProxy(
               ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
         nanoBefore = System.nanoTime();
         cpuBefore = osMBean.getProcessCpuTime();
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      this.setTitle("jsdp statistics");
      this.text.setEditable(false);
      this.getContentPane().add(text);
      this.setSize(300, 120);
      this.setVisible(true);
   }
   
   public synchronized static MonitoringInterface getMonitoringInterface(){
      return (instance == null) ? instance = new MonitoringInterface() : instance;
   }
   
   public synchronized static void setStartTime(long startTime){
      getMonitoringInterface().startTime = startTime;
   }
   
   public synchronized static void setStates(long generatedStates, long reusedStates){
      getMonitoringInterface().generatedStates = generatedStates;
      getMonitoringInterface().reusedStates = reusedStates;
      
      long cpuAfter = getMonitoringInterface().osMBean.getProcessCpuTime();
      long nanoAfter = System.nanoTime();
      
      long percent;
      if (nanoAfter > getMonitoringInterface().nanoBefore)
       percent = ((cpuAfter-getMonitoringInterface().cpuBefore)*100L)/
         (nanoAfter-getMonitoringInterface().nanoBefore);
      else percent = 0;
      
      setText("Time: " + (int) Math.ceil(((nanoAfter-getMonitoringInterface().nanoBefore)*Math.pow(10, -9))) +"\n"
            + "CPU: "  +percent+"%" +" ("+Runtime.getRuntime().availableProcessors()+" cores)\n"
            + "States processed per second: "+ (int) Math.ceil((generatedStates+reusedStates)/((nanoAfter-getMonitoringInterface().nanoBefore)*Math.pow(10, -9))) +"\n"
            + "Generated states: " + generatedStates +"\n"
            + "Reused states: " + reusedStates);
   }
   
   private synchronized static void setText(String text){
      getMonitoringInterface().text.setText(text);
   }
}

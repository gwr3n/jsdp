package jsdp.utilities.monitoring;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.sun.management.OperatingSystemMXBean;

public abstract class MonitoringInterface extends JFrame implements Runnable{

   private static final long serialVersionUID = 1L;
   
   protected JTextArea text = new JTextArea();
   
   protected OperatingSystemMXBean osMBean;
   protected long nanoBefore;
   protected long nanoAfter;
   protected long cpuBefore;
   protected long cpuAfter;
   protected boolean terminate = false;
   
   protected long generatedStates;

   public MonitoringInterface() throws HeadlessException {
      super();
   }

   public MonitoringInterface(GraphicsConfiguration gc) {
      super(gc);
   }

   public MonitoringInterface(String title) throws HeadlessException {
      super(title);
   }

   public MonitoringInterface(String title, GraphicsConfiguration gc) {
      super(title, gc);
   }

   protected void setText(String text) {
      this.text.setText(text);
   }

   public void startMonitoring() {
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

   public void terminate() {
      this.terminate = true;
   }
   
   public int getTime(){
      return (int) Math.ceil(((this.nanoAfter-this.nanoBefore)*Math.pow(10, -9)));
   }
   
   public long getPercentCPU(){
      long percent;
      if (this.nanoAfter > this.nanoBefore)
         percent = ((this.cpuAfter-this.cpuBefore)*100L)/(this.nanoAfter-this.nanoBefore);
      else percent = 0; 
      return percent;
   }
   
   public long getGeneratedStates(){
      return this.generatedStates;
   }
   
   public abstract double getProcessedStatesPerSecond();
}
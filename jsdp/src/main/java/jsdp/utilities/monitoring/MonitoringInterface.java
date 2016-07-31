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
   protected long cpuBefore;
   protected boolean terminate = false;

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

   protected synchronized void setText(String text) {
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

}
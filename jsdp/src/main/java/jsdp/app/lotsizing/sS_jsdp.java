/**
 * jsdp: A Java Stochastic Dynamic Programming Library
 * 
 * MIT License
 * 
 * Copyright (c) 2016 Roberto Rossi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jsdp.app.lotsizing;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import umontreal.ssj.charts.XYLineChart;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;
import umontreal.ssj.probdist.NormalDist;
import jsdp.app.lotsizing.simulation.SimulatePolicies;

/**
 *  We formulate the stochastic lot sizing problem as defined in  
 *  
 *  Herbert E. Scarf. Optimality of (s, S) policies in the dynamic inventory problem. In K. J. Arrow, S. Karlin, 
 *  and P. Suppes, editors, Mathematical Methods in the Social Sciences, pages 196–202. Stanford University
 *  Press, Stanford, CA, 1960.
 *  
 *  as a stochastic dynamic programming problem. 
 *  
 *  We use forward and backward recursion to find optimal policies.
 *  
 *  We plot the optimal expected total cost function as a function of a period opening inventory level.
 *  
 * @author Roberto Rossi
 *
 */
public class sS_jsdp {

   static final Logger logger = LogManager.getLogger(sS_jsdp.class.getName());

   public static void main(String[] args) {
      //Factor must be 1 for discrete distributions
      sS_State.setStateBoundaries(1, -100, 100);

      double fixedOrderingCost = 50; 
      double proportionalOrderingCost = 0; 
      double holdingCost = 1;
      double penaltyCost = 4;

      double[] demand = {20,30,20,40};

      /*Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(demand.length)
                                              .mapToObj(i -> new NormalDist(demand[i],demand[i]*0.4))
                                              .toArray(Distribution[]::new);*/

      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                                .limit(demand.length)
                                                .mapToObj(i -> new PoissonDist(demand[i]))
                                                .toArray(Distribution[]::new);
      
      double minDemand = 0;
      double maxDemand = sS_State.getMaxInventory()-sS_State.getMinInventory();
      
      double initialInventory = 0;
      sS_StateSpaceSampleIterator.SamplingScheme samplingScheme = sS_StateSpaceSampleIterator.SamplingScheme.NONE;
      int maxSampleSize = 25;

      System.out.println("--------------Forward recursion--------------");
      simpleTestForward(fixedOrderingCost, 
                        proportionalOrderingCost, 
                        holdingCost, 
                        penaltyCost, 
                        distributions, 
                        minDemand,
                        maxDemand,
                        initialInventory);
      System.out.println("--------------Backward recursion--------------");
      simpleTestBackward(fixedOrderingCost, 
                         proportionalOrderingCost, 
                         holdingCost, 
                         penaltyCost, 
                         distributions, 
                         minDemand,
                         maxDemand,
                         initialInventory,
                         samplingScheme,
                         maxSampleSize);
      System.out.println("--------------Cost function plot--------------");
      int targetPeriod = 0;
      boolean printCostFunctionValues = false;
      boolean latexOutput = false;
      plotCostFunction(targetPeriod, 
                       fixedOrderingCost, 
                       proportionalOrderingCost, 
                       holdingCost, 
                       penaltyCost, 
                       distributions,
                       minDemand,
                       maxDemand,
                       printCostFunctionValues,
                       latexOutput,
                       samplingScheme,
                       maxSampleSize);
   }

   public static void simpleTestBackward(double fixedOrderingCost,
                                         double proportionalOrderingCost,
                                         double holdingCost,
                                         double penaltyCost,
                                         Distribution[] distributions,
                                         double minDemand,
                                         double maxDemand,
                                         double initialInventory,
                                         sS_StateSpaceSampleIterator.SamplingScheme samplingScheme,
                                         int maxSampleSize){

      /**
       * Simulation confidence level and error threshold
       */
      double confidence = 0.95;
      double errorTolerance = 0.001;

      solveSampleInstanceBackwardRecursion(distributions,
                                           minDemand,
                                           maxDemand,
                                           fixedOrderingCost,
                                           proportionalOrderingCost,
                                           holdingCost,
                                           penaltyCost,
                                           initialInventory,
                                           confidence,
                                           errorTolerance,
                                           samplingScheme,
                                           maxSampleSize);
   }

   public static void simpleTestForward(double fixedOrderingCost,
                                        double proportionalOrderingCost,
                                        double holdingCost,
                                        double penaltyCost,
                                        Distribution[] distributions,
                                        double minDemand,
                                        double maxDemand,
                                        double initialInventory){

      /**
       * Simulation confidence level and error threshold
       */
      double confidence = 0.95;
      double errorTolerance = 0.001;

      solveSampleInstanceForwardRecursion(distributions,
                                          minDemand,
                                          maxDemand,
                                          fixedOrderingCost,
                                          proportionalOrderingCost,
                                          holdingCost,
                                          penaltyCost,
                                          initialInventory,
                                          confidence,
                                          errorTolerance);
   }

   public static void solveSampleInstanceBackwardRecursion(Distribution[] distributions,
                                                           double minDemand,
                                                           double maxDemand,
                                                           double fixedOrderingCost, 
                                                           double proportionalOrderingCost, 
                                                           double holdingCost,
                                                           double penaltyCost,
                                                           double initialInventory,
                                                           double confidence,
                                                           double errorTolerance,
                                                           sS_StateSpaceSampleIterator.SamplingScheme samplingScheme,
                                                           int maxSampleSize){

      sS_BackwardRecursion recursion = new sS_BackwardRecursion(distributions,
                                                                minDemand,
                                                                maxDemand,
                                                                fixedOrderingCost,
                                                                proportionalOrderingCost,
                                                                holdingCost,
                                                                penaltyCost,
                                                                samplingScheme,
                                                                maxSampleSize);
      /*sS_SequentialBackwardRecursion recursion = new sS_SequentialBackwardRecursion(distributions,
                                                                                    fixedOrderingCost,
                                                                                    proportionalOrderingCost,
                                                                                    holdingCost,
                                                                                    penaltyCost);*/

      StopWatch timer = new StopWatch();
      timer.start();
      recursion.runBackwardRecursion();
      timer.stop();
      System.out.println();
      double ETC = recursion.getExpectedCost(initialInventory);
      System.out.println("Expected total cost (assuming an initial inventory level "+initialInventory+"): "+ETC);
      System.out.println("Time elapsed: "+timer);
      System.out.println();
      
      double[][] optimalPolicy = recursion.getOptimalPolicy(initialInventory);
      double[] s = optimalPolicy[0];
      double[] S = optimalPolicy[1];      
      for(int i = 0; i < distributions.length; i++){
         System.out.println("S["+(i+1)+"]:"+S[i]+"\ts["+(i+1)+"]:"+s[i]);
      }

      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.00",otherSymbols);

      double[] results = SimulatePolicies.simulate_sS(distributions, 
                                                      fixedOrderingCost, 
                                                      holdingCost, 
                                                      penaltyCost, 
                                                      proportionalOrderingCost, 
                                                      initialInventory, 
                                                      S, s, 
                                                      confidence, 
                                                      errorTolerance);
      System.out.println();
      System.out.println("Simulated cost: "+ df.format(results[0])+
                         " Confidence interval=("+df.format(results[0]-results[1])+","+
                         df.format(results[0]+results[1])+")@"+
                         df.format(confidence*100)+"% confidence");
      System.out.println();
   }

   public static void solveSampleInstanceForwardRecursion(Distribution[] distributions,
                                                          double minDemand,
                                                          double maxDemand,
                                                          double fixedOrderingCost, 
                                                          double proportionalOrderingCost, 
                                                          double holdingCost,
                                                          double penaltyCost,
                                                          double initialInventory,
                                                          double confidence,
                                                          double errorTolerance){

      sS_ForwardRecursion recursion = new sS_ForwardRecursion(distributions,
                                                              minDemand,
                                                              maxDemand,
                                                              fixedOrderingCost,
                                                              proportionalOrderingCost,
                                                              holdingCost,
                                                              penaltyCost);

      sS_StateDescriptor initialState = new sS_StateDescriptor(0, sS_State.inventoryToState(initialInventory));

      /**
       * Replace parallelStream with stream in "runForwardRecursion" to prevent deadlock
       */
      StopWatch timer = new StopWatch();
      timer.start();
      recursion.runForwardRecursion(((sS_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
      timer.stop();
      double ETC = recursion.getExpectedCost(initialInventory);
      double action = sS_State.stateToInventory(recursion.getOptimalAction(initialState).getIntAction());
      System.out.println("Expected total cost (assuming an initial inventory level "+initialInventory+"): "+ETC);
      System.out.println("Optimal initial action: "+action);
      System.out.println("Time elapsed: "+timer);
      System.out.println();
      
      double[][] optimalPolicy = recursion.getOptimalPolicy(initialInventory);
      double[] s = optimalPolicy[0];
      double[] S = optimalPolicy[1];
      for(int i = 0; i < distributions.length; i++){
         System.out.println("S["+(i+1)+"]:"+S[i]+"\ts["+(i+1)+"]:"+s[i]);
      }
      System.out.println("Note that (s,S) values for period 0 have been set on best effort basis.");

      DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
      DecimalFormat df = new DecimalFormat("#.00",otherSymbols);

      double[] results = SimulatePolicies.simulate_sS(distributions, 
                                                      fixedOrderingCost, 
                                                      holdingCost, 
                                                      penaltyCost, 
                                                      proportionalOrderingCost, 
                                                      initialInventory, 
                                                      recursion, 
                                                      confidence, 
                                                      errorTolerance);
      System.out.println("Simulated cost: "+df.format(results[0])+
                         " Confidence interval=("+df.format(results[0]-results[1])+","+
                         df.format(results[0]+results[1])+")@"+
                         df.format(confidence*100)+"% confidence");
      System.out.println();
   }

   public static void plotCostFunction(int targetPeriod,
                                       double fixedOrderingCost, 
                                       double proportionalOrderingCost, 
                                       double holdingCost,
                                       double penaltyCost,
                                       Distribution[] distributions,
                                       double minDemand,
                                       double maxDemand,
                                       boolean printCostFunctionValues,
                                       boolean latexOutput,
                                       sS_StateSpaceSampleIterator.SamplingScheme samplingScheme,
                                       int maxSampleSize){

      sS_BackwardRecursion recursion = new sS_BackwardRecursion(distributions,
                                                                minDemand,
                                                                maxDemand,
                                                                fixedOrderingCost,
                                                                proportionalOrderingCost,
                                                                holdingCost,
                                                                penaltyCost,
                                                                samplingScheme,
                                                                maxSampleSize);
      recursion.runBackwardRecursion(targetPeriod);
      XYSeries series = new XYSeries("(s,S) policy");
      for(double i = sS_State.getMinInventory(); i <= sS_State.getMaxInventory(); i += sS_State.getStepSize()){
         sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(targetPeriod, sS_State.inventoryToState(i));
         series.add(i,recursion.getExpectedCost(stateDescriptor));
         if(printCostFunctionValues) 
            System.out.println(i+"\t"+recursion.getExpectedCost(stateDescriptor));
      }
      XYDataset xyDataset = new XYSeriesCollection(series);
      JFreeChart chart = ChartFactory.createXYLineChart("(s,S) policy - period "+targetPeriod+" expected total cost", "Opening inventory level", "Expected total cost",
            xyDataset, PlotOrientation.VERTICAL, false, true, false);
      ChartFrame frame = new ChartFrame("(s,S) policy",chart);
      frame.setVisible(true);
      frame.setSize(500,400);

      if(latexOutput){
         try {
            XYLineChart lc = new XYLineChart("(s,S) policy", "Opening inventory level", "Expected total cost", new XYSeriesCollection(series));
            File latexFolder = new File("./latex");
            if(!latexFolder.exists()){
               latexFolder.mkdir();
            }
            Writer file = new FileWriter("./latex/graph.tex");
            file.write(lc.toLatex(8, 5));
            file.close();
         } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
   }
}

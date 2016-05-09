package app.stochasticlotsizing;

import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import app.stochasticlotsizing.simulation.SimulatePolicies;
import sdp.State;
import umontreal.iro.lecuyer.charts.XYLineChart;

public class sS_DP {
	
	static final Logger logger = LogManager.getLogger(sS_DP.class.getName());
	
	public static void main(String[] args) {
		//simpleTest();
		plotTest();
	}
	
	public static void simpleTest(){
		
		sS_State.factor = 1;					//Factor must be 1 for discrete distributions
		sS_State.minInventory = -100;
		sS_State.maxInventory = 150;
		
		double fixedOrderingCost = 80; 
		double proportionalOrderingCost = 0; 
		double holdingCost = 1;
		double penaltyCost = 2;
		
		double[] demand = {15,16,15,14,11,7,6,3};
		
		double initialInventory = 0;
		
		/**
		 * Simulation confidence level and error threshold
		 */
		double confidence = 0.95;
		double errorTolerance = 0.001;
		
		solveSampleInstance(demand,fixedOrderingCost,proportionalOrderingCost,holdingCost,penaltyCost,initialInventory,confidence,errorTolerance);
	}
	
	public static void plotTest(){
		sS_State.factor = 1;					//Factor must be 1 for discrete distributions
		sS_State.minInventory = -100;
		sS_State.maxInventory = 100;
		
		double fixedOrderingCost = 50; 
		double proportionalOrderingCost = 0; 
		double holdingCost = 1;
		double penaltyCost = 4;
		
		double[] demand = {20,30,20,40};
		
		plotCostFunction(demand,fixedOrderingCost,proportionalOrderingCost,holdingCost,penaltyCost,false,false,false);
	}
	
	public static void solveSampleInstance(
			double[] demand,
			double fixedOrderingCost, 
			double proportionalOrderingCost, 
			double holdingCost,
			double penaltyCost,
			double initialInventory,
			double confidence,
			double errorTolerance){
		
		sS_BackwardRecursionPoisson recursion = new sS_BackwardRecursionPoisson(demand,fixedOrderingCost,proportionalOrderingCost,holdingCost,penaltyCost);
		StopWatch timer = new StopWatch();
		timer.start();
		recursion.runBackwardRecursion();
		timer.stop();
	    System.out.println();
		double[] S = new double[demand.length];
		double[] s = new double[demand.length];
		double ETC = Double.NaN;
		for(int i = 0; i < demand.length; i++){
			if(i == 0) {
				sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(0, (int)Math.round(initialInventory*sS_State.factor));
				State initialState = ((sS_StateSpace)recursion.getStateSpace(i)).getState(stateDescriptor);
				ETC = recursion.expectedCost(initialState);
				s[i] = recursion.find_s(i).getInitialInventory()/sS_State.factor;
				S[i] = ((sS_Action)recursion.getCostRepository().getOptimalAction(initialState)).getOrderQuantity()/sS_State.factor+initialInventory;
			}
			else{
				s[i] = recursion.find_s(i).getInitialInventory()/sS_State.factor;
				S[i] = recursion.find_S(i).getInitialInventory()/sS_State.factor;
			}
		}
		
		System.out.println("Expected total cost (assuming an initial inventory level "+initialInventory+"): "+ETC);
		System.out.println("Time elapsed: "+timer);
		System.out.println();
		for(int i = 0; i < demand.length; i++){
			System.out.println("S["+(i+1)+"]:"+S[i]+"\ts["+(i+1)+"]:"+s[i]);
		}
		
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
	    DecimalFormat df = new DecimalFormat("#.00",otherSymbols);
		
		double[] results = SimulatePolicies.simulate_sS_Poisson(demand.length, demand, fixedOrderingCost, holdingCost, penaltyCost, proportionalOrderingCost, initialInventory, S, s, confidence, errorTolerance);
		System.out.println();
		System.out.println("Simulated cost: "+df.format(results[0])+" Confidence interval=("+df.format(results[0]-results[1])+","+df.format(results[0]+results[1])+")@"+df.format(confidence*100)+"% confidence");
	}
	
	public static void plotCostFunction(
			double[] demand,
			double fixedOrderingCost, 
			double proportionalOrderingCost, 
			double holdingCost,
			double penaltyCost,
			boolean orderAtPeriod0,
			boolean printCostFunctionValues,
			boolean latexOutput){
		
		sS_BackwardRecursionPoisson recursion = new sS_BackwardRecursionPoisson(demand,fixedOrderingCost,proportionalOrderingCost,holdingCost,penaltyCost);
		if(orderAtPeriod0)
			recursion.runBackwardRecursion();
		else
			recursion.runBackwardRecursion(0);
		XYSeries series = new XYSeries("(s,S) policy");
		for(int i = 0; i <= sS_State.maxInventory; i++){
			int period = 0;
			sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period, i);
			sS_State state = (sS_State) ((sS_StateSpace)recursion.getStateSpace()[period]).getState(stateDescriptor);
			series.add(i/sS_State.factor,recursion.expectedCost(state));
			if(printCostFunctionValues) 
				System.out.println(i/sS_State.factor+"\t"+recursion.expectedCost(state));
		}
		XYDataset xyDataset = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createXYLineChart("(s,S) policy", "Opening inventory level", "Expected total cost",
				 xyDataset, PlotOrientation.VERTICAL, false, true, false);
		ChartFrame frame = new ChartFrame("(s,S) policy",chart);
		frame.setVisible(true);
		frame.setSize(500,400);
		
		XYLineChart lc = new XYLineChart("(s,S) policy", "Opening inventory level", "Expected total cost", new XYSeriesCollection(series));
		
		if(latexOutput){
			try {
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

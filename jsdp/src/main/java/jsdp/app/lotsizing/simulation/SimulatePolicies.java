package jsdp.app.lotsizing.simulation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.IntStream;

import jsdp.app.lotsizing.sampling.SampleFactory;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.stat.Tally;

public class SimulatePolicies {
	
	public static void main(String args[]){
		double ch = 1;	  
		double co = 100;
		double cu = 0;
		double cp = 10;
		double co_eff_var = 0.25;
		double demand[] = {20,40,60,40};
		double[] stdDemand = new double[demand.length];
		for(int o = 0; o < demand.length; o++) stdDemand[o] = demand[o]*co_eff_var; 
		Distribution[] distributions = IntStream.iterate(0, i -> i + 1).limit(demand.length).mapToObj(i -> new NormalDist(demand[i], stdDemand[i])).toArray(Distribution[]::new);
		
		double errorTolerance = 0.0001;
	    double confidence = 0.95;
	    
	    double[] S = {70,141,114,53};
		double[] s = {14,29,58,28};
		
		double initialStock = S[0];
		
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
	    DecimalFormat df = new DecimalFormat("#.00",otherSymbols);
		
		double[] results = SimulatePolicies.simulate_sS(distributions, co, ch, cp, cu, initialStock, S, s, confidence, errorTolerance);
		System.out.println("Simulated cost: "+df.format(results[0])+" Confidence interval=("+df.format(results[0]-results[1])+","+df.format(results[0]+results[1])+")@"+df.format(confidence*100)+"% confidence");
	}
	
	public static double[] simulate_sS(
			Distribution[] demand, 
			double orderCost, 
			double holdingCost, 
			double penaltyCost,
			double unitCost,
			double initialStock,
			double[] S,
			double[] s,
			double confidence,
			double error
			){
		Tally costTally = new Tally();
		Tally[] stockPTally = new Tally[demand.length];
		Tally[] stockNTally = new Tally[demand.length];
		for(int i = 0; i < demand.length; i++) {
			stockPTally[i] = new Tally();
			stockNTally[i] = new Tally();
		}
		
		int minRuns = 1000;
		int maxRuns = 1000000;
		
		double[] centerAndRadius = new double[2];
		for(int i = 0; i < minRuns || (centerAndRadius[1]>=centerAndRadius[0]*error && i < maxRuns); i++){
			double[] demandRealizations = SampleFactory.getInstance().getNextSample(demand);
			
			double replicationCost = 0;
			double inventory = initialStock;
			for(int t = 0; t < demand.length; t++){
				if(inventory <= s[t]){
					replicationCost += orderCost;
					replicationCost += Math.max(0, S[t]-inventory)*unitCost;
					inventory = S[t]-demandRealizations[t];
					replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
				}else{
					inventory = inventory-demandRealizations[t];
					replicationCost += Math.max(inventory, 0)*holdingCost - Math.min(inventory, 0)*penaltyCost;
				}
				stockPTally[t].add(Math.max(inventory, 0));
				stockNTally[t].add(Math.max(-inventory, 0));
			}
			costTally.add(replicationCost);
			if(i >= minRuns) costTally.confidenceIntervalNormal(confidence, centerAndRadius);
		}
		return centerAndRadius;
	}
}

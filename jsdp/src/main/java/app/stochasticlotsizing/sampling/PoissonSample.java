package app.stochasticlotsizing.sampling;

import java.util.Random;

import umontreal.iro.lecuyer.probdist.PoissonDist;

public class PoissonSample {
	
	public static double[][] getPoissonSample(double[] lambda, int points, long seed){
		Random generator = new Random(seed);
        double[][] poissonSample = new double[points][lambda.length];
        for(int i = 0; i < points; i++){
            for(int j = 0; j < lambda.length; j++){
                poissonSample[i][j] = PoissonDist.inverseF(lambda[j], generator.nextDouble());
            }
        }
        return poissonSample;
    }
	
	public static double[][] getPoissonLHSSample(double[] lambda, int points, long seed){
        double[][] sampledProbabilities = LHSampling.latin_random(lambda.length, points, seed);
        double[][] poissonSample = new double[points][lambda.length];
        for(int i = 0; i < points; i++){
            for(int j = 0; j < lambda.length; j++){
            	poissonSample[i][j] = PoissonDist.inverseF(lambda[j], sampledProbabilities[j][i]);
            }
        }
        return poissonSample;
    }
}

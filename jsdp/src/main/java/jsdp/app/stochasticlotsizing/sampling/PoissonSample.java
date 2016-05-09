package jsdp.app.stochasticlotsizing.sampling;

import java.util.Random;

import umontreal.iro.lecuyer.probdist.PoissonDist;
import umontreal.iro.lecuyer.randvar.UniformGen;
import umontreal.iro.lecuyer.randvarmulti.IIDMultivariateGen;
import umontreal.iro.lecuyer.randvarmulti.RandomMultivariateGen;
import umontreal.iro.lecuyer.rng.MRG32k3aL;
import umontreal.iro.lecuyer.rng.RandomStream;

public class PoissonSample {
	
	static RandomStream stream;
	
	static {
		stream = new MRG32k3aL();
	}
	
	public static void resetNextSubstream(){
		stream.resetNextSubstream();
	}
	
	public static void resetStartStream(){
		stream.resetStartStream();
	}
	
	public static void main(String args[]){
		stream.resetStartStream();
		for(double e : PoissonSample.getNextPoissonSample(new double[]{30,20,30})){
			System.out.print(e + " ");
		}
		System.out.println();
		stream.resetNextSubstream();
		for(double e : PoissonSample.getNextPoissonSample(new double[]{10,20,30})){
			System.out.print(e + " ");
		}
	}
	
	public static double[] getNextPoissonSample(double[] lambda){
		UniformGen uniform = new UniformGen(stream);
		RandomMultivariateGen generator = new IIDMultivariateGen(uniform, lambda.length);
		double[] p = new double[lambda.length];
		generator.nextPoint(p);
		double[] sample = new double[lambda.length];
		for(int i = 0; i < p.length; i++){
			sample[i] = PoissonDist.inverseF(lambda[i], p[i]);
		}
		return sample;
	}
	
	public static double[][] getPoissonSampleMatrix(double[] lambda, int points, long seed){
		Random generator = new Random(seed);
        double[][] poissonSample = new double[points][lambda.length];
        for(int i = 0; i < points; i++){
            for(int j = 0; j < lambda.length; j++){
                poissonSample[i][j] = PoissonDist.inverseF(lambda[j], generator.nextDouble());
            }
        }
        return poissonSample;
    }
	
	public static double[][] getPoissonLHSSampleMatrix(double[] lambda, int points, long seed){
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

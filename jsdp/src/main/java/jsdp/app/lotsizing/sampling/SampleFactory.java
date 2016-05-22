package jsdp.app.lotsizing.sampling;

import java.util.stream.IntStream;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.randvar.UniformGen;
import umontreal.ssj.rng.MRG32k3aL;
import umontreal.ssj.rng.RandomStream;

public class SampleFactory {
	private static SampleFactory instance = null;
	
	public static SampleFactory getInstance(){
		return instance == null ? new SampleFactory() : instance;
	}
	
	private RandomStream stream;
	
	public SampleFactory(){
		stream = new MRG32k3aL();
	}
	
	public void resetNextSubstream(){
		stream.resetNextSubstream();
	}
	
	public void resetStartStream(){
		stream.resetStartStream();
	}
	
	public double[] getNextSample(Distribution[] distributions){
		UniformGen uniform = new UniformGen(stream);
		return IntStream.iterate(0, i -> i + 1).limit(distributions.length).mapToDouble(
				i -> distributions[i].inverseF(uniform.nextDouble())
				).toArray();
	}
	
	public double[][] getLHSSampleMatrix(Distribution[] distributions, int samples){
        double[][] sampledProbabilities = LHSampling.latin_random(distributions.length, samples, stream);
        double[][] poissonSample = new double[samples][distributions.length];
        for(int i = 0; i < samples; i++){
            for(int j = 0; j < distributions.length; j++){
            	poissonSample[i][j] = distributions[i].inverseF(sampledProbabilities[j][i]);
            }
        }
        return poissonSample;
    }
}

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

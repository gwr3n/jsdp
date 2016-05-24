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

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.randvar.UniformGen;
import umontreal.ssj.randvar.UniformIntGen;
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
	
	private double[] shuffle(double[] sample){
		for(int i = 0; i < sample.length; i++){
			int j = UniformIntGen.nextInt(stream, 0, sample.length - 1);
			double temp = sample[i];
			sample[i] = sample[j];
			sample[j] = temp;
		}
		return sample;
	}
	
	public double[][] getNextLHSample(Distribution[] distributions, int samples){
		double x[][] = new double[distributions.length][samples];
		x = IntStream.iterate(0, d -> d + 1)
					 .limit(distributions.length)
					 .mapToObj(
							 d -> DoubleStream.iterate(0, i -> i + 1.0/samples)
							 				  .limit(samples)
							 				  .map(i -> distributions[d].inverseF(i + UniformGen.nextDouble(stream, 0, 1.0/samples)))
							 				  .toArray())
					 .toArray(double[][]::new);	
		for(int i = 0; i < x.length; i++){
			shuffle(x[i]);
		}
		return x;
	}
}

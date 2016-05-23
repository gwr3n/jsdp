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

import umontreal.ssj.randvar.UniformGen;
import umontreal.ssj.randvar.UniformIntGen;
import umontreal.ssj.rng.MRG32k3aL;
import umontreal.ssj.rng.RandomStream;

public class LHSampling {

	public static double[][] latin_random_matrix ( int dim_num, int point_num, RandomStream stream ){
        double x[][] = new double[dim_num][point_num];
        int [] perm;

        for (int i = 0; i < dim_num; i++ )
        {
            perm = perm_random ( point_num, stream );

        for (int j = 0; j < point_num; j++ )
        {
          x[i][j] = perm[j] - 1;
        }
      }
      return x;
    }
	
    public static double[][] latin_random ( int dim_num, int point_num, RandomStream stream ){
        double x[][] = new double[dim_num][point_num];
        int [] perm;

        for (int i = 0; i < dim_num; i++ )
        {
            perm = perm_random ( point_num, stream );

        for (int j = 0; j < point_num; j++ )
        {
          x[i][j] = ( ( ( double ) ( perm[j] - 1 ) ) + UniformGen.nextDouble(stream, 0, 1) ) / ( ( double ) point_num );
        }
      }
      return x;
    }

    static int[] perm_random ( int point_num, RandomStream stream){
        int[] perm = new int[point_num];

        for (int i = 0; i < point_num; i++) {
            perm[i] = i + 1;
        }

        for (int i = 1; i <= point_num; i++) {
            int j = UniformIntGen.nextInt(stream, i, point_num); 
            int swap = perm[i - 1];
            perm[i - 1] = perm[j - 1];
            perm[j - 1] = swap;
        }

        return perm;
    }

    static void test(){
        int dimNumber = 4;
        int pointNumber = 4;
        RandomStream  stream = new MRG32k3aL();

        double[][] latin = latin_random(dimNumber, pointNumber, stream);

        for(int j = 0; j < pointNumber; j++){
            for(int i = 0; i < dimNumber; i++){
                System.out.print((""+latin[i][j]).substring(0,5)+ "\t");
            }
            System.out.println();
        }
    }

    public static void main(String args[]){
        LHSampling.test();
    }
}

package net.imglib2.blk;

import java.util.Arrays;
import java.util.Random;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.util.Intervals;

class RandomSourceData
{
	final int[] sourceSize;
	final double[] source;

	public RandomSourceData( final int[] targetSize, final double[] sigmas )
	{
		final int n = targetSize.length; // expected: 3

		final int[] sizes = Gauss3.halfkernelsizes( sigmas );
		sourceSize = new int[ n ];
		Arrays.setAll( sourceSize, d -> targetSize[ d ] + 2 * ( sizes[ d ] - 1 ) );

		source = new double[ ( int ) Intervals.numElements( sourceSize ) ];
		final Random random = new Random( 1 );
		Arrays.setAll( source, i -> random.nextDouble() );
	}
}

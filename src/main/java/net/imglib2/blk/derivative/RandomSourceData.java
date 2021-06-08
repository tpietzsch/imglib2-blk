package net.imglib2.blk.derivative;

import java.util.Arrays;
import java.util.Random;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.util.Intervals;

class RandomSourceData
{
	final int[] sourceSize;
	final double[] source;

	public RandomSourceData( final int[] targetSize, final Kernel1D[] kernels )
	{
		final int n = targetSize.length; // expected: 3

		sourceSize = new int[ n ];
		Arrays.setAll( sourceSize, d -> {
			final Kernel1D kernel = kernels[ d ];
			final int pad = kernel == null ? 0 : ( kernel.size() - 1 );
			return targetSize[ d ] + pad;
		}  );

		source = new double[ ( int ) Intervals.numElements( sourceSize ) ];
		final Random random = new Random( 1 );
		Arrays.setAll( source, i -> random.nextDouble() );
	}
}

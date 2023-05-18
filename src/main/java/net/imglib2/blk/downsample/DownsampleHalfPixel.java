package net.imglib2.blk.downsample;

import java.util.Arrays;

import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;

public class DownsampleHalfPixel
{
	public static long[] getDownsampledDimensions( final long[] imgDimensions, final boolean[] downsampleInDim )
	{
		final int n = imgDimensions.length;
		if ( downsampleInDim.length != n )
			throw new IllegalArgumentException();
		final long[] destSize = new long[ n ];
		Arrays.setAll( destSize, d -> downsampleInDim[ d ] ? ( imgDimensions[ d ] + 1 ) / 2 : imgDimensions[ d ] );
		return destSize;
	}

	private static int mulDims( int[] dims, int from, int to )
	{
		int product = 1;
		for ( int d = from; d < to; ++d )
			product *= dims[ d ];
		return product;
	}

}

package net.imglib2.blk.downsample;

import java.util.Arrays;
import net.imglib2.blocks.TempArray;
import net.imglib2.util.Intervals;

import static net.imglib2.type.PrimitiveType.FLOAT;

public class DownsampleFloat
{
	private final int n;
	private final int[] destSize;
	private final int[] sourceSize;
	private final int[] sourceOffset;

	private final boolean[] downsampleInDim;
	private final int[] downsampleDims;
	private final int steps;

	// sources for every per-dimension downsampling step.
	// dest is the tempArray of the next step, or final dest for the last step.
	// tempArrays[0] can be used to copy the source block into.
	private final TempArray<float[]> tempArrays[];
	private final int[] tempArraySizes;

	public DownsampleFloat( final boolean[] downsampleInDim )
	{
		n = downsampleInDim.length;
		destSize = new int[ n ];
		sourceSize = new int[ n ];
		sourceOffset = new int[ n ];
		Arrays.setAll( sourceOffset, d -> downsampleInDim[ d ] ? -1 : 0 );

		this.downsampleInDim = downsampleInDim;
		downsampleDims = downsampleDimIndices( downsampleInDim );
		steps = downsampleDims.length;

		tempArrays = new TempArray[ steps ];
		tempArrays[ 0 ] = TempArray.forPrimitiveType( FLOAT );
		if ( steps >= 2 )
		{
			tempArrays[ 1 ] = TempArray.forPrimitiveType( FLOAT );
			if ( steps >= 3 )
			{
				tempArrays[ 2 ] = TempArray.forPrimitiveType( FLOAT );
				for ( int i = 3; i < steps; ++i )
					tempArrays[ i ] = tempArrays[ i - 2 ];
			}
		}

		tempArraySizes = new int[ steps ];
	}

	private static int[] downsampleDimIndices( final boolean[] downsampleInDim )
	{
		final int n = downsampleInDim.length;
		final int[] dims = new int[ n ];
		int j = 0;
		for ( int i = 0; i < n; i++ )
			if ( downsampleInDim[ i ] )
				dims[ j++ ] = i;
		return Arrays.copyOf( dims, j );
	}

	public void setTargetSize( final int[] destSize )
	{
		if ( Arrays.equals( destSize, this.destSize ) )
			return;

		for ( int d = 0; d < n; d++ )
		{
			this.destSize[ d ] = destSize[ d ];
			sourceSize[ d ] = downsampleInDim[ d ] ? destSize[ d ] * 2 + 1 : destSize[ d ];
		}

		final int[] itSrcSize = sourceSize.clone();
		for ( int i = 0; i < steps; ++i )
		{
			tempArraySizes[ i ] = ( int ) Intervals.numElements( itSrcSize );
			itSrcSize[ downsampleDims[ i ] ] = destSize[ downsampleDims[ i ] ];
		}
	}

	public int[] getSourceSize()
	{
		return sourceSize;
	}

	public int[] getSourceOffset()
	{
		return sourceOffset;
	}

	// optional. also other arrays can be passed to compute()
	public float[] getSourceBuffer()
	{
		return getSourceBuffer( 0 );
	}

	private float[] getSourceBuffer( int i )
	{
		return tempArrays[ i ].get( tempArraySizes[ i ] );
	}

	public void compute( final float[] src, final float[] dest )
	{
		float[] itSrc = src;
		final int[] itDestSize = sourceSize.clone();
		for ( int i = 0; i < steps; ++i )
		{
			final int d = downsampleDims[ i ];
			itDestSize[ d ] = destSize[ d ];
			final boolean lastStep = ( i == steps - 1 );
			final float[] itDest = lastStep ? dest : getSourceBuffer( i + 1 );
			downsample( itSrc, itDestSize, itDest, d );
			itSrc = itDest;
		}
	}

	public static long[] getDownsampledDimensions( final long[] imgDimensions, final boolean[] downsampleInDim )
	{
		final int n = imgDimensions.length;
		if ( downsampleInDim.length != n )
			throw new IllegalArgumentException();
		final long[] destSize = new long[ n ];
		Arrays.setAll( destSize, d -> downsampleInDim[ d ] ? ( imgDimensions[ d ] + 1 ) / 2 : imgDimensions[ d ] );
		return destSize;
	}

	static void downsample( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		if ( dim == 0 )
			downsampleX( source, destSize, dest );
		else
			downsampleN( source, destSize, dest, dim );
	}

	private static void downsampleX( final float[] source, final int[] destSize, final float[] dest )
	{
		final int destLineLength = destSize[ 0 ];

		int nLines = 1;
		for ( int d = 1; d < destSize.length; ++d )
			nLines *= destSize[ d ];

		for ( int y = 0; y < nLines; ++y )
		{
			final int destOffset = y * destLineLength;
			for ( int x = 0; x < destLineLength; ++x )
			{
				final int si = 2 * ( destOffset + x );
				dest[ destOffset + x ] =
						0.25f * source[ si ] +
								0.5f * source[ si + 1 ] +
								0.25f * source[ si + 2 ];
			}
		}
	}

	private static void downsampleN( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		int lineLength = 1;
		for ( int d = 0; d < dim; ++d )
			lineLength *= destSize[ d ];

		int nLines = 1;
		for ( int d = dim; d < destSize.length; ++d )
			nLines *= destSize[ d ];

		for ( int y = 0; y < nLines; ++y )
		{
			final int destOffset = y * lineLength;
			final int srcOffset = 2 * destOffset;
			for ( int x = 0; x < lineLength; ++x )
			{
				dest[ destOffset + x ] = 0.25f * source[ srcOffset + x ] +
						0.5f * source[ srcOffset + x + lineLength ] +
						0.25f * source[ srcOffset + x + 2 * lineLength ];
			}
		}
	}
}

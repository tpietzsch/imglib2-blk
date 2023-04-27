package net.imglib2.blk.downsample;

import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.Interval;
import net.imglib2.blk.downsample.algo.BlockProcessor;
import net.imglib2.blocks.TempArray;
import net.imglib2.util.Intervals;

import static net.imglib2.type.PrimitiveType.FLOAT;

public class DownsampleFloat implements BlockProcessor< float[] >
{
	private final int n;
	private final int[] destSize;
	private final long[] sourcePos;
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
		sourcePos = new long[ n ];
		Arrays.setAll( sourceOffset, d -> downsampleInDim[ d ] ? -1 : 0 );

		this.downsampleInDim = downsampleInDim;
		downsampleDims = downsampleDimIndices( downsampleInDim );
		steps = downsampleDims.length;

		tempArrays = createTempArrays( steps );
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

	private static TempArray<float[]>[] createTempArrays( final int steps )
	{
		final TempArray< float[] > tempArrays[] = new TempArray[ steps ];
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
		return tempArrays;
	}

	private DownsampleFloat( DownsampleFloat downsample )
	{
		// re-use
		n = downsample.n;
		sourceOffset = downsample.sourceOffset;
		downsampleInDim = downsample.downsampleInDim;
		downsampleDims = downsample.downsampleDims;
		steps = downsample.steps;

		// init empty
		destSize = new int[ n ];
		sourcePos = new long[ n ];
		sourceSize = new int[ n ];
		tempArraySizes = new int[ steps ];

		// init new instance
		tempArrays = createTempArrays( steps );
	}

	public DownsampleFloat newInstance()
	{
		return new DownsampleFloat( this );
	}

	@Override
	public Supplier< DownsampleFloat > threadSafeSupplier()
	{
		// TODO make idempotent: always return the same ThreadLocal, e.g., threadSafeSupplier().get().threadSafeSupplier() == threadSafeSupplier()
		return ThreadLocal.withInitial( this::newInstance )::get;
	}

	// TODO REMOVE
	void setTargetSize( final int[] destSize )
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

	@Override
	public void setTargetInterval( final Interval interval )
	{
		boolean destSizeChanged = false;
		for ( int d = 0; d < n; ++d )
		{
			final long tpos = interval.min( d );
			// TODO: sourceOffset directly here...
			sourcePos[ d ] = downsampleInDim[ d ] ? tpos * 2 + sourceOffset[ d ] : tpos;

			final int tdim = safeInt( interval.dimension( d ) );
			if ( tdim != destSize[ d ] )
			{
				destSize[ d ] = tdim;
				sourceSize[ d ] = downsampleInDim[ d ] ? tdim * 2 + 1 : tdim;
				destSizeChanged = true;
			}
		}

		if ( destSizeChanged )
		{
			int size = safeInt( Intervals.numElements( sourceSize ) );
			tempArraySizes[ 0 ] = size;
			for ( int i = 1; i < steps; ++i )
			{
				final int d = downsampleDims[ i - 1 ];
				size = size / sourceSize[ d ] * destSize[ d ];
				tempArraySizes[ i ] = size;
			}
		}
	}

	private static int safeInt( final long value )
	{
		if ( value > Integer.MAX_VALUE )
			throw new IllegalArgumentException( "value too large" );
		return ( int ) value;
	}

	@Override
	public int[] getSourceSize()
	{
		return sourceSize;
	}

	@Override
	public long[] getSourcePos()
	{
		return sourcePos;
	}

	// TODO REMOVE
	int[] getSourceOffset()
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

	@Override
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

	// TODO: make private
	static void downsample( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		if ( dim == 0 )
			downsampleX( source, destSize, dest );
		else
			downsampleN( source, destSize, dest, dim );
	}

	// TODO: make private
	static void downsampleX( final float[] source, final int[] destSize, final float[] dest )
	{
		final int destLineLength = destSize[ 0 ];
		final int srcLineLength = 2 * destSize[ 0 ] + 1;

		int nLines = 1;
		for ( int d = 1; d < destSize.length; ++d )
			nLines *= destSize[ d ];

		for ( int y = 0; y < nLines; ++y )
		{
			final int destOffset = y * destLineLength;
			final int srcOffset = y * srcLineLength;
			for ( int x = 0; x < destLineLength; ++x )
			{
				final int si = srcOffset + 2 * x;
				dest[ destOffset + x ] =
						0.25f * source[ si ] +
								0.5f * source[ si + 1 ] +
								0.25f * source[ si + 2 ];
			}
		}
	}

	// TODO: make private
	static void downsampleN( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		int lineLength = 1;
		for ( int d = 0; d < dim; ++d )
			lineLength *= destSize[ d ];

		final int nLines = destSize[ dim ];

		int nPlanes = 1;
		for ( int d = dim + 1; d < destSize.length; ++d )
			nPlanes *= destSize[ d ];

		for ( int z = 0; z < nPlanes; ++z )
		{
			for ( int y = 0; y < nLines; ++y )
			{
				final int destOffset = ( z * nLines * lineLength ) + ( y * lineLength );
				final int srcOffset = ( z * ( 2 * nLines + 1 ) * lineLength ) + ( 2 * y * lineLength );
				for ( int x = 0; x < lineLength; ++x )
				{
					dest[ destOffset + x ] = 0.25f * source[ srcOffset + x ] +
							0.5f * source[ srcOffset + x + lineLength ] +
							0.25f * source[ srcOffset + x + 2 * lineLength ];
				}
			}
		}
	}
}

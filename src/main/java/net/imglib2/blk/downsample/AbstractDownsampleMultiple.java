package net.imglib2.blk.downsample;

import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.Interval;
import net.imglib2.blk.downsample.algo.BlockProcessor;
import net.imglib2.blocks.TempArray;
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Intervals;

abstract class AbstractDownsampleMultiple< T extends AbstractDownsampleMultiple< T, P, Q >, P, Q > implements BlockProcessor< P, P >
{
	final PrimitiveType primitiveTypeInOut;
	final PrimitiveType primitiveTypeAux;

	final int n;
	final int[] destSize;
	final long[] sourcePos;
	final int[] sourceSize;

	final boolean[] downsampleInDim;
	final int[] downsampleDims;
	final int steps;

	// sources for every per-dimension downsampling step.
	// dest is the tempArray of the next step, or final dest for the last step.
	// tempArrays[0] can be used to copy the source block into.
	private final TempArray< ? > tempArrays[];
	final int[] tempArraySizes;

	Supplier< T > threadSafeSupplier;

	AbstractDownsampleMultiple( final boolean[] downsampleInDim, final PrimitiveType primitiveTypeInOut, final PrimitiveType primitiveTypeAux )
	{
		n = downsampleInDim.length;
		this.primitiveTypeInOut = primitiveTypeInOut;
		this.primitiveTypeAux = primitiveTypeAux;
		destSize = new int[ n ];
		sourceSize = new int[ n ];
		sourcePos = new long[ n ];

		this.downsampleInDim = downsampleInDim;
		downsampleDims = downsampleDimIndices( downsampleInDim );
		steps = downsampleDims.length;

		tempArrays = createTempArrays( steps, primitiveTypeInOut, primitiveTypeAux );
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

	private static < P > TempArray< P >[] createTempArrays( final int steps, final PrimitiveType primitiveTypeInOut, final PrimitiveType primitiveTypeAux )
	{
		final TempArray< P > tempArrays[] = new TempArray[ steps ];
		tempArrays[ 0 ] = TempArray.forPrimitiveType( primitiveTypeInOut );
		if ( steps >= 2 )
		{
			tempArrays[ 1 ] = TempArray.forPrimitiveType( primitiveTypeAux );
			if ( steps >= 3 )
			{
				tempArrays[ 2 ] = TempArray.forPrimitiveType( primitiveTypeAux );
				for ( int i = 3; i < steps; ++i )
					tempArrays[ i ] = tempArrays[ i - 2 ];
			}
		}
		return tempArrays;
	}

	AbstractDownsampleMultiple( T downsample )
	{
		// re-use
		primitiveTypeInOut = downsample.primitiveTypeInOut;
		primitiveTypeAux = downsample.primitiveTypeAux;
		n = downsample.n;
		downsampleInDim = downsample.downsampleInDim;
		downsampleDims = downsample.downsampleDims;
		steps = downsample.steps;
		threadSafeSupplier = downsample.threadSafeSupplier;

		// init empty
		destSize = new int[ n ];
		sourcePos = new long[ n ];
		sourceSize = new int[ n ];
		tempArraySizes = new int[ steps ];

		// init new instance
		tempArrays = createTempArrays( steps, primitiveTypeInOut, primitiveTypeAux );
	}

	abstract T newInstance();

	@Override
	public synchronized Supplier< T > threadSafeSupplier()
	{
		if ( threadSafeSupplier == null )
			threadSafeSupplier = ThreadLocal.withInitial( this::newInstance )::get;
		return threadSafeSupplier;
	}

	@Override
	public void setTargetInterval( final Interval interval )
	{
		boolean destSizeChanged = false;
		for ( int d = 0; d < n; ++d )
		{
			final long tpos = interval.min( d );
			sourcePos[ d ] = downsampleInDim[ d ] ? tpos * 2 - 1 : tpos;

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
				final int d = downsampleDims[ steps - i ];
				size = size / sourceSize[ d ] * destSize[ d ];
				tempArraySizes[ i ] = size;
			}
		}
	}

	static int safeInt( final long value )
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

	// optional. also other arrays can be passed to compute()
	@Override
	public P getSourceBuffer()
	{
		return getSourceBuffer( 0 );
	}

	@SuppressWarnings( "unchecked" )
	private < T > T getSourceBuffer( int i )
	{
		return ( T ) ( tempArrays[ i ].get( tempArraySizes[ i ] ) );
	}

	@Override
	public void compute( final P src, final P dest )
	{
		final int[] itDestSize = sourceSize.clone();
		for ( int i = 0; i < steps; ++i )
		{
			final int d = downsampleDims[ steps - i - 1 ];
			itDestSize[ d ] = destSize[ d ];
			final boolean firstStep = ( i == 0 );
			final boolean lastStep = ( i == steps - 1 );
			if ( firstStep && lastStep )
			{
				downsamplePP( src, itDestSize, dest, d );
			}
			else if ( firstStep )
			{
				final Q itDest = getSourceBuffer( i + 1 );
				downsamplePQ( src, itDestSize, itDest, d );
			}
			else if ( lastStep )
			{
				final Q itSrc = getSourceBuffer( i );
				downsampleQP( itSrc, itDestSize, dest, d );
			}
			else
			{
				final Q itSrc = getSourceBuffer( i );
				final Q itDest = getSourceBuffer( i + 1 );
				downsampleQQ( itSrc, itDestSize, itDest, d );
			}
		}
	}

	abstract void downsamplePP( final P source, final int[] destSize, final P dest, final int dim );

	abstract void downsamplePQ( final P source, final int[] destSize, final Q dest, final int dim );

	abstract void downsampleQQ( final Q source, final int[] destSize, final Q dest, final int dim );

	abstract void downsampleQP( final Q source, final int[] destSize, final P dest, final int dim );
}

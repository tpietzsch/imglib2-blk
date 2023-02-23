package net.imglib2.blk.copy;

import net.imglib2.blk.copy.ViewBlocksPlayground.ViewProperties;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;

import static net.imglib2.blk.copy.PrimitiveBlocksUtils.extractOobValue;

class RandomAccessibleBlocks< T extends NativeType< T >, R extends NativeType< R > > implements PrimitiveBlocks< T >
{
	private final PermuteInvert permuteInvert;

	private final ViewProperties< T, R > props;

	private final RangeCopier copier;

	private final TempArray< R > tempArray;

	public RandomAccessibleBlocks( final ViewProperties< T, R > props )
	{
		this.props = props;
		final PrimitiveType primitiveType = props.getRootType().getNativeTypeFactory().getPrimitiveType();
		final MemCopy memCopy = MemCopy.forPrimitiveType( primitiveType );
		final Object oob = extractOobValue( props.getRootType(), props.getExtension() );
		final Ranges findRanges = Ranges.forExtension( props.getExtension() );
		copier = RangeCopier.create( props.getRoot(), findRanges, memCopy, oob );
		tempArray = Cast.unchecked( TempArray.forPrimitiveType( primitiveType ) );
		permuteInvert = new PermuteInvert( memCopy, props.getPermuteInvertTransform() );
	}

	@Override
	public T getType()
	{
		return props.getViewType();
	}

	/**
	 * @param srcPos
	 * 		min coordinates of block to copy from src Img.
	 * @param dest
	 * 		destination array. Type is {@code byte[]}, {@code float[]},
	 * 		etc, corresponding to the src Img's native type.
	 * @param size
	 * 		dimensions of block to copy from src Img.
	 */
	public void copy( final int[] srcPos, final Object dest, final int[] size )
	{
		final MixedTransform transform = props.getTransform();
		final int n = transform.numTargetDimensions();
		final int[] destPos = new int[ n ];
		final int[] destSize = new int[ n ];
		for ( int d = 0; d < n; d++ )
		{
			final int t = ( int ) transform.getTranslation( d );
			if ( transform.getComponentZero( d ) )
			{
				destPos[ d ] = t;
				destSize[ d ] = 1;
			}
			else
			{
				final int c = transform.getComponentMapping( d );
				destPos[ d ] = transform.getComponentInversion( d )
						? t - srcPos[ c ] - size[ c ] + 1
						: t + srcPos[ c ];
				destSize[ d ] = size[ c ];
			}
		}

		copier.copy( destPos, dest, destSize );

		final int length = ( int ) Intervals.numElements( size );
		final Object temp = tempArray.get( length );
		permuteInvert.permuteAndInvert( dest, temp, size );
		System.arraycopy( temp, 0, dest, 0, length );
	}

	static class PermuteInvert
	{
		private final MemCopy memCopy;

		private final int n;

		private final int[] scomp;

		private final boolean[] sinv;

		private final int[] ssize;

		private final int[] ssteps;

		private final int[] tsteps;

		private final int[] csteps;

		private int cstart;

		public PermuteInvert( final MemCopy memCopy, MixedTransform transform )
		{
			this.memCopy = memCopy;
			this.n = transform.numSourceDimensions();
			scomp = new int[ n ];
			sinv = new boolean[ n ];
			transform.getComponentMapping( scomp );
			transform.getComponentInversion( sinv );
			ssize = new int[ n ];
			ssteps = new int[ n ];
			tsteps = new int[ n ];
			csteps = new int[ n ];
		}

		// TODO: Object --> T
		public void permuteAndInvert( Object src, Object dest, int[] destSize  )
		{
			final int[] tsize = destSize;

			for ( int d = 0; d < n; ++d )
				ssize[ d ] = tsize[ scomp[ d ] ];

			ssteps[ 0 ] = 1;
			for ( int d = 0; d < n - 1; ++d )
				ssteps[ d + 1 ] = ssteps[ d ] * ssize[ d ];

			tsteps[ 0 ] = 1;
			for ( int d = 0; d < n - 1; ++d )
				tsteps[ d + 1 ] = tsteps[ d ] * tsize[ d ];

			cstart = 0;
			for ( int d = 0; d < n; ++d )
			{
				final int c = scomp[ d ];
				csteps[ d ] = sinv[ d ] ? -tsteps[ c ] : tsteps[ c ];
				cstart += sinv[ d ] ? ( tsize[ c ] - 1 ) * tsteps[ c ] : 0;
			}

			copyRecursively( src, 0, dest, cstart, n - 1 );
		}

		private void copyRecursively( final Object src, final int srcPos, final Object dest, final int destPos, final int d )
		{
			if ( d == 0 )
			{
				// TODO add strided copy to MemCopy
				final int length = ssize[ d ];
				final int stride = csteps[ d ];
				for ( int i = 0; i < length; ++i )
					memCopy.copyValue( src, srcPos + i, dest, destPos + i * stride, 1 );
			}
			else
			{
				final int length = ssize[ d ];
				final int srcStride = ssteps[ d ];
				final int destStride = csteps[ d ];
				for ( int i = 0; i < length; ++i )
					copyRecursively( src, srcPos + i * srcStride, dest, destPos + i * destStride, d - 1 );
			}
		}

		// creates an independent copy of {@code copier}
		private PermuteInvert( PermuteInvert copier )
		{
			memCopy = copier.memCopy;
			n = copier.n;
			scomp = copier.scomp;
			sinv = copier.sinv;
			ssize = new int[ n ];
			ssteps = new int[ n ];
			tsteps = new int[ n ];
			csteps = new int[ n ];
		}

		PermuteInvert newInstance()
		{
			return new PermuteInvert( this );
		}
	}


	@Override
	public PrimitiveBlocks< T > threadSafe()
	{
		return PrimitiveBlocksUtils.threadSafe( this::newInstance );
	}

	RandomAccessibleBlocks< T, R > newInstance()
	{
		return new RandomAccessibleBlocks<>( this );
	}

	private RandomAccessibleBlocks( final RandomAccessibleBlocks< T, R > blocks )
	{
		props = blocks.props;
		copier = blocks.copier.newInstance();
		permuteInvert = blocks.permuteInvert.newInstance();
		tempArray = blocks.tempArray.newInstance();
	}
}

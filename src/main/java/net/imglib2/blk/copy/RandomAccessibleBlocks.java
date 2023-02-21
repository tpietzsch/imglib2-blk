package net.imglib2.blk.copy;

import java.util.List;
import net.imglib2.blk.copy.ViewBlocksPlayground.ViewProperties;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;

import static net.imglib2.blk.copy.PrimitiveBlocksUtils.extractOobValue;
import static net.imglib2.blk.copy.PrimitiveBlocksUtils.invPermutationInversion;

class RandomAccessibleBlocks< T extends NativeType< T >, R extends NativeType< R > > implements PrimitiveBlocks< T >
{
	private final PermuteInvert permuteInvert;

	private ViewProperties< T, R > props;

	private final RangeCopier copier;

	public RandomAccessibleBlocks( final ViewProperties< T, R > props )
	{
		this.props = props;
		final PrimitiveType primitiveType = props.getRootType().getNativeTypeFactory().getPrimitiveType();
		final MemCopy memCopy = MemCopy.forPrimitiveType( primitiveType );
		final Object oob = extractOobValue( props.getRootType(), props.getExtension() );
		final Ranges findRanges = Ranges.forExtension( props.getExtension() );
		copier = RangeCopier.create( props.getRoot(), findRanges, memCopy, oob );
		final MixedTransform invPermuteInvertTransform = invPermutationInversion( props.getPermuteInvertTransform() );
		permuteInvert = new PermuteInvert( memCopy, invPermuteInvertTransform );
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
			else {
				final int c = transform.getComponentMapping( d );
				final boolean i = transform.getComponentInversion( d );
				destPos[ d ] = ( i ? size[ c ] - srcPos[ c ] + 1 : srcPos[ c ] ) + t;
				destSize[ d ] = size[ c ];
			}
		}

		copier.copy( destPos, dest, destSize );

		// TODO create temporary memory
		permuteInvert.permuteAndInvert(  );

	}

	static class PermuteInvert
	{
		private final MemCopy memCopy;

		private MixedTransform transform;

		private final int n;

		private int[] ssize;
		private final int[] ssteps;
		private final int[] tsize;
		private final int[] tsteps;
		private final int[] scomp;
		private final boolean[] sinv;
		private final int[] csteps;
		private int cstart;

		public PermuteInvert( final MemCopy memCopy, MixedTransform transform )
		{
			this.memCopy = memCopy;
			this.transform = transform;
			this.n = transform.numSourceDimensions();
			ssteps = new int[ n ];
			tsize = new int[ n ];
			tsteps = new int[ n ];
			scomp = new int[ n ];
			sinv = new boolean[ n ];
			transform.getComponentMapping( scomp );
			transform.getComponentInversion( sinv );
			csteps = new int[ n ];
		}

		// TODO: Object --> T
		public void permuteAndInvert( Object src, int[] srcSize, Object dest )
		{
			ssize = srcSize;

			for ( int d = 0; d < n; ++d )
				tsize[ scomp[ d ] ] = ssize[ d ];

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
			transform = copier.transform;
			n = copier.n;
			ssteps = new int[ n ];
			tsize = new int[ n ];
			tsteps = new int[ n ];
			scomp = copier.scomp.clone();
			sinv = copier.sinv.clone();
			csteps = new int[ n ];
		}

		PermuteInvert newInstance() {
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
		permuteInvert = blocks.permuteInvert.newInstance()
	}
}

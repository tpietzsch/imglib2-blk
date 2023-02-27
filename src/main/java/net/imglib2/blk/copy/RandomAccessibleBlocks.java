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

	private final TempArray< R > tempArrayPermute;

	public RandomAccessibleBlocks( final ViewProperties< T, R > props )
	{
		this.props = props;
		final PrimitiveType primitiveType = props.getRootType().getNativeTypeFactory().getPrimitiveType();
		final MemCopy memCopy = MemCopy.forPrimitiveType( primitiveType );
		final Object oob = extractOobValue( props.getRootType(), props.getExtension() );
		final Ranges findRanges = Ranges.forExtension( props.getExtension() );
		copier = RangeCopier.create( props.getRoot(), findRanges, memCopy, oob );
		tempArrayPermute = Cast.unchecked( TempArray.forPrimitiveType( primitiveType ) );
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

		final boolean permute = props.hasPermuteInvertTransform();
		final Object copyDest;
		if ( permute )
		{
			final int length = ( int ) Intervals.numElements( size );
			copyDest = tempArrayPermute.get( length );
		}
		else
			copyDest = dest;

		copier.copy( destPos, copyDest, destSize );

		if ( permute )
		{
			permuteInvert.permuteAndInvert( copyDest, dest, size );
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
		tempArrayPermute = blocks.tempArrayPermute.newInstance();
	}
}

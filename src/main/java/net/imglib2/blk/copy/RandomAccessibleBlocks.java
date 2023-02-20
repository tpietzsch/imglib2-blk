package net.imglib2.blk.copy;

import net.imglib2.blk.copy.ViewBlocksPlayground.ViewProperties;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;

import static net.imglib2.blk.copy.PrimitiveBlocksUtils.extractOobValue;

class RandomAccessibleBlocks< T extends NativeType< T >, R extends NativeType< R > > implements PrimitiveBlocks< T >
{
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
		copier.copy( srcPos, dest, size );
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
	}
}

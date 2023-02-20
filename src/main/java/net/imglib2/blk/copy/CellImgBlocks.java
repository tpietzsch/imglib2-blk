package net.imglib2.blk.copy;

import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.type.NativeType;

import static net.imglib2.blk.copy.PrimitiveBlocksUtils.extractOobValue;

public class CellImgBlocks< T extends NativeType< T > > implements PrimitiveBlocks< T >
{
	private final T type;

	private final CellImgRangeCopier copier;

	public CellImgBlocks( final AbstractCellImg< T, ?, ?, ? > img, final Extension extension )
	{
		type = img.createLinkedType();
		if ( type.getEntitiesPerPixel().getRatio() != 1 )
			throw new IllegalArgumentException( "Types with entitiesPerPixel != 1 are not supported" );
		final MemCopy memCopy = MemCopy.forPrimitiveType( type.getNativeTypeFactory().getPrimitiveType() );
		final Object oob = extractOobValue( type, extension );
		final Ranges findRanges = Ranges.forExtension( extension );
		copier = new CellImgRangeCopier( img, findRanges, memCopy, oob );
	}

	@Override
	public T getType()
	{
		return type;
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

	CellImgBlocks< T > newInstance()
	{
		return new CellImgBlocks<>( this );
	}

	private CellImgBlocks( final CellImgBlocks< T > blocks )
	{
		type = blocks.type;
		copier = blocks.copier.newInstance();
	}
}

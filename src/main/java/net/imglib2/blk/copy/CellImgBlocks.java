package net.imglib2.blk.copy;

import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.type.NativeType;

public class CellImgBlocks
{
	private final ThreadLocal< RangeCopier > copier;

	public CellImgBlocks( final AbstractCellImg< ?, ?, ? extends Cell< ? >, ? > cellImg, Extension extension )
	{
		this( cellImg, extension, ( byte ) 0 );
	}

	// TODO: CONSTANT extension method should have value parameter. Would be good use-case for sealed classes instead of enum.
	public CellImgBlocks( final AbstractCellImg< ?, ?, ? extends Cell< ? >, ? > cellImg, Extension extension, final byte oobValue )
	{
		// TODO: store type, verify dest array type in copy(...)
		final NativeType< ? > type = cellImg.createLinkedType();
//		System.out.println( "type = " + type.getClass() );

		final Ranges findRanges = Ranges.forExtension( extension );
		final MemCopy memCopy = MemCopy.BYTE;
		final byte[] oob = new byte[] { oobValue };
		copier = ThreadLocal.withInitial( () -> new RangeCopier( cellImg, findRanges, memCopy, oob ) );
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
		copier.get().copy( srcPos, dest, size );
	}

}

package net.imglib2.blk.copy;

import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class CellImgBlocks< T extends NativeType< T > >
{
	private final ThreadLocal< RangeCopier > copier;

	// TODO: This was added out of laziness. Probably remove...
	private final AbstractCellImg< T, ?, ? extends Cell< ? >, ? > source;

	public CellImgBlocks( final AbstractCellImg< T, ?, ? extends Cell< ? >, ? > cellImg, Extension extension )
	{
		this( cellImg, extension, null );
	}

	// TODO: CONSTANT extension method should have value parameter. Would be good use-case for sealed classes instead of enum.
	public CellImgBlocks( final AbstractCellImg< T, ?, ? extends Cell< ? >, ? > cellImg, Extension extension, final T oobValue )
	{
		// TODO: store type, verify dest array type in copy(...)
		final T type = cellImg.createLinkedType();
		final MemCopy memCopy;
		final Object oob;
		if ( type instanceof UnsignedByteType )
		{
			memCopy = MemCopy.BYTE;
			final byte v = oobValue == null ? 0 : ( ( UnsignedByteType ) oobValue ).getByte();
			oob = new byte[] { v };
		}
		else if ( type instanceof FloatType )
		{
			memCopy = MemCopy.FLOAT;
			final float v = oobValue == null ? 0 : ( ( FloatType ) oobValue ).get();
			oob = new float[] { v };
		}
		else if ( type instanceof DoubleType )
		{
			memCopy = MemCopy.DOUBLE;
			final double v = oobValue == null ? 0 : ( ( DoubleType ) oobValue ).get();
			oob = new double[] { v };
		}
		else
			throw new IllegalArgumentException( type.getClass() + " is not supported" );

		final Ranges findRanges = Ranges.forExtension( extension );
		copier = ThreadLocal.withInitial( () -> new RangeCopier( cellImg, findRanges, memCopy, oob ) );

		source = cellImg;
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

	// TODO: This was added out of laziness. It probably should not be in the final API.
	public AbstractCellImg< T, ?, ? extends Cell< ? >, ? > source()
	{
		return source;
	}
}

package net.imglib2.blk.copy;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

class ArrayImgBlocks< T extends NativeType< T > > implements PrimitiveBlocks< T >
{
	private final T type;

	private final ThreadLocal< ArrayImgRangeCopier > copier;

	public ArrayImgBlocks( final ArrayImg< T, ? > arrayImg, final Extension extension )
	{
		this( arrayImg, extension, null );
	}

	// TODO: CONSTANT extension method should have value parameter. Would be good use-case for sealed classes instead of enum.
	public ArrayImgBlocks( ArrayImg< T, ? > arrayImg, final Extension extension, final T oobValue )
	{
		type = arrayImg.createLinkedType();
		final MemCopy memCopy;
		final Object oob;
		if ( type instanceof UnsignedByteType )
		{
			memCopy = MemCopy.BYTE;
			final byte v = oobValue == null ? 0 : ( ( UnsignedByteType ) oobValue ).getByte();
			oob = new byte[] { v };
		}
		else if ( type instanceof UnsignedShortType )
		{
			memCopy = MemCopy.SHORT;
			final short v = oobValue == null ? 0 : ( ( UnsignedShortType ) oobValue ).getShort();
			oob = new short[] { v };
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
		copier = ThreadLocal.withInitial( () -> new ArrayImgRangeCopier( arrayImg, findRanges, memCopy, oob ) );
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
		copier.get().copy( srcPos, dest, size );
	}

	@Override
	public PrimitiveBlocks< T > threadSafe()
	{
		return this;
	}
}

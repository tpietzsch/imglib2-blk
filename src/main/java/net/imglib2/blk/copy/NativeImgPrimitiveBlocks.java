package net.imglib2.blk.copy;

import net.imglib2.img.NativeImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;

import static net.imglib2.blk.copy.PrimitiveBlocksUtils.extractOobValue;

// TODO: Should NativeImgPrimitiveBlocks be public?? Maybe just detect special case in PrimitiveBlocks and make this package-private
public class NativeImgPrimitiveBlocks< T extends NativeType< T > > implements PrimitiveBlocks< T >
{
	public NativeImgPrimitiveBlocks( final ArrayImg< T, ? > img, final Extension extension )
	{
		this( img, extension, ( findRanges, memCopy, oob ) -> new ArrayImgRangeCopier( img, findRanges, memCopy, oob ) );
	}

	public NativeImgPrimitiveBlocks( final PlanarImg< T, ? > img, final Extension extension )
	{
		this( img, extension, ( findRanges, memCopy, oob ) -> new PlanarImgRangeCopier( img, findRanges, memCopy, oob ) );
	}

	public NativeImgPrimitiveBlocks( final AbstractCellImg< T, ?, ?, ? > img, final Extension extension )
	{
		this( img, extension, ( findRanges, memCopy, oob ) -> new CellImgRangeCopier( img, findRanges, memCopy, oob ) );
	}

	static < T extends NativeType< T > > NativeImgPrimitiveBlocks< T > of( final NativeImg< T, ? > img, final Extension extension )
	{
		if ( img instanceof ArrayImg )
		{
			return new NativeImgPrimitiveBlocks<>( img, extension,
					( findRanges, memCopy, oob ) -> new ArrayImgRangeCopier( ( ArrayImg ) img, findRanges, memCopy, oob ) );
		}
		else if ( img instanceof PlanarImg )
		{
			return new NativeImgPrimitiveBlocks<>( img, extension,
					( findRanges, memCopy, oob ) -> new PlanarImgRangeCopier( ( PlanarImg ) img, findRanges, memCopy, oob ) );
		}
		else if ( img instanceof AbstractCellImg )
		{
			return new NativeImgPrimitiveBlocks<>( img, extension,
					( findRanges, memCopy, oob ) -> new CellImgRangeCopier( ( AbstractCellImg ) img, findRanges, memCopy, oob ) );
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}


	private interface RangeCopierFactory
	{
		RangeCopier create( Ranges findRanges, MemCopy memCopy, Object oob );
	}

	private final T type;

	private final RangeCopier copier;

	private < P > NativeImgPrimitiveBlocks( final NativeImg< T, ? > img, final Extension extension, final RangeCopierFactory copierFactory )
	{
		type = img.createLinkedType();
		if ( type.getEntitiesPerPixel().getRatio() != 1 )
			throw new IllegalArgumentException( "Types with entitiesPerPixel != 1 are not supported" );
		final MemCopy memCopy = MemCopy.forPrimitiveType( type.getNativeTypeFactory().getPrimitiveType() );
		final Object oob = extractOobValue( type, extension );
		final Ranges findRanges = Ranges.forExtension( extension );
		copier = copierFactory.create( findRanges, memCopy, oob );
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

	NativeImgPrimitiveBlocks< T > newInstance()
	{
		return new NativeImgPrimitiveBlocks<>( this );
	}

	private NativeImgPrimitiveBlocks( final NativeImgPrimitiveBlocks< T > blocks )
	{
		type = blocks.type;
		copier = blocks.copier.newInstance();
	}
}

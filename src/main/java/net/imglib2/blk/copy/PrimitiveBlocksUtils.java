package net.imglib2.blk.copy;

import java.util.function.Supplier;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;

class PrimitiveBlocksUtils
{
	static < T extends NativeType< T > > PrimitiveBlocks< T > threadSafe( final Supplier< PrimitiveBlocks< T > > supplier )
	{
		final ThreadLocal< PrimitiveBlocks< T > > tl = ThreadLocal.withInitial( supplier );
		return new PrimitiveBlocks< T >()
		{
			@Override
			public T getType()
			{
				return tl.get().getType();
			}

			@Override
			public void copy( final int[] srcPos, final Object dest, final int[] size )
			{
				tl.get().copy( srcPos, dest, size );
			}

			@Override
			public PrimitiveBlocks< T > threadSafe()
			{
				return this;
			}
		};
	}

	static < T extends NativeType< T > > Object extractOobValue( final T type, final Extension extension )
	{
		if ( extension.type() == Extension.Type.CONSTANT )
		{
			final T oobValue = ( ( ExtensionImpl.ConstantExtension< T > ) extension ).getValue();
			final ArrayImg< T, ? > img = new ArrayImgFactory<>( type ).create( 1 );
			img.firstElement().set( oobValue );
			return ( ( ArrayDataAccess< ? > ) ( img.update( null ) ) ).getCurrentStorageArray();
		}
		else
			return null;
	}
}

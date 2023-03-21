package net.imglib2.blk.copy;

import java.util.function.Supplier;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.img.AbstractNativeImg;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;

class Convert< A extends NativeType< A >, B extends NativeType< B > >
{
	private final Supplier< Converter< A, B > > converterSupplier;

	private final Converter< A, B > converter;

	private final Wrapper< A > srcWrapper;

	private final Wrapper< B > destWrapper;

	public Convert( final A srcType, final B destType, final Supplier< Converter< A, B > > converterSupplier )
	{
		this.converterSupplier = converterSupplier;
		converter = ( Converter< A, B > ) converterSupplier.get();
		srcWrapper = Wrapper.forType( srcType );
		destWrapper = Wrapper.forType( destType );
	}

	public void convert( Object src, Object dest, final int length )
	{
		A srcT = srcWrapper.wrap( src );
		B destT = destWrapper.wrap( dest );
		for ( int i = 0; i < length; i++ )
		{
			srcT.index().set( i );
			destT.index().set( i );
			converter.convert( srcT, destT );
		}
	}

	// creates an independent copy of {@code convert}
	private Convert( Convert convert )
	{
		converterSupplier = convert.converterSupplier;
		converter = ( Converter< A, B > ) converterSupplier.get();
		srcWrapper = convert.srcWrapper;
		destWrapper = convert.destWrapper;
	}

	Convert newInstance()
	{
		return new Convert( this );
	}

	interface Wrapper< T extends NativeType< T > >
	{
		T wrap( Object array );

		static < T extends NativeType< T > > Wrapper< T > forType( T type )
		{
			return FakeImg.createWrapper( type );
		}
	}

	private static class FakeImg<T extends NativeType< T >, A extends ArrayDataAccess< A > > extends AbstractNativeImg< T, A >
	{
		private final A access;

		public FakeImg( final A access )
		{
			super( new long[] { 1 }, new Fraction() );
			this.access = access;
		}

		@Override
		public A update( final Object updater )
		{
			return access;
		}

		static < T extends NativeType< T >, A extends ArrayDataAccess< A > > Wrapper< T > createWrapper( final T type )
		{
			final NativeTypeFactory< T, A > nativeTypeFactory = ( NativeTypeFactory< T, A > ) type.getNativeTypeFactory();
			final PrimitiveTypeProperties< ?, ? > props = PrimitiveTypeProperties.get( nativeTypeFactory.getPrimitiveType() );
			final Wrapper< T > wrap = array -> {
				final FakeImg< T, A > ts = new FakeImg<>( ( A ) props.wrap( array ) );
				final T t = nativeTypeFactory.createLinkedType( ts );
				t.updateContainer( null );
				return t;
			};
			return wrap;
		}

		@Override
		public Cursor< T > cursor()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Cursor< T > localizingCursor()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Object iterationOrder()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public RandomAccess< T > randomAccess()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public ImgFactory< T > factory()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Img< T > copy()
		{
			throw new UnsupportedOperationException();
		}
	}

}

package net.imglib2.blk.copy;

import java.util.function.Supplier;
import net.imglib2.converter.Converter;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

class ConvertImpl
{
	static class Convert_UnsignedShortType_FloatType implements Convert
	{
		private final Supplier< Converter< UnsignedShortType, FloatType > > converterSupplier;

		private final Converter< UnsignedShortType, FloatType > converter;

		public Convert_UnsignedShortType_FloatType(final Supplier< Converter< UnsignedShortType, FloatType > > converterSupplier)
		{
			this.converterSupplier = converterSupplier;
			converter = converterSupplier.get();
		}

		@Override
		public void convert( final Object src, final Object dest, final int length )
		{
			final UnsignedShortType srcT = new UnsignedShortType( new ShortArray( ( short[] ) src ) );
			final FloatType destT = new FloatType( new FloatArray( ( float[] ) dest ) );
			for ( int i = 0; i < length; i++ )
			{
				srcT.index().set( i );
				destT.index().set( i );
				converter.convert( srcT, destT );
			}
		}

		// creates an independent copy of {@code convert}
		private Convert_UnsignedShortType_FloatType( Convert_UnsignedShortType_FloatType convert )
		{
			converterSupplier = convert.converterSupplier;
			converter = converterSupplier.get();
		}

		@Override
		public Convert newInstance()
		{
			return new Convert_UnsignedShortType_FloatType( this );
		}
	}

	static class Convert_UnsignedByteType_FloatType implements Convert
	{
		private final Supplier< Converter< UnsignedByteType, FloatType > > converterSupplier;

		private final Converter< UnsignedByteType, FloatType > converter;

		public Convert_UnsignedByteType_FloatType(final Supplier< Converter< UnsignedByteType, FloatType > > converterSupplier)
		{
			this.converterSupplier = converterSupplier;
			converter = converterSupplier.get();
		}

		@Override
		public void convert( final Object src, final Object dest, final int length )
		{
			final UnsignedByteType srcT = new UnsignedByteType( new ByteArray( ( byte[] ) src ) );
			final FloatType destT = new FloatType( new FloatArray( ( float[] ) dest ) );
			for ( int i = 0; i < length; i++ )
			{
				srcT.index().set( i );
				destT.index().set( i );
				converter.convert( srcT, destT );
			}
		}

		// creates an independent copy of {@code convert}
		private Convert_UnsignedByteType_FloatType( Convert_UnsignedByteType_FloatType convert )
		{
			converterSupplier = convert.converterSupplier;
			converter = converterSupplier.get();
		}

		@Override
		public Convert newInstance()
		{
			return new Convert_UnsignedByteType_FloatType( this );
		}
	}
}

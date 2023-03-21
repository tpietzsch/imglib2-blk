package net.imglib2.blk.copy;

import java.util.function.Supplier;
import net.imglib2.converter.Converter;
import net.imglib2.type.NativeType;

interface Convert
{
	void convert( Object src, Object dest, final int length );

	Convert newInstance();

	static < A extends NativeType< A >, B extends NativeType< B > > Convert create(
			final A srcType,
			final B destType,
			final Supplier< Converter< A, B > > converterSupplier )
	{
		return new ConvertGeneric<>( srcType, destType, converterSupplier );
	}
}

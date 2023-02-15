package net.imglib2.blk.copy;

import java.util.EnumMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.PrimitiveType;

import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;
import static net.imglib2.type.PrimitiveType.INT;
import static net.imglib2.type.PrimitiveType.LONG;
import static net.imglib2.type.PrimitiveType.SHORT;

public class PrimitiveTypeProperties< P, A extends ArrayDataAccess< A > >
{
	final Class< P > primitiveArrayClass;

	final IntFunction< P > createPrimitiveArray;

	final Function< P, A > wrapAsAccess;

	static PrimitiveTypeProperties< ?, ? > get( final PrimitiveType primitiveType )
	{
		return creators.get( primitiveType );
	}

	A wrap( Object data )
	{
		if ( data == null )
			throw new NullPointerException();
		if ( !primitiveArrayClass.isInstance( data ) )
			throw new IllegalArgumentException( "expected " + primitiveArrayClass.getSimpleName() + " argument" );
		return wrapAsAccess.apply( ( P ) data );
	}

	private PrimitiveTypeProperties( final Class< P > primitiveArrayClass, final IntFunction< P > createPrimitiveArray, final Function< P, A > wrapAsAccess )
	{
		this.primitiveArrayClass = primitiveArrayClass;
		this.createPrimitiveArray = createPrimitiveArray;
		this.wrapAsAccess = wrapAsAccess;
	}

	private static final EnumMap< PrimitiveType, PrimitiveTypeProperties< ?, ? > > creators = new EnumMap<>( PrimitiveType.class );

	static
	{
		creators.put( BYTE, new PrimitiveTypeProperties<>( byte[].class, byte[]::new, ByteArray::new ) );
		creators.put( SHORT, new PrimitiveTypeProperties<>( short[].class, short[]::new, ShortArray::new ) );
		creators.put( INT, new PrimitiveTypeProperties<>( int[].class, int[]::new, IntArray::new ) );
		creators.put( LONG, new PrimitiveTypeProperties<>( long[].class, long[]::new, LongArray::new ) );
		creators.put( FLOAT, new PrimitiveTypeProperties<>( float[].class, float[]::new, FloatArray::new ) );
		creators.put( DOUBLE, new PrimitiveTypeProperties<>( double[].class, double[]::new, DoubleArray::new ) );
	}
}

package net.imglib2.blk.downsample;

import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.Interval;
import net.imglib2.blk.downsample.algo.BlockProcessor;
import net.imglib2.blocks.TempArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;

import static net.imglib2.blk.downsample.TypeConversionGenerated.from_i32;
import static net.imglib2.blk.downsample.TypeConversionGenerated.from_u8;
import static net.imglib2.blk.downsample.TypeConversionGenerated.to_f32;
import static net.imglib2.blk.downsample.TypeConversionGenerated.to_u8;

public class GenericTypeConversionPlayground
{
	// instances of convert loops
	static void convert_u8_to_f32( final byte[] src, final float[] dest, final int length )
	{
		System.out.println( "GenericTypeConversionPlayground.convert_u8_to_f32" );
		for ( int i = 0; i < length; ++i )
			dest[ i ] = to_f32( from_u8( src[ i ] ) );
	}

	// instances of convert loops
	static void convert_i32_to_u8( final int[] src, final byte[] dest, final int length )
	{
		System.out.println( "GenericTypeConversionPlayground.convert_i32_to_u8" );
		for ( int i = 0; i < length; ++i )
			dest[ i ] = to_u8( from_i32( src[ i ] ) );
	}

	// incomplete dummy
	enum UnaryOperatorType
	{
		U8_TO_F32, I32_TO_U8;

		static UnaryOperatorType of( NativeType< ? > source, NativeType<?> target )
		{
			return of( OperandType.of( source ), OperandType.of( target ) );
		}

		static UnaryOperatorType of(OperandType source, OperandType target)
		{
			switch ( source )
			{
			case U8:
				switch ( target )
				{
				case F32:
					return U8_TO_F32;
				default:
					throw new UnsupportedOperationException( "TODO" );
				}
			case I32:
				switch ( target )
				{
				case U8:
					return I32_TO_U8;
				default:
					throw new UnsupportedOperationException( "TODO" );
				}
			default:
				throw new UnsupportedOperationException( "TODO" );
			}
		}
	}

	enum OperandType
	{
		U8,
		F32,
		I32;

		static OperandType of( NativeType< ? > type )
		{
			if ( type instanceof UnsignedByteType )
				return U8;
			else if ( type instanceof FloatType )
				return F32;
			else if ( type instanceof IntType )
				return I32;
			else
				throw new IllegalArgumentException( "Unsupported Type: " + type.getClass().getSimpleName() );
		}
	}


	static class Convert_u8_to_f32 implements ConvertLoop< byte[], float[] >
	{
		static final Convert_u8_to_f32 INSTANCE = new Convert_u8_to_f32();

		@Override
		public void apply( final byte[] src, final float[] dest, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ i ] = to_f32( from_u8( src[ i ] ) );
		}
	}

	static class Convert_i32_to_u8 implements ConvertLoop< int[], byte[] >
	{
		static final Convert_i32_to_u8 INSTANCE = new Convert_i32_to_u8();

		@Override
		public void apply( final int[] src, final byte[] dest, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ i ] = to_u8( from_i32( src[ i ] ) );
		}
	}

	@FunctionalInterface
	interface ConvertLoop< I, O >
	{
		void apply( final I src, final O dest, final int length );

		static < I, O > ConvertLoop< I, O > get( UnaryOperatorType type )
		{
			switch( type )
			{
			case U8_TO_F32:
				return Cast.unchecked( Convert_u8_to_f32.INSTANCE );
//				return Cast.unchecked( ( ConvertLoop< byte[], float[] > ) GenericTypeConversionPlayground::convert_u8_to_f32 );
			case I32_TO_U8:
				return Cast.unchecked( Convert_i32_to_u8.INSTANCE );
			default:
				throw new UnsupportedOperationException( "TODO" );
			}
		}
	}

	/**
	 * @param <I> input primitive array type, e.g., float[]
	 * @param <O> output primitive array type, e.g., float[]
	 */
	static class TypeConvert< S extends NativeType< S >, T extends NativeType< T >, I, O > implements BlockProcessor< I, O >
	{
		private final S sourceType;
		private final T targetType;

		private final TempArray< I > tempArray;
		private final ConvertLoop< I, O > loop;

		private Supplier< TypeConvert< S, T, I, O > > threadSafeSupplier;

		private long[] sourcePos;
		private int[] sourceSize;
		private int sourceLength;

		public TypeConvert( final S sourceType, final T targetType )
		{
			this.sourceType = sourceType;
			this.targetType = targetType;
			tempArray = TempArray.forPrimitiveType( sourceType.getNativeTypeFactory().getPrimitiveType() );
			loop = ConvertLoop.get( UnaryOperatorType.of( sourceType, targetType ) );
		}

		private TypeConvert( TypeConvert< S, T, I, O > convert )
		{
			sourceType = convert.sourceType;
			targetType = convert.targetType;
			tempArray = convert.tempArray.newInstance();
			loop = convert.loop;
			threadSafeSupplier = convert.threadSafeSupplier;
		}

		private TypeConvert< S, T, I, O > newInstance()
		{
			return new TypeConvert<>( this );
		}

		@Override
		public Supplier< ? extends BlockProcessor< I, O > > threadSafeSupplier()
		{
			if ( threadSafeSupplier == null )
				threadSafeSupplier = ThreadLocal.withInitial( this::newInstance )::get;
			return threadSafeSupplier;
		}

		@Override
		public void setTargetInterval( final Interval interval )
		{
			final int n = interval.numDimensions();
			if ( sourcePos == null || sourcePos.length != n )
			{
				sourcePos = new long[ n ];
				sourceSize = new int[ n ];
			}
			interval.min( sourcePos );
			Arrays.setAll( sourceSize, d -> safeInt( interval.dimension( d ) ) );
			sourceLength = safeInt( Intervals.numElements( sourceSize ) );
		}

		@Override
		public long[] getSourcePos()
		{
			return sourcePos;
		}

		@Override
		public int[] getSourceSize()
		{
			return sourceSize;
		}

		@Override
		public I getSourceBuffer()
		{
			return tempArray.get( sourceLength );
		}

		@Override
		public void compute( final I src, final O dest )
		{
			loop.apply( src, dest, sourceLength );
		}
	}

	static int safeInt( final long value )
	{
		if ( value > Integer.MAX_VALUE )
			throw new IllegalArgumentException( "value too large" );
		return ( int ) value;
	}



	public static void main( String[] args )
	{

	}
}

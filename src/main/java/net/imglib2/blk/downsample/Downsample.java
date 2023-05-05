package net.imglib2.blk.downsample;

import java.util.Arrays;

import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;

public class Downsample
{
	public static long[] getDownsampledDimensions( final long[] imgDimensions, final boolean[] downsampleInDim )
	{
		final int n = imgDimensions.length;
		if ( downsampleInDim.length != n )
			throw new IllegalArgumentException();
		final long[] destSize = new long[ n ];
		Arrays.setAll( destSize, d -> downsampleInDim[ d ] ? ( imgDimensions[ d ] + 1 ) / 2 : imgDimensions[ d ] );
		return destSize;
	}

	public static class Float extends AbstractDownsample< Float, float[] >
	{
		public Float( final boolean[] downsampleInDim )
		{
			super( downsampleInDim, FLOAT );
		}

		private Float( Float downsample )
		{
			super( downsample );
		}

		@Override
		Float newInstance()
		{
			return new Float( this );
		}

		@Override
		void downsample( final float[] source, final int[] destSize, final float[] dest, final int dim )
		{
			if ( dim == 0 )
				downsampleX( source, destSize, dest );
			else
				downsampleN( source, destSize, dest, dim );
		}

		private static void downsampleX( final float[] source, final int[] destSize, final float[] dest )
		{
			final int len1 = destSize[ 0 ];
			final int len2 = mulDims( destSize, 1, destSize.length );
			for ( int z = 0; z < len2; ++z )
			{
				final int destOffsetZ = z * len1;
				final int srcOffsetZ = z * ( 2 * len1 + 1 );
				line_X( source, dest, destOffsetZ, srcOffsetZ, len1 );
			}
		}

		private static void downsampleN( final float[] source, final int[] destSize, final float[] dest, final int dim )
		{
			final int len0 = mulDims( destSize, 0, dim );
			final int len1 = destSize[ dim ];
			final int len2 = mulDims( destSize, dim + 1, destSize.length );
			for ( int z = 0; z < len2; ++z )
			{
				final int destOffsetZ = z * len1 * len0;
				final int srcOffsetZ = z * ( 2 * len1 + 1 ) * len0;
				for ( int y = 0; y < len1; ++y )
				{
					final int destOffset = destOffsetZ + y * len0;
					final int srcOffset = srcOffsetZ + 2 * y * len0;
					line_N( source, dest, destOffset, srcOffset, len0 );
				}
			}
		}

		private static int mulDims( int[] dims, int from, int to )
		{
			int product = 1;
			for ( int d = from; d < to; ++d )
				product *= dims[ d ];
			return product;
		}

		private static void line_X( final float[] source, final float[] dest,
				final int destOffset,
				final int srcOffset,
				final int lineLength )
		{
			for ( int x = 0; x < lineLength; ++x )
			{
				dest[ destOffset + x ] = wavg_float(
						source[ srcOffset + 2 * x ],
						source[ srcOffset + 2 * x + 1 ],
						source[ srcOffset + 2 * x + 2 ] );
			}
		}

		private static void line_N( final float[] source, final float[] dest,
				final int destOffset,
				final int srcOffset,
				final int lineLength )
		{
			for ( int x = 0; x < lineLength; ++x )
			{
				dest[ destOffset + x ] = wavg_float(
						source[ srcOffset + x ],
						source[ srcOffset + x + lineLength ],
						source[ srcOffset + x + 2 * lineLength ] );
			}
		}
	}

	public static class Double extends AbstractDownsample< Double, double[] >
	{
		public Double( final boolean[] downsampleInDim )
		{
			super( downsampleInDim, DOUBLE );
		}

		private Double( Double downsample )
		{
			super( downsample );
		}

		@Override
		Double newInstance()
		{
			return new Double( this );
		}

		@Override
		void downsample( final double[] source, final int[] destSize, final double[] dest, final int dim )
		{
			downsample_double_double( source, destSize, dest, dim );
		}
	}


	// -- downsample --
	// TODO: auto-generate

	private static void downsample_double_double( final double[] source, final int[] destSize, final double[] dest, final int dim )
	{
		if ( dim == 0 )
			downsampleX_double_double( source, destSize, dest );
		else
			downsampleN_double_double( source, destSize, dest, dim );
	}

	private static void downsampleX_double_double( final double[] source, final int[] destSize, final double[] dest )
	{
		final int len1 = destSize[ 0 ];
		final int len2 = mulDims( destSize, 1, destSize.length );
		for ( int z = 0; z < len2; ++z )
		{
			final int destOffsetZ = z * len1;
			final int srcOffsetZ = z * ( 2 * len1 + 1 );
			for ( int x = 0; x < len1; ++x )
			{
				dest[ destOffsetZ + x ] = wavg_double(
						source[ srcOffsetZ + 2 * x ],
						source[ srcOffsetZ + 2 * x + 1 ],
						source[ srcOffsetZ + 2 * x + 2 ] );
			}
		}
	}

	private static void downsampleN_double_double( final double[] source, final int[] destSize, final double[] dest, final int dim )
	{
		final int len0 = mulDims( destSize, 0, dim );
		final int len1 = destSize[ dim ];
		final int len2 = mulDims( destSize, dim + 1, destSize.length );
		for ( int z = 0; z < len2; ++z )
		{
			final int destOffsetZ = z * len1 * len0;
			final int srcOffsetZ = z * ( 2 * len1 + 1 ) * len0;
			for ( int y = 0; y < len1; ++y )
			{
				final int destOffset = destOffsetZ + y * len0;
				final int srcOffset = srcOffsetZ + 2 * y * len0;
				for ( int x = 0; x < len0; ++x )
				{
					dest[ destOffset + x ] = wavg_double(
							source[ srcOffset + x ],
							source[ srcOffset + x + len0 ],
							source[ srcOffset + x + 2 * len0 ] );
				}
			}
		}
	}

	// -- helpers --

	private static int mulDims( int[] dims, int from, int to )
	{
		int product = 1;
		for ( int d = from; d < to; ++d )
			product *= dims[ d ];
		return product;
	}

	// -- averaging --
	// TODO: could be auto-generated, but maybe easier to hand-code?

	private static float wavg_float( final float a, final float b, final float c )
	{
		return 0.25f * ( a + 2 * b + c );
	}

	private static float wavg_u8_float( final byte a, final byte b, final byte c )
	{
		return 0.25f * ( u8_to_int( a ) + 2 * u8_to_int( b ) + u8_to_int( c ) );
	}

	private static float wavg_u16_float( final short a, final short b, final short c )
	{
		return 0.25f * ( u16_to_int( a ) + 2 * u16_to_int( b ) + u16_to_int( c ) );
	}

	private static double wavg_double( final double a, final double b, final double c )
	{
		return 0.25 * ( a + 2 * b + c );
	}

	private static double wavg_u8_double( final byte a, final byte b, final byte c )
	{
		return 0.25 * ( u8_to_int( a ) + 2 * u8_to_int( b ) + u8_to_int( c ) );
	}

	private static double wavg_u16_double( final short a, final short b, final short c )
	{
		return 0.25 * ( u16_to_int( a ) + 2 * u16_to_int( b ) + u16_to_int( c ) );
	}

	// -- type conversion --



	// (1) if input type is unsigned:
	// convert unsigned inputs to signed int/long, using these:
	private static int from_u8( final byte u8 ) {return u8 & 0xff;}
	private static int from_u16( final short u16 ) {return u16 & 0xffff;}
	private static long from_u32( final int u32 ) {return u32 & 0xffffffffL;}

	// (2) if input type is x32: add a cast to (long) to avoid overflow of the addition

	// (3) if output type is f32: use "*0.25f"
	//     if output type is f64: use "*0.25"

	//     if output type is integral, use rounding


	private static int u8_to_int( final byte u8 )
	{
		return u8 & 0xff;
	}

	private static int u16_to_int( final short u16 )
	{
		return u16 & 0xffff;
	}

	private static byte i32_to_u8_clamp( final int i32 )
	{
		return ( byte ) ( Math.min( Math.max( i32, 0 ), 0xff ) & 0xff );
	}

	private static byte u32_to_u8_clamp( final int u32 )
	{
		if ( ( u32 & 0xffffff ) != 0 )
			return ( byte ) 0xff;
		else
			return ( byte ) ( u32 & 0xff );
	}

	private static short i32_to_u16_clamp( final int i32 )
	{
		return ( byte ) ( Math.min( Math.max( i32, 0 ), 0xffff ) & 0xffff );
	}
}

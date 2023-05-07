package net.imglib2.blk.downsample;

import static net.imglib2.blk.downsample.ConversionGenerated.to_u8;
import static net.imglib2.blk.downsample.ConversionGenerated.u8_to_int;
import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.FLOAT;

public class DownsampleMultiple
{

	public static class UnsignedByteViaFloat extends AbstractDownsampleMultiple< DownsampleMultiple.UnsignedByteViaFloat, byte[], float[] >
	{
		public UnsignedByteViaFloat( final boolean[] downsampleInDim )
		{
			super( downsampleInDim, BYTE, FLOAT );
		}

		private UnsignedByteViaFloat( UnsignedByteViaFloat downsample )
		{
			super( downsample );
		}

		@Override
		UnsignedByteViaFloat newInstance()
		{
			return new UnsignedByteViaFloat( this );
		}

		@Override
		void downsamplePP( final byte[] source, final int[] destSize, final byte[] dest, final int dim )
		{
			if( dim == 0 )
				u8_u8_X( source, destSize, dest );
			else
				u8_u8_N( source, destSize, dest, dim );
		}

		@Override
		void downsamplePQ( final byte[] source, final int[] destSize, final float[] dest, final int dim )
		{
			if( dim == 0 )
				u8_float_X( source, destSize, dest );
			else
				u8_float_N( source, destSize, dest, dim );
		}

		@Override
		void downsampleQQ( final float[] source, final int[] destSize, final float[] dest, final int dim )
		{
			if( dim == 0 )
				float_float_X( source, destSize, dest );
			else
				float_float_N( source, destSize, dest, dim );
		}

		@Override
		void downsampleQP( final float[] source, final int[] destSize, final byte[] dest, final int dim )
		{
			if( dim == 0 )
				float_u8_X( source, destSize, dest );
			else
				float_u8_N( source, destSize, dest, dim );
		}
	}


	private static void u8_float_X( final byte[] source, final int[] destSize, final float[] dest )
	{
		final int len1 = destSize[ 0 ];
		final int len2 = mulDims( destSize, 1, destSize.length );
		for ( int z = 0; z < len2; ++z )
		{
			final int destOffsetZ = z * len1;
			final int srcOffsetZ = z * ( 2 * len1 + 1 );
			for ( int x = 0; x < len1; ++x )
			{
				dest[ destOffsetZ + x ] = wavg_u8_float(
						source[ srcOffsetZ + 2 * x ],
						source[ srcOffsetZ + 2 * x + 1 ],
						source[ srcOffsetZ + 2 * x + 2 ] );
			}
		}
	}

	private static void u8_u8_X( final byte[] source, final int[] destSize, final byte[] dest )
	{
		final int len1 = destSize[ 0 ];
		final int len2 = mulDims( destSize, 1, destSize.length );
		for ( int z = 0; z < len2; ++z )
		{
			final int destOffsetZ = z * len1;
			final int srcOffsetZ = z * ( 2 * len1 + 1 );
			for ( int x = 0; x < len1; ++x )
			{
				dest[ destOffsetZ + x ] = wavg_u8_u8(
						source[ srcOffsetZ + 2 * x ],
						source[ srcOffsetZ + 2 * x + 1 ],
						source[ srcOffsetZ + 2 * x + 2 ] );
			}
		}
	}

	private static void float_u8_X( final float[] source, final int[] destSize, final byte[] dest )
	{
		final int len1 = destSize[ 0 ];
		final int len2 = mulDims( destSize, 1, destSize.length );
		for ( int z = 0; z < len2; ++z )
		{
			final int destOffsetZ = z * len1;
			final int srcOffsetZ = z * ( 2 * len1 + 1 );
			for ( int x = 0; x < len1; ++x )
			{
				dest[ destOffsetZ + x ] = wavg_float_u8(
						source[ srcOffsetZ + 2 * x ],
						source[ srcOffsetZ + 2 * x + 1 ],
						source[ srcOffsetZ + 2 * x + 2 ] );
			}
		}
	}

	private static void float_float_X( final float[] source, final int[] destSize, final float[] dest )
	{
		final int len1 = destSize[ 0 ];
		final int len2 = mulDims( destSize, 1, destSize.length );
		for ( int z = 0; z < len2; ++z )
		{
			final int destOffsetZ = z * len1;
			final int srcOffsetZ = z * ( 2 * len1 + 1 );
			for ( int x = 0; x < len1; ++x )
			{
				dest[ destOffsetZ + x ] = wavg_float_float(
						source[ srcOffsetZ + 2 * x ],
						source[ srcOffsetZ + 2 * x + 1 ],
						source[ srcOffsetZ + 2 * x + 2 ] );
			}
		}
	}


	private static void u8_float_N( final byte[] source, final int[] destSize, final float[] dest, final int dim )
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
					dest[ destOffset + x ] = wavg_u8_float(
							source[ srcOffset + x ],
							source[ srcOffset + x + len0 ],
							source[ srcOffset + x + 2 * len0 ] );
				}
			}
		}
	}

	private static void u8_u8_N( final byte[] source, final int[] destSize, final byte[] dest, final int dim )
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
					dest[ destOffset + x ] = wavg_u8_u8(
							source[ srcOffset + x ],
							source[ srcOffset + x + len0 ],
							source[ srcOffset + x + 2 * len0 ] );
				}
			}
		}
	}

	private static void float_u8_N( final float[] source, final int[] destSize, final byte[] dest, final int dim )
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
					dest[ destOffset + x ] = wavg_float_u8(
							source[ srcOffset + x ],
							source[ srcOffset + x + len0 ],
							source[ srcOffset + x + 2 * len0 ] );
				}
			}
		}
	}

	private static void float_float_N( final float[] source, final int[] destSize, final float[] dest, final int dim )
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
					dest[ destOffset + x ] = wavg_float_float(
							source[ srcOffset + x ],
							source[ srcOffset + x + len0 ],
							source[ srcOffset + x + 2 * len0 ] );
				}
			}
		}
	}


	private static float wavg_u8_float( final byte a, final byte b, final byte c )
	{
		return 0.25f * ( u8_to_int( a ) + 2 * u8_to_int( b ) + u8_to_int( c ) );
	}

	private static byte wavg_u8_u8( final byte a, final byte b, final byte c )
	{
		return to_u8( 0.25f * ( u8_to_int( a ) + 2 * u8_to_int( b ) + u8_to_int( c ) ) );
	}

	private static byte wavg_float_u8( final float a, final float b, final float c )
	{
		return to_u8( 0.25f * ( a + 2 * b + c ) );
	}

	private static float wavg_float_float( final float a, final float b, final float c )
	{
		return 0.25f * ( a + 2 * b + c );
	}


	private static int mulDims( int[] dims, int from, int to )
	{
		int product = 1;
		for ( int d = from; d < to; ++d )
			product *= dims[ d ];
		return product;
	}
}

package net.imglib2.blk.downsample;

public class Routines
{


	private static void u8_float_X( final byte[] source, final int[] destSize, final float[] dest )
	{
		final int destLineLength = destSize[ 0 ];
		final int srcLineLength = 2 * destSize[ 0 ] + 1;

		int nLines = 1;
		for ( int d = 1; d < destSize.length; ++d )
			nLines *= destSize[ d ];

		for ( int y = 0; y < nLines; ++y )
		{
			final int destOffset = y * destLineLength;
			final int srcOffset = y * srcLineLength;
			for ( int x = 0; x < destLineLength; ++x )
			{
				final int si = srcOffset + 2 * x;
				dest[ destOffset + x ] = wavg_u8_float( source[ si ], source[ si + 1 ], source[ si + 2 ] );
			}
		}
	}

	private static void u8_float_N( final byte[] source, final int[] destSize, final float[] dest, final int dim )
	{
		int lineLength = 1;
		for ( int d = 0; d < dim; ++d )
			lineLength *= destSize[ d ];

		final int nLines = destSize[ dim ];

		int nPlanes = 1;
		for ( int d = dim + 1; d < destSize.length; ++d )
			nPlanes *= destSize[ d ];

		for ( int z = 0; z < nPlanes; ++z )
		{
			for ( int y = 0; y < nLines; ++y )
			{
				final int destOffset = ( z * nLines * lineLength ) + ( y * lineLength );
				final int srcOffset = ( z * ( 2 * nLines + 1 ) * lineLength ) + ( 2 * y * lineLength );
				for ( int x = 0; x < lineLength; ++x )
				{
					dest[ destOffset + x ] = wavg_u8_float( source[ srcOffset + x ], source[ srcOffset + x + lineLength ], source[ srcOffset + x + 2 * lineLength ] );
				}
			}
		}
	}

	private static float wavg_u8_float( final byte a, final byte b, final byte c )
	{
		return 0.25f * u8_to_int(a) +
				0.5f * u8_to_int(b) +
				0.25f * u8_to_int(c);

	}

	private static int u8_to_int( final byte signedByte )
	{
		return signedByte & 0xff;
	}

	private static void u8_u8_X( final byte[] source, final int[] destSize, final byte[] dest )
	{
		final int destLineLength = destSize[ 0 ];
		final int srcLineLength = 2 * destSize[ 0 ] + 1;

		int nLines = 1;
		for ( int d = 1; d < destSize.length; ++d )
			nLines *= destSize[ d ];

		for ( int y = 0; y < nLines; ++y )
		{
			final int destOffset = y * destLineLength;
			final int srcOffset = y * srcLineLength;
			for ( int x = 0; x < destLineLength; ++x )
			{
				final int si = srcOffset + 2 * x;
				dest[ destOffset + x ] = getCodedSignedByte(
						0.25f * getUnsignedByte( source[ si ] ) +
								0.5f * getUnsignedByte( source[ si + 1 ] ) +
								0.25f * getUnsignedByte( source[ si + 2 ] ) );
			}
		}
	}

	private static void float_u8_X( final float[] source, final int[] destSize, final byte[] dest )
	{
		final int destLineLength = destSize[ 0 ];
		final int srcLineLength = 2 * destSize[ 0 ] + 1;

		int nLines = 1;
		for ( int d = 1; d < destSize.length; ++d )
			nLines *= destSize[ d ];

		for ( int y = 0; y < nLines; ++y )
		{
			final int destOffset = y * destLineLength;
			final int srcOffset = y * srcLineLength;
			for ( int x = 0; x < destLineLength; ++x )
			{
				final int si = srcOffset + 2 * x;
				dest[ destOffset + x ] = getCodedSignedByte(
						0.25f * source[ si ] +
								0.5f * source[ si + 1 ] +
								0.25f * source[ si + 2 ] );
			}
		}
	}

	private static void float_float_X( final float[] source, final int[] destSize, final float[] dest )
	{
		final int destLineLength = destSize[ 0 ];
		final int srcLineLength = 2 * destSize[ 0 ] + 1;

		int nLines = 1;
		for ( int d = 1; d < destSize.length; ++d )
			nLines *= destSize[ d ];

		for ( int y = 0; y < nLines; ++y )
		{
			final int destOffset = y * destLineLength;
			final int srcOffset = y * srcLineLength;
			for ( int x = 0; x < destLineLength; ++x )
			{
				final int si = srcOffset + 2 * x;
				dest[ destOffset + x ] =
						0.25f * source[ si ] +
								0.5f * source[ si + 1 ] +
								0.25f * source[ si + 2 ];
			}
		}
	}


	private static void u8_u8_N( final byte[] source, final int[] destSize, final byte[] dest, final int dim )
	{
		int lineLength = 1;
		for ( int d = 0; d < dim; ++d )
			lineLength *= destSize[ d ];

		final int nLines = destSize[ dim ];

		int nPlanes = 1;
		for ( int d = dim + 1; d < destSize.length; ++d )
			nPlanes *= destSize[ d ];

		for ( int z = 0; z < nPlanes; ++z )
		{
			for ( int y = 0; y < nLines; ++y )
			{
				final int destOffset = ( z * nLines * lineLength ) + ( y * lineLength );
				final int srcOffset = ( z * ( 2 * nLines + 1 ) * lineLength ) + ( 2 * y * lineLength );
				for ( int x = 0; x < lineLength; ++x )
				{
					dest[ destOffset + x ] = getCodedSignedByte(
							0.25f * getUnsignedByte(source[ srcOffset + x ]) +
									0.5f * getUnsignedByte(source[ srcOffset + x + lineLength ]) +
									0.25f * getUnsignedByte(source[ srcOffset + x + 2 * lineLength ]) );
				}
			}
		}
	}

	private static void float_u8_N( final float[] source, final int[] destSize, final byte[] dest, final int dim )
	{
		int lineLength = 1;
		for ( int d = 0; d < dim; ++d )
			lineLength *= destSize[ d ];

		final int nLines = destSize[ dim ];

		int nPlanes = 1;
		for ( int d = dim + 1; d < destSize.length; ++d )
			nPlanes *= destSize[ d ];

		for ( int z = 0; z < nPlanes; ++z )
		{
			for ( int y = 0; y < nLines; ++y )
			{
				final int destOffset = ( z * nLines * lineLength ) + ( y * lineLength );
				final int srcOffset = ( z * ( 2 * nLines + 1 ) * lineLength ) + ( 2 * y * lineLength );
				for ( int x = 0; x < lineLength; ++x )
				{
					dest[ destOffset + x ] = getCodedSignedByte(
							0.25f * source[ srcOffset + x ] +
									0.5f * source[ srcOffset + x + lineLength ] +
									0.25f * source[ srcOffset + x + 2 * lineLength ] );
				}
			}
		}
	}

	private static void float_float_N( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		int lineLength = 1;
		for ( int d = 0; d < dim; ++d )
			lineLength *= destSize[ d ];

		final int nLines = destSize[ dim ];

		int nPlanes = 1;
		for ( int d = dim + 1; d < destSize.length; ++d )
			nPlanes *= destSize[ d ];

		for ( int z = 0; z < nPlanes; ++z )
		{
			for ( int y = 0; y < nLines; ++y )
			{
				final int destOffset = ( z * nLines * lineLength ) + ( y * lineLength );
				final int srcOffset = ( z * ( 2 * nLines + 1 ) * lineLength ) + ( 2 * y * lineLength );
				for ( int x = 0; x < lineLength; ++x )
				{
					dest[ destOffset + x ] = 0.25f * source[ srcOffset + x ] +
							0.5f * source[ srcOffset + x + lineLength ] +
							0.25f * source[ srcOffset + x + 2 * lineLength ];
				}
			}
		}
	}

	private static int getUnsignedByte( final byte signedByte )
	{
		return signedByte & 0xff;
	}

	private static byte getCodedSignedByte( final int unsignedByte )
	{
		return ( byte ) ( unsignedByte & 0xff );
	}

	private static byte getCodedSignedByte( final float unsignedByte )
	{
		return getCodedSignedByte( ( int ) unsignedByte );
	}


	public static void main( String[] args )
	{
		int int_a = 0x10f7;
		long long_a = 0x3451ef0;

		System.out.println( "int_a = " + int_a );
		System.out.println( "to_u8_1(int_a) = " + to_u8_1( int_a ) + " = " + getUnsignedByte( to_u8_1( int_a ) ) );
		System.out.println( "to_u8_2(int_a) = " + to_u8_2( int_a ) + " = " + getUnsignedByte( to_u8_2( int_a ) ) );

		System.out.println( "long_a = " + long_a );
		System.out.println( "to_u8_1(long_a) = " + to_u8_1( long_a ) + " = " + getUnsignedByte( to_u8_1( long_a ) ) );
		System.out.println( "to_u8_2(long_a) = " + to_u8_2( long_a ) + " = " + getUnsignedByte( to_u8_2( long_a ) ) );
	}

	private static byte to_u8_1( int value ) { return ( byte ) ( value & 0xff ); }
	private static byte to_u8_1( long value ) { return ( byte ) ( value & 0xff ); }
	private static byte to_u8_2( int value ) { return ( byte ) value; }
	private static byte to_u8_2( long value ) { return ( byte ) value; }




	private static byte to_i8( int value ) { return ( byte ) value; }
	private static byte to_i8( long value ) { return ( byte ) value; }

	// TODO mask unnecessary
	private static byte to_u8( int value ) { return ( byte ) ( value & 0xff ); }
	private static byte to_u8( long value ) { return ( byte ) ( value & 0xff ); }

	private static short to_i16( int value ) { return ( short ) value; }
	private static short to_i16( long value ) { return ( short ) value; }

	// TODO mask unnecessary
	private static short to_u16( int value ) { return ( short ) ( value & 0xffff ); }
	private static short to_u16( long value ) { return ( short ) ( value & 0xffff ); }

	// TODO cast unnecessary
	private static int to_i32( int value ) { return ( int ) value; }

	private static int to_i32( long value ) { return ( int ) value; }

	// TODO mask unnecessary, cast unnecessary
	private static int to_u32( int value ) { return ( int ) ( value & 0xffffffffL ); }

	// TODO mask unnecessary
	private static int to_u32( long value ) { return ( int ) ( value & 0xffffffffL ); }

	// cast unnecessary
	private static long to_i64( int value ) { return ( long ) value; }
	private static long to_i64( long value ) { return ( long ) value; }

}

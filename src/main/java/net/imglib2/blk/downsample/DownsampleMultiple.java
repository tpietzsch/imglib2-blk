package net.imglib2.blk.downsample;

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
						0.25f * getUnsignedByte(source[ si ]) +
								0.5f * getUnsignedByte(source[ si + 1 ]) +
								0.25f * getUnsignedByte(source[ si + 2 ]);
			}
		}
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
					dest[ destOffset + x ] =
							0.25f * getUnsignedByte(source[ srcOffset + x ]) +
									0.5f * getUnsignedByte(source[ srcOffset + x + lineLength ]) +
									0.25f * getUnsignedByte(source[ srcOffset + x + 2 * lineLength ]);
				}
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

}

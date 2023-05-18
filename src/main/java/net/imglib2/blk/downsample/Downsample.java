package net.imglib2.blk.downsample;

import java.util.Arrays;
import net.imglib2.blk.downsample.algo.BlockProcessor;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;

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

	public static < T extends NativeType< T >, P >
	BlockProcessor< P, P > downsample( final T type, final boolean[] downsampleInDim )
	{
		final PrimitiveType pt = type.getNativeTypeFactory().getPrimitiveType();
		return pt.equals( FLOAT ) || pt.getByteCount() < FLOAT.getByteCount()
				? downsampleFloat( type, downsampleInDim )
				: downsampleDouble( type, downsampleInDim );
	}

	public static < T extends NativeType< T >, P >
	BlockProcessor< P, P > downsampleFloat( final T type, final boolean[] downsampleInDim )
	{
		return processConverted( type, new FloatType(), new DownsampleFloat( downsampleInDim ) );
	}

	public static < T extends NativeType< T >, P >
	BlockProcessor< P, P > downsampleDouble( final T type, final boolean[] downsampleInDim )
	{
		return processConverted( type, new DoubleType(), new DownsampleDouble( downsampleInDim ) );
	}

	public static < T extends NativeType< T >, P >
	BlockProcessor< P, P > downsampleHalfPixel( final T type, final boolean[] downsampleInDim )
	{
		final PrimitiveType pt = type.getNativeTypeFactory().getPrimitiveType();
		return pt.equals( FLOAT ) || pt.getByteCount() < FLOAT.getByteCount()
				? downsampleHalfPixelFloat( type, downsampleInDim )
				: downsampleHalfPixelDouble( type, downsampleInDim );
	}

	public static < T extends NativeType< T >, P >
	BlockProcessor< P, P > downsampleHalfPixelFloat( final T type, final boolean[] downsampleInDim )
	{
		return processConverted( type, new FloatType(), new DownsampleHalfPixelFloat( downsampleInDim ) );
	}

	public static < T extends NativeType< T >, P >
	BlockProcessor< P, P > downsampleHalfPixelDouble( final T type, final boolean[] downsampleInDim )
	{
		return processConverted( type, new DoubleType(), new DownsampleHalfPixelDouble( downsampleInDim ) );
	}

	private static < T extends NativeType< T >, P, S extends NativeType< S >, Q >
	BlockProcessor< P, P > processConverted( final T type, final S processType, BlockProcessor< Q, Q > processor )
	{
		if ( processType.getClass().isInstance( type ) )
		{
			return Cast.unchecked( processor );
		}
		else
		{
			return new TypeConvert< T, S, P, Q >( type, processType )
					.andThen( processor )
					.andThen( new TypeConvert< S, T, Q, P >( processType, type ) );
		}
	}

	// -- implementation --

	static class DownsampleFloat extends AbstractDownsample< DownsampleFloat, float[] >
	{
		public DownsampleFloat( final boolean[] downsampleInDim )
		{
			super( downsampleInDim, FLOAT );
		}

		private DownsampleFloat( DownsampleFloat downsample )
		{
			super( downsample );
		}

		@Override
		DownsampleFloat newInstance()
		{
			return new DownsampleFloat( this );
		}

		@Override
		void downsample( final float[] source, final int[] destSize, final float[] dest, final int dim )
		{
			downsample_float( source, destSize, dest, dim );
		}
	}

	static class DownsampleDouble extends AbstractDownsample< DownsampleDouble, double[] >
	{
		public DownsampleDouble( final boolean[] downsampleInDim )
		{
			super( downsampleInDim, DOUBLE );
		}

		private DownsampleDouble( DownsampleDouble downsample )
		{
			super( downsample );
		}

		@Override
		DownsampleDouble newInstance()
		{
			return new DownsampleDouble( this );
		}

		@Override
		void downsample( final double[] source, final int[] destSize, final double[] dest, final int dim )
		{
			downsample_double( source, destSize, dest, dim );
		}
	}

	static class DownsampleHalfPixelFloat extends AbstractDownsampleHalfPixel< DownsampleHalfPixelFloat, float[] >
	{
		public DownsampleHalfPixelFloat( final boolean[] downsampleInDim )
		{
			super( downsampleInDim, FLOAT );
		}

		private DownsampleHalfPixelFloat( DownsampleHalfPixelFloat downsample )
		{
			super( downsample );
		}

		@Override
		DownsampleHalfPixelFloat newInstance()
		{
			return new DownsampleHalfPixelFloat( this );
		}

		@Override
		void downsample( final float[] source, final int[] destSize, final float[] dest, final int dim )
		{
			downsample_halfpixel_float( source, destSize, dest, dim );
		}
	}

	static class DownsampleHalfPixelDouble extends AbstractDownsampleHalfPixel< DownsampleHalfPixelDouble, double[] >
	{
		public DownsampleHalfPixelDouble( final boolean[] downsampleInDim )
		{
			super( downsampleInDim, DOUBLE );
		}

		private DownsampleHalfPixelDouble( DownsampleHalfPixelDouble downsample )
		{
			super( downsample );
		}

		@Override
		DownsampleHalfPixelDouble newInstance()
		{
			return new DownsampleHalfPixelDouble( this );
		}

		@Override
		void downsample( final double[] source, final int[] destSize, final double[] dest, final int dim )
		{
			downsample_halfpixel_double( source, destSize, dest, dim );
		}
	}

	private static void downsample_float( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		if ( dim == 0 )
			downsampleX_float( source, destSize, dest );
		else
			downsampleN_float( source, destSize, dest, dim );
	}

	private static void downsampleX_float( final float[] source, final int[] destSize, final float[] dest )
	{
		final int len1 = destSize[ 0 ];
		final int len2 = mulDims( destSize, 1, destSize.length );
		for ( int z = 0; z < len2; ++z )
		{
			final int destOffsetZ = z * len1;
			final int srcOffsetZ = z * ( 2 * len1 + 1 );
			for ( int x = 0; x < len1; ++x )
			{
				dest[ destOffsetZ + x ] = wavg_float(
						source[ srcOffsetZ + 2 * x ],
						source[ srcOffsetZ + 2 * x + 1 ],
						source[ srcOffsetZ + 2 * x + 2 ] );
			}
		}
	}

	private static void downsampleN_float( final float[] source, final int[] destSize, final float[] dest, final int dim )
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
					dest[ destOffset + x ] = wavg_float(
							source[ srcOffset + x ],
							source[ srcOffset + x + len0 ],
							source[ srcOffset + x + 2 * len0 ] );
				}
			}
		}
	}

	private static float wavg_float( final float a, final float b, final float c )
	{
		return 0.25f * ( a + 2 * b + c );
	}


	private static void downsample_double( final double[] source, final int[] destSize, final double[] dest, final int dim )
	{
		if ( dim == 0 )
			downsampleX_double( source, destSize, dest );
		else
			downsampleN_double( source, destSize, dest, dim );
	}

	private static void downsampleX_double( final double[] source, final int[] destSize, final double[] dest )
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

	private static void downsampleN_double( final double[] source, final int[] destSize, final double[] dest, final int dim )
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

	private static double wavg_double( final double a, final double b, final double c )
	{
		return 0.25 * ( a + 2 * b + c );
	}

	private static void downsample_halfpixel_float( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		if ( dim == 0 )
			downsampleX_halfpixel_float( source, destSize, dest );
		else
			downsampleN_halfpixel_float( source, destSize, dest, dim );
	}

	private static void downsampleX_halfpixel_float( final float[] source, final int[] destSize, final float[] dest )
	{
		final int len = mulDims( destSize, 0, destSize.length );
		for ( int x = 0; x < len; ++x )
			dest[ x ] = avg_float(
					source[ 2 * x ],
					source[ 2 * x + 1 ] );
	}

	static void downsampleN_halfpixel_float( final float[] source, final int[] destSize, final float[] dest, final int dim )
	{
		int len0 = mulDims( destSize, 0, dim );
		int len1 = mulDims( destSize, dim, destSize.length );

		for ( int y = 0; y < len1; ++y )
		{
			final int destOffset = y * len0;
			final int srcOffset = 2 * destOffset;
			for ( int x = 0; x < len0; ++x )
				dest[ destOffset + x ] = avg_float(
						source[ srcOffset + x ],
						source[ srcOffset + x + len0 ] );
		}
	}

	private static float avg_float( final float a, final float b )
	{
		return 0.5f * ( a + b );
	}

	private static void downsample_halfpixel_double( final double[] source, final int[] destSize, final double[] dest, final int dim )
	{
		if ( dim == 0 )
			downsampleX_halfpixel_double( source, destSize, dest );
		else
			downsampleN_halfpixel_double( source, destSize, dest, dim );
	}

	private static void downsampleX_halfpixel_double( final double[] source, final int[] destSize, final double[] dest )
	{
		final int len = mulDims( destSize, 0, destSize.length );
		for ( int x = 0; x < len; ++x )
			dest[ x ] = avg_double(
					source[ 2 * x ],
					source[ 2 * x + 1 ] );
	}

	static void downsampleN_halfpixel_double( final double[] source, final int[] destSize, final double[] dest, final int dim )
	{
		int len0 = mulDims( destSize, 0, dim );
		int len1 = mulDims( destSize, dim, destSize.length );

		for ( int y = 0; y < len1; ++y )
		{
			final int destOffset = y * len0;
			final int srcOffset = 2 * destOffset;
			for ( int x = 0; x < len0; ++x )
				dest[ destOffset + x ] = avg_double(
						source[ srcOffset + x ],
						source[ srcOffset + x + len0 ] );
		}
	}

	private static double avg_double( final double a, final double b )
	{
		return 0.5 * ( a + b );
	}

	private static int mulDims( int[] dims, int from, int to )
	{
		int product = 1;
		for ( int d = from; d < to; ++d )
			product *= dims[ d ];
		return product;
	}
}

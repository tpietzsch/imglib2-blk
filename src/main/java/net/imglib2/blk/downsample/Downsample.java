package net.imglib2.blk.downsample;

import java.util.Arrays;
import net.imglib2.blk.downsample.GenericTypeConversionPlayground.TypeConvert;
import net.imglib2.blk.downsample.algo.BlockProcessor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.real.FloatType;

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


	public static < T extends NativeType< T >, P >
	BlockProcessor< P, P > viaFloat( final T type, final boolean[] downsampleInDim )
	{
		final TypeConvert< T, FloatType, P, float[] > convertToFloat = new TypeConvert<>( type, new FloatType() );
		final TypeConvert< FloatType, T, float[], P > convertFromFloat = new TypeConvert<>( new FloatType(), type );
		return convertToFloat.andThen( new Float( downsampleInDim ) ).andThen( convertFromFloat );
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

	private static double wavg_double( final double a, final double b, final double c )
	{
		return 0.25 * ( a + 2 * b + c );
	}

}

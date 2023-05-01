package net.imglib2.blk.downsample;

import java.util.Arrays;

import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;

public class DownsampleHalfPixel
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

	public static class Float extends AbstractDownsampleHalfPixel< Float, float[] >
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
			int destLineLength = 1;
			for ( int d = 0; d < destSize.length; ++d )
				destLineLength *= destSize[ d ];
			for ( int x = 0; x < destLineLength; ++x )
				dest[ x ] = 0.5f * ( source[ 2 * x ] + source[ 2 * x + 1 ] );
		}

		static void downsampleN( final float[] source, final int[] destSize, final float[] dest, final int dim )
		{
			int lineLength = 1;
			for ( int d = 0; d < dim; ++d )
				lineLength *= destSize[ d ];

			int nLines = 1;
			for ( int d = dim; d < destSize.length; ++d )
				nLines *= destSize[ d ];

			for ( int y = 0; y < nLines; ++y )
			{
				final int destOffset = y * lineLength;
				final int srcOffset = 2 * destOffset;
				for ( int x = 0; x < lineLength; ++x )
				{
					dest[ destOffset + x ] = 0.5f * source[ srcOffset + x ] + 0.5f * source[ srcOffset + x + lineLength ];
				}
			}
		}
	}

	public static class Double extends AbstractDownsampleHalfPixel< Double, double[] >
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
			if ( dim == 0 )
				downsampleX( source, destSize, dest );
			else
				downsampleN( source, destSize, dest, dim );
		}

		private static void downsampleX( final double[] source, final int[] destSize, final double[] dest )
		{
			int destLineLength = 1;
			for ( int d = 0; d < destSize.length; ++d )
				destLineLength *= destSize[ d ];
			for ( int x = 0; x < destLineLength; ++x )
				dest[ x ] = 0.5 * ( source[ 2 * x ] + source[ 2 * x + 1 ] );
		}

		static void downsampleN( final double[] source, final int[] destSize, final double[] dest, final int dim )
		{
			int lineLength = 1;
			for ( int d = 0; d < dim; ++d )
				lineLength *= destSize[ d ];

			int nLines = 1;
			for ( int d = dim; d < destSize.length; ++d )
				nLines *= destSize[ d ];

			for ( int y = 0; y < nLines; ++y )
			{
				final int destOffset = y * lineLength;
				final int srcOffset = 2 * destOffset;
				for ( int x = 0; x < lineLength; ++x )
					dest[ destOffset + x ] = 0.5 * ( source[ srcOffset + x ] + source[ srcOffset + x + lineLength ] );
			}
		}
	}
}

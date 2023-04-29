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

		private static void downsampleN( final float[] source, final int[] destSize, final float[] dest, final int dim )
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
			if ( dim == 0 )
				downsampleX( source, destSize, dest );
			else
				downsampleN( source, destSize, dest, dim );
		}

		private static void downsampleX( final double[] source, final int[] destSize, final double[] dest )
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
							0.25 * source[ si ] +
									0.5 * source[ si + 1 ] +
									0.25 * source[ si + 2 ];
				}
			}
		}

		private static void downsampleN( final double[] source, final int[] destSize, final double[] dest, final int dim )
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
						dest[ destOffset + x ] = 0.25 * source[ srcOffset + x ] +
								0.5 * source[ srcOffset + x + lineLength ] +
								0.25 * source[ srcOffset + x + 2 * lineLength ];
					}
				}
			}
		}
	}
}

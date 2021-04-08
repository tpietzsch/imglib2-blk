package net.imglib2.blk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ConvolveExample
{
	static class ExpectedResults
	{
		private final double[] sigmas;
		private final int[] targetSize;

		private final double[] source;
		private final Img< DoubleType > sourceImg;

		private final double[] target;
		private final Img< DoubleType > targetImg;

		public ExpectedResults( final int[] targetSize, final double[] sigmas )
		{
			final int n = targetSize.length;

			this.targetSize = targetSize;
			this.sigmas = sigmas;

			final int[] sizes = Gauss3.halfkernelsizes( sigmas );
			final int[] sourceSize = new int[ n ];
			Arrays.setAll( sourceSize, d -> targetSize[ d ] + 2 * ( sizes[ d ] - 1 ) );

			target = new double[ ( int ) Intervals.numElements( targetSize ) ];
			targetImg = ArrayImgs.doubles( target, Util.int2long( targetSize ) );

			source = new double[ ( int ) Intervals.numElements( sourceSize ) ];
			sourceImg = ArrayImgs.doubles( source, Util.int2long( sourceSize ) );

			final Random random = new Random( 1 );
			Arrays.setAll( source, i -> random.nextDouble() );

			final long[] shift = new long[ n ];
			Arrays.setAll( shift, d -> -( sizes[ d ] - 1 ) );
			Gauss3.gauss( sigmas, Views.translate( sourceImg, shift ), targetImg, 1 );
		}
	}

//	private final double[] source;
//	private final Img< DoubleType > sourceImg;
//
//	private final double[] target;
//	private final Img< DoubleType > targetImg;

	public ConvolveExample( ExpectedResults expectedResults )
	{
		final Kernel1D[] kernels = Kernel1D.symmetric( Gauss3.halfkernels( expectedResults.sigmas ) );

//		source = new double[ pw * ph * pd ];
//		Random random = new Random( 1 );
//		Arrays.setAll( source, i -> random.nextDouble() );
//		sourceImg = ArrayImgs.doubles( source, pw, ph, pd );
//
//		final int tw = pw - 2 * ( sizes[ 0 ] - 1 );
//		final int th = ph - 2 * ( sizes[ 1 ] - 1 );
//		final int td = pd - 2 * ( sizes[ 2 ] - 1 );
//		target = new double[ tw * th * td ];
//		targetImg = ArrayImgs.doubles( target, tw, th, td );

		System.out.println( "kernels = " + Arrays.toString( kernels ) );

		final int[] targetSizeZ = expectedResults.targetSize;
		final int[] targetSizeY = new int[] {
				targetSizeZ[ 0 ],
				targetSizeZ[ 1 ],
				targetSizeZ[ 2 ] + kernels[ 2 ].size() - 1,
		};
		final int[] targetSizeX = new int[] {
				targetSizeY[ 0 ],
				targetSizeY[ 1 ] + kernels[ 1 ].size() - 1,
				targetSizeY[ 2 ]
		};
		final int[] sourceSize = new int[] {
				targetSizeX[ 0 ] + kernels[ 0 ].size() - 1,
				targetSizeX[ 1 ],
				targetSizeX[ 2 ]
		};

		final double[] source = expectedResults.source;
		final double[] targetX = new double[ ( int ) Intervals.numElements( targetSizeX ) ];
		final double[] targetY = new double[ ( int ) Intervals.numElements( targetSizeY ) ];
		final double[] targetZ = new double[ ( int ) Intervals.numElements( targetSizeZ ) ];

		convolveX( source, sourceSize, targetX, targetSizeX, kernels[ 0 ] );
		convolveY( targetX, targetSizeX, targetY, targetSizeY, kernels[ 1 ] );
		convolveZ( targetY, targetSizeY, targetZ, targetSizeZ, kernels[ 2 ] );

		double diff = 0;
		for ( int i = 0; i < targetZ.length; i++ )
			diff += Math.abs( targetZ[ i ] - expectedResults.target[ i ] );

		System.out.println( "diff = " + diff );
	}

	// sx, sy, sz    : current position in source
	// tx, ty, tz    : current position in target
	// k             : current position in kernel

	// sxl, syl, szl : source size
	// txl, tyl, tzl : target size
	// kl            : kernel size

	private void convolveX(
			final double[] source,
			final int[] sourceSize,
			final double[] target,
			final int[] targetSize,
			final Kernel1D kernel1D )
	{
		final int sxl = sourceSize[ 0 ];
		final int syl = sourceSize[ 1 ];

		final int txl = targetSize[ 0 ];
		final int tyl = targetSize[ 1 ];
		final int tzl = targetSize[ 2 ];

		final double[] kernel = kernel1D.fullKernel();
		final int kl = kernel.length;

		for ( int x = 0; x < txl; ++x )
			for ( int y = 0; y < tyl; ++y )
				for ( int z = 0; z < tzl; ++z )
					for ( int k = 0; k < kl; ++k )
						target[ z * tyl * txl + y * txl + x ] += kernel[ k ] * source[ z * syl * sxl + y * sxl + ( x + k ) ];
	}

	private void convolveY(
			final double[] source,
			final int[] sourceSize,
			final double[] target,
			final int[] targetSize,
			final Kernel1D kernel1D )
	{
		final int sxl = sourceSize[ 0 ];
		final int syl = sourceSize[ 1 ];

		final int txl = targetSize[ 0 ];
		final int tyl = targetSize[ 1 ];
		final int tzl = targetSize[ 2 ];

		final double[] kernel = kernel1D.fullKernel();
		final int kl = kernel.length;

		for ( int x = 0; x < txl; ++x )
			for ( int y = 0; y < tyl; ++y )
				for ( int z = 0; z < tzl; ++z )
					for ( int k = 0; k < kl; ++k )
						target[ z * tyl * txl + y * txl + x ] += kernel[ k ] * source[ z * syl * sxl + ( y + k ) * sxl + x ];
	}

	private void convolveZ(
			final double[] source,
			final int[] sourceSize,
			final double[] target,
			final int[] targetSize,
			final Kernel1D kernel1D )
	{
		final int sxl = sourceSize[ 0 ];
		final int syl = sourceSize[ 1 ];

		final int txl = targetSize[ 0 ];
		final int tyl = targetSize[ 1 ];
		final int tzl = targetSize[ 2 ];

		final double[] kernel = kernel1D.fullKernel();
		final int kl = kernel.length;

		for ( int x = 0; x < txl; ++x )
			for ( int y = 0; y < tyl; ++y )
				for ( int z = 0; z < tzl; ++z )
					for ( int k = 0; k < kl; ++k )
						target[ z * tyl * txl + y * txl + x ] += kernel[ k ] * source[ ( z + k ) * syl * sxl + y * sxl + x ];
	}

	public static void main( String[] args )
	{
		final double[] sigmas = { 8, 8, 8 };
		final int[] targetSize = { 68, 68, 48 };
		final ExpectedResults expected = new ExpectedResults( targetSize, sigmas );
		new ConvolveExample( expected );
	}
}

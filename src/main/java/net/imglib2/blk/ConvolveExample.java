package net.imglib2.blk;

import java.util.Arrays;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.util.Intervals;

public class ConvolveExample
{
	final int[] sourceSize;
	final double[] source;

	final Kernel1D[] kernels;

	final int[] targetSizeX;
	final int[] targetSizeY;
	final int[] targetSizeZ;

	final double[] targetX;
	final double[] targetY;
	final double[] targetZ;

	public static void main( String[] args )
	{
		final double[] sigmas = { 8, 8, 8 };
		final int[] targetSize = { 68, 68, 48 };
		final RandomSourceData sourceData = new RandomSourceData( targetSize, sigmas );

		final ExpectedResults expected = new ExpectedResults( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
		final ConvolveExample actual = new ConvolveExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );

		expected.compute();
		actual.compute();

		double diff = 0;
		for ( int i = 0; i < actual.targetZ.length; i++ )
			diff += Math.abs( actual.targetZ[ i ] - expected.target[ i ] );
		System.out.println( "diff = " + diff );
	}

	public ConvolveExample( final int[] targetSize, final double[] sigmas, final double[] source, final int[] expectedSourceSize )
	{
		this.source = source;

		kernels = Kernel1D.symmetric( Gauss3.halfkernels( sigmas ) );

		targetSizeZ = targetSize;
		targetSizeY = new int[] {
				targetSizeZ[ 0 ],
				targetSizeZ[ 1 ],
				targetSizeZ[ 2 ] + kernels[ 2 ].size() - 1,
		};
		targetSizeX = new int[] {
				targetSizeY[ 0 ],
				targetSizeY[ 1 ] + kernels[ 1 ].size() - 1,
				targetSizeY[ 2 ]
		};
		sourceSize = new int[] {
				targetSizeX[ 0 ] + kernels[ 0 ].size() - 1,
				targetSizeX[ 1 ],
				targetSizeX[ 2 ]
		};

		// verify source size
		int diff = 0;
		for ( int i = 0; i < sourceSize.length; ++i )
			diff += Math.abs( sourceSize[ i ] - expectedSourceSize[ i ] );
		if ( diff != 0 )
			throw new IllegalArgumentException();

		targetX = new double[ ( int ) Intervals.numElements( targetSizeX ) ];
		targetY = new double[ ( int ) Intervals.numElements( targetSizeY ) ];
		targetZ = new double[ ( int ) Intervals.numElements( targetSizeZ ) ];
	}

	public void compute()
	{
		convolveX();
		convolveY();
		convolveZ();
	}

	public void convolveX()
	{
		convolveX( source, sourceSize, targetX, targetSizeX, kernels[ 0 ] );
	}

	public void convolveY()
	{
		convolveY( targetX, targetSizeX, targetY, targetSizeY, kernels[ 1 ] );
	}

	public void convolveZ()
	{
		convolveZ( targetY, targetSizeY, targetZ, targetSizeZ, kernels[ 2 ] );
	}

	// sx, sy, sz    : current position in source
	// tx, ty, tz    : current position in target
	// k             : current position in kernel

	// sxl, syl, szl : source size
	// txl, tyl, tzl : target size
	// kl            : kernel size

	private static void convolveX(
			final double[] source,
			final int[] sourceSize,
			final double[] target,
			final int[] targetSize,
			final Kernel1D kernel1D )
	{
//		System.out.println( "ConvolveExample.convolveX" );
		final int kstep = 1;
		convolve( source, sourceSize, target, targetSize, kernel1D, kstep );

//		final int ol = sourceSize[ 1 ] * sourceSize[ 2 ];
//		final int til = targetSize[ 0 ];
//		final int sil = sourceSize[ 0 ];
//		convolve2( source, target, kernel1D, ol, til, sil, kstep );
	}

	private static void convolveY(
			final double[] source,
			final int[] sourceSize,
			final double[] target,
			final int[] targetSize,
			final Kernel1D kernel1D )
	{
//		System.out.println( "ConvolveExample.convolveY" );
		final int kstep = sourceSize[ 0 ];
		convolve( source, sourceSize, target, targetSize, kernel1D, kstep );

//		final int ol = sourceSize[ 2 ];
//		final int til = targetSize[ 0 ] * targetSize[ 1 ];
//		final int sil = sourceSize[ 0 ] * sourceSize[ 1 ];
//		convolve2( source, target, kernel1D, ol, til, sil, kstep );
	}

	private static void convolveZ(
			final double[] source,
			final int[] sourceSize,
			final double[] target,
			final int[] targetSize,
			final Kernel1D kernel1D )
	{
//		System.out.println( "ConvolveExample.convolveZ" );
		final int kstep = sourceSize[ 1 ] * sourceSize[ 0 ];
		convolve( source, sourceSize, target, targetSize, kernel1D, kstep );

//		final int ol = 1;
//		final int til = targetSize[ 0 ] * targetSize[ 1 ] * targetSize[ 2 ];
//		final int sil = sourceSize[ 0 ] * sourceSize[ 1 ] * sourceSize[ 2 ];
//		convolve2( source, target, kernel1D, ol, til, sil, kstep );
	}

	private static void convolve(
			final double[] source,
			final int[] sourceSize,
			final double[] target,
			final int[] targetSize,
			final Kernel1D kernel1D,
			final int kstep )
	{
		final int sxl = sourceSize[ 0 ];
		final int syl = sourceSize[ 1 ];

		final int txl = targetSize[ 0 ];
		final int tyl = targetSize[ 1 ];
		final int tzl = targetSize[ 2 ];

		final double[] kernel = kernel1D.fullKernel();
		final int kl = kernel.length;

		final double[] sourceCopy = new double[ txl ];
		final double[] targetCopy = new double[ txl ];

		for ( int z = 0; z < tzl; ++z )
			for ( int y = 0; y < tyl; ++y )
			{
				final int tzy = z * tyl * txl + y * txl;
				final int szy = z * syl * sxl + y * sxl;
				Arrays.fill( targetCopy, 0 );
				for ( int k = 0; k < kl; ++k )
				{
					// NB: Copy data to make auto-vectorization happen.
					// See https://richardstartin.github.io/posts/multiplying-matrices-fast-and-slow
					System.arraycopy( source, szy + k * kstep, sourceCopy, 0, sourceCopy.length );
					line( sourceCopy, targetCopy, txl, kernel[ k ] );
//					line( source, target, txl, kernel[ k ], tzy, szy + k * kstep );
				}
				System.arraycopy( targetCopy, 0, target, tzy, targetCopy.length );
			}
	}

	private static void line( final double[] source, final double[] target, final int txl, final double v )
	{
		for ( int x = 0; x < txl; ++x )
//			target[ x ] = Math.fma( v, source[ x ], target[ x ] );
			target[ x ] += v * source[ x ];
	}

	private static void convolve2(
			final double[] source,
			final double[] target,
			final Kernel1D kernel1D,
			final int ol,
			final int til,
			final int sil, // == til + ( kernel.length - 1 ) * kstep
			final int kstep )
	{
		final double[] kernel = kernel1D.fullKernel();
		final int kl = kernel.length;

//		System.out.println( "ol = " + ol );
//		System.out.println( "sil = " + sil );
//		System.out.println( "til = " + til );
//		System.out.println( "sil - til = " + (sil - til) );
//		System.out.println( "kstep = " + kstep );
//		System.out.println( "kernel1D = " + kernel1D.size() );
//		System.out.println();

		final int bw = 64;
		final double[] sourceCopy = new double[ bw ];
		final double[] targetCopy = new double[ bw ];
		final int nBlocks = til / bw;
		final int trailing = til - nBlocks * bw;

//		System.out.println( "til = " + til );
//		System.out.println( "trailing = " + trailing );

		for ( int o = 0; o < ol; ++o )
		{
			final int to = o * til;
			final int so = o * sil;

			for ( int b = 0; b < nBlocks; ++b )
			{
				final int tob = to + b * bw;
				final int sob = so + b * bw;

				Arrays.fill( targetCopy, 0 );
				for ( int k = 0; k < kl; ++k )
				{
					// NB: Copy data to make auto-vectorization happen.
					// See https://richardstartin.github.io/posts/multiplying-matrices-fast-and-slow
					System.arraycopy( source, sob + k * kstep, sourceCopy, 0, bw );
					line( sourceCopy, targetCopy, bw, kernel[ k ] );
				}
				System.arraycopy( targetCopy, 0, target, tob, bw );
			}
			if ( trailing > 0 )
			{
				final int tob = to + nBlocks * bw;
				final int sob = so + nBlocks * bw;

				Arrays.fill( targetCopy, 0 );
				for ( int k = 0; k < kl; ++k )
				{
					// NB: Copy data to make auto-vectorization happen.
					// See https://richardstartin.github.io/posts/multiplying-matrices-fast-and-slow
					System.arraycopy( source, sob + k * kstep, sourceCopy, 0, trailing );
					line( sourceCopy, targetCopy, trailing, kernel[ k ] );
				}
				System.arraycopy( targetCopy, 0, target, tob, trailing );
			}
		}
	}


}

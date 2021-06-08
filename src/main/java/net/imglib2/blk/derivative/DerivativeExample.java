package net.imglib2.blk.derivative;

import java.util.Arrays;
import java.util.List;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blk.GaussDoubleBlocked;
import net.imglib2.util.Intervals;

public class DerivativeExample
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
		final int[] targetSize = { 32, 32, 32 };

		final Kernel1D[] kernels = Kernel1D.symmetric( Gauss3.halfkernels( sigmas ) );
		final RandomSourceData sourceData = new RandomSourceData( targetSize, kernels );

		final ExpectedResults expected = new ExpectedResults( targetSize, kernels, sourceData.source, sourceData.sourceSize );
		final DerivativeExample actual = new DerivativeExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );

		double diff = 0;
		for ( int i = 0; i < actual.targetZ.length; i++ )
			diff += Math.abs( actual.targetZ[ i ] - expected.target[ i ] );
		System.out.println( "diff = " + diff );
	}

	public DerivativeExample( final int[] targetSize, final double[] sigmas, final double[] source, final int[] expectedSourceSize )
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

	private static final int blockSize = 1024;

	public void convolveX()
	{
//		final int d = 0; // this is X direction
//		final int n = 3; // XYZ
//		int ol = 1;
//		for ( int dd = d + 1; dd < n; ++dd )
//			ol *= targetSizeX[ dd ];
		final int ol = targetSizeX[ 1 ] * targetSizeX[ 2 ];

//		final int d = 0; // this is X direction
//		int til = 1;
//		for ( int dd = 0; dd < d + 1; ++dd )
//			til *= targetSizeX[ dd ];
		final int til = targetSizeX[ 0 ];

//		final int d = 0; // this is X direction
//		int kstep = 1;
//		for ( int dd = 0; dd < d; ++dd )
//			kstep *= sourceSize[ dd ];
		final int kstep = 1;

		GaussDoubleBlocked.convolve( source, targetX, kernels[ 0 ], ol, til, kstep, blockSize );
//		convolveX( source, sourceSize, targetX, targetSizeX, kernels[ 0 ] );
	}

	public void convolveY()
	{
//		final int d = 1; // this is Y direction
//		final int n = 3; // XYZ
//		int ol = 1;
//		for ( int dd = d + 1; dd < n; ++dd )
//			ol *= targetSizeY[ dd ];
		final int ol = targetSizeY[ 2 ];

//		final int d = 1; // this is Y direction
//		int til = 1;
//		for ( int dd = 0; dd < d + 1; ++dd )
//			til *= targetSizeY[ dd ];
		final int til = targetSizeY[ 0 ] * targetSizeY[ 1 ];

//		final int d = 1; // this is Y direction
//		int kstep = 1;
//		for ( int dd = 0; dd < d; ++dd )
//			kstep *= targetSizeX[ dd ];
		final int kstep = targetSizeX[ 0 ];

		GaussDoubleBlocked.convolve( source, targetX, kernels[ 1 ], ol, til, kstep, blockSize );
//		convolveY( targetX, targetSizeX, targetY, targetSizeY, kernels[ 1 ] );
	}

	public void convolveZ()
	{
//		final int d = 2; // this is Z direction
//		final int n = 3; // XYZ
//		int ol = 1;
//		for ( int dd = d + 1; dd < n; ++dd )
//			ol *= targetSizeZ[ dd ];
		final int ol = 1;

//		final int d = 2; // this is Z direction
//		int til = 1;
//		for ( int dd = 0; dd < d + 1; ++dd )
//			til *= targetSizeZ[ dd ];
		final int til = targetSizeZ[ 0 ] * targetSizeZ[ 1 ] * targetSizeZ[ 2 ];

//		final int d = 2; // this is Z direction
//		int kstep = 1;
//		for ( int dd = 0; dd < d; ++dd )
//			kstep *= targetSizeY[ dd ];
		final int kstep = targetSizeY[ 0 ] * targetSizeY[ 1 ];

		GaussDoubleBlocked.convolve( source, targetX, kernels[ 2 ], ol, til, kstep, blockSize );
//		convolveZ( targetY, targetSizeY, targetZ, targetSizeZ, kernels[ 2 ] );
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


//	private void derivativeConvolver( final int[] orders )
//	{
//		final double pixelSizei = 0;
//		final List< Convolution< NumericType< ? > > > convolutions = new ArrayList<>();
//		for ( int i = 0; i < orders.length; i++ )
//		{
//			final int order = orders[ i ];
//			if ( order != 0 )
//			{
//				Kernel1D multiply = multiply( SIMPLE_KERNELS.get( order ), Math.pow( pixelSizei, -order ) );
//				convolutions.add( SeparableKernelConvolution.convolution1d( multiply, i ) );
//			}
//		}
//		if (convolutions.isEmpty())
//			throw new IllegalArgumentException(); // TODO: just return input???
//
//		final Convolution< NumericType< ? > > convolution = Convolution.concat( convolutions );
//
//		final int numThreads = 1;
//		final ExecutorService service = Executors.newFixedThreadPool( numThreads );
//		convolution.setExecutor( service );
//		convolution.process( Views.translate( sourceImg, shift ), targetImg );
//		service.shutdown();
//	}


	// TODO: move to DerivativeExample
	public static List<Kernel1D> SIMPLE_KERNELS = Arrays.asList(
			Kernel1D.centralAsymmetric(1),
			Kernel1D.centralAsymmetric(0.5, 0, -0.5),
			Kernel1D.centralAsymmetric(1, -2, 1));

	private Kernel1D multiply(Kernel1D kernel1D, double scaleFactor) {
		double[] fullKernel = multiply(kernel1D.fullKernel(), scaleFactor);
		int originIndex = (int) -kernel1D.min();
		return Kernel1D.asymmetric(fullKernel, originIndex);
	}

	private double[] multiply(double[] array, double scaleFactor) {
		double[] result = new double[array.length];
		for (int i = 0; i < array.length; i++)
			result[i] = array[i] * scaleFactor;
		return result;
	}
}

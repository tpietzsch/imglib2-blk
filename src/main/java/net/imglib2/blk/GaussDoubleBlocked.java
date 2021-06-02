package net.imglib2.blk;

import java.util.Arrays;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.util.Intervals;

public class GaussDoubleBlocked
{
	private final int n;
	private final int[] targetSize;
	private final int[] sourceSize;
	private final int[] sourceOffset;

	private static final int blockSize = 1024;

	private final Kernel1D[] kernels;
	private final double[][] targets;

	private final int ols[];
	private final int ils[];
	private final int ksteps[];

	public GaussDoubleBlocked( final double[] sigmas )
	{
		kernels = Kernel1D.symmetric( Gauss3.halfkernels( sigmas ) );

		n = kernels.length;
		targets = new double[ n ][];
		ols = new int[ n ];
		ils = new int[ n ];
		ksteps = new int[ n ];

		targetSize = new int[ n ];
		sourceSize = new int[ n ];
		sourceOffset = new int[ n ];
	}

	public void setTargetSize( final int[] targetSize )
	{
		if ( Arrays.equals( targetSize, this.targetSize ) )
			return;

		for ( int d = 0; d < n; d++ )
		{
			this.targetSize[ d ] = targetSize[ d ];
			sourceSize[ d ] = targetSize[ d ];
		}

		for ( int d = n - 1; d >= 0; --d )
		{
			ils[ d ] = 1;
			for ( int dd = 0; dd < d + 1; ++dd )
				ils[ d ] *= sourceSize[ dd ];

			ols[ d ] = 1;
			for ( int dd = d + 1; dd < n; ++dd )
				ols[ d ] *= sourceSize[ dd ];

			ksteps[ d ] = 1;
			for ( int dd = 0; dd < d; ++dd )
				ksteps[ d ] *= sourceSize[ dd ];

			if ( d < n - 1 )
			{
				final int l = ( int ) Intervals.numElements( sourceSize );
				if ( targets[ d ] == null || targets[ d ].length < l )
					targets[ d ] = new double[ l ];
			}

			sourceSize[ d ] += kernels[ d ].size() - 1;
			sourceOffset[ d ] = - ( kernels[ d ].size() - 1 ) / 2;
		}
	}

	public int[] getSourceSize()
	{
		return sourceSize;
	}

	public int[] getSourceOffset()
	{
		return sourceOffset;
	}

	// optional. also other arrays can be passed to compute()
	public double[] getSourceBuffer()
	{
		final int l = ( int ) Intervals.numElements( sourceSize );
		if ( targets[ n - 1 ] == null || targets[ n - 1 ].length < l )
			targets[ n - 1 ] = new double[ l ];
		return targets[ n -  1 ];
	}

	public void compute( final double[] source, final double[] target )
	{
		for ( int d = 0; d < n; ++d )
			convolve(
					d == 0 ? source : targets[ d - 1 ],
					d == n - 1 ? target : targets[ d ],
					kernels[ d ], ols[ d ], ils[ d ], ksteps[ d ], blockSize );
	}

	static void convolve(
			final double[] source,
			final double[] target,
			final Kernel1D kernel1D,
			final int ol,
			final int til,
			final int kstep,
			final int bw )
	{
		final double[] kernel = kernel1D.fullKernel();
		final int kl = kernel.length;
		final int sil= til + ( kl - 1 ) * kstep;

		final double[] sourceCopy = new double[ bw ];
		final double[] targetCopy = new double[ bw ];
		final int nBlocks = til / bw;
		final int trailing = til - nBlocks * bw;

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

	private static void line( final double[] source, final double[] target, final int txl, final double v )
	{
		for ( int x = 0; x < txl; ++x )
//			target[ x ] = Math.fma( v, source[ x ], target[ x ] );
			target[ x ] += v * source[ x ];
	}
}

package net.imglib2.blk;

import java.util.Arrays;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.util.Intervals;

public class ConvolveFloatBlockedExample
{
	final float[] source;
	final int[] targetSize;

	final Kernel1D[] kernels;

	final float[][] targets;

	final int ols[];
	final int ils[];
	final int ksteps[];

	public ConvolveFloatBlockedExample( final int[] targetSize, final double[] sigmas, final float[] source, final int[] expectedSourceSize )
	{
		this.source = source;
		this.targetSize = targetSize;

		kernels = Kernel1D.symmetric( Gauss3.halfkernels( sigmas ) );

		final int n = targetSize.length;
		targets = new float[ n ][];
		ols = new int[ n ];
		ils = new int[ n ];
		ksteps = new int[ n ];

		final int[] size = targetSize.clone();
		for ( int d = n - 1; d >= 0; --d )
		{
			ils[ d ] = 1;
			for ( int dd = 0; dd < d + 1; ++dd )
				ils[ d ] *= size[ dd ];

			ols[ d ] = 1;
			for ( int dd = d + 1; dd < n; ++dd )
				ols[ d ] *= size[ dd ];

			ksteps[ d ] = 1;
			for ( int dd = 0; dd < d; ++dd )
				ksteps[ d ] *= size[ dd ];

			targets[ d ] = new float[ ( int ) Intervals.numElements( size ) ];

			size[ d ] += kernels[ d ].size() - 1;
		}

		// verify source size
		int diff = 0;
		for ( int i = 0; i < size.length; ++i )
			diff += Math.abs( size[ i ] - expectedSourceSize[ i ] );
		if ( diff != 0 )
			throw new IllegalArgumentException();
	}

	public void compute( final int blockSize )
	{
		final int n = targets.length;
		for ( int d = 0; d < n; ++d )
			convolve2( d == 0 ? source : targets[ d - 1 ], targets[ d ], kernels[ d ], ols[ d ], ils[ d ], ksteps[ d ], blockSize );
	}

	private static float[] toFloats( final double[] doubles )
	{
		float[] floats = new float[ doubles.length ];
		for ( int i = 0; i < doubles.length; i++ )
			floats[ i ] = ( float ) doubles[ i ];
		return floats;
	}

	private static void convolve(
			final float[] source,
			final float[] target,
			final Kernel1D kernel1D,
			final int ol,
			final int til,
			final int kstep,
			final int bw )
	{
		final float[] kernel = toFloats( kernel1D.fullKernel() );
		final int kl = kernel.length;
		final int sil= til + ( kl - 1 ) * kstep;

//		System.out.println( "til = " + til );
//		System.out.println( "kstep = " + kstep );
//		System.out.println( "kernel1D = " + kernel1D.size() );

//		final int bw = 1 << 7;
		final float[] sourceCopy = new float[ bw ];
		final float[] targetCopy = new float[ bw ];
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

	private static void line( final float[] source, final float[] target, final int txl, final float v )
	{
		for ( int x = 0; x < txl; ++x )
			target[ x ] = Math.fma( v, source[ x ], target[ x ] );
//			target[ x ] += v * source[ x ];
	}

	private static void convolve2(
			final float[] source,
			final float[] target,
			final Kernel1D kernel1D,
			final int ol,
			final int til,
			final int kstep,
			final int bw )
	{
		final float[] kernel = toFloats( kernel1D.fullKernel() );
		final int kl = kernel.length;
		final int sil = til + ( kl - 1 ) * kstep;

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

				Arrays.fill( target, tob, tob + bw, 0 );
				for ( int k = 0; k < kl; ++k )
					line2( source, sob + k * kstep, target, tob, bw, kernel[ k ] );
			}
			if ( trailing > 0 )
			{
				final int tob = to + nBlocks * bw;
				final int sob = so + nBlocks * bw;

				Arrays.fill( target, tob, tob + trailing, 0 );
				for ( int k = 0; k < kl; ++k )
					line2( source, sob + k * kstep, target, tob, trailing, kernel[ k ] );
			}
		}
	}

	private static final VectorSpecies< Float > SPECIES = FloatVector.SPECIES_PREFERRED;

	private static void line2( final float[] source, final int so, final float[] target, final int to, final int l, final float v )
	{
		int x = 0;
		for (; x < SPECIES.loopBound( l ); x += SPECIES.length() )
		{
			final FloatVector s = FloatVector.fromArray( SPECIES, source, so + x );
			final FloatVector t = FloatVector.fromArray( SPECIES, target, to + x );
			final FloatVector r = s.fma( FloatVector.broadcast( SPECIES, v ), t );
			r.intoArray( target, to + x );
		}

		for ( ; x < l; ++x )
			target[ to + x ] = Math.fma( v, source[ so + x ], target[ to + x ] );
//			target[ x ] += v * source[ x ];
	}

}

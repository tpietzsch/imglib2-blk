package net.imglib2.blk.derivative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

class ExpectedResults
{
	private final Kernel1D[] kernels;

	final double[] target;

	final Img< DoubleType > sourceImg;
	final Img< DoubleType > targetImg;

	private final long[] shift;

	public ExpectedResults( final int[] targetSize, final Kernel1D[] kernels, final double[] source, final int[] sourceSize )
	{
		this.kernels = kernels;
		final int n = targetSize.length; // expected: 3

		target = new double[ ( int ) Intervals.numElements( targetSize ) ];
		targetImg = ArrayImgs.doubles( target, Util.int2long( targetSize ) );
		sourceImg = ArrayImgs.doubles( source, Util.int2long( sourceSize ) );

		shift = new long[ n ];
		Arrays.setAll( shift, d -> {
			final Kernel1D kernel = kernels[ d ];
			final int pad = kernel == null ? 0 : ( kernel.size() - 1 );
			return -( pad / 2 );
		} );
	}

	public void compute()
	{
		final List< Convolution< NumericType< ? > > > convolutions = new ArrayList<>();
		for ( int d = 0; d < kernels.length; d++ )
		{
			final Kernel1D kernel = kernels[ d ];
			if ( kernel != null )
				convolutions.add( SeparableKernelConvolution.convolution1d( kernel, d ) );
		}
		if (convolutions.isEmpty())
			throw new IllegalArgumentException(); // TODO: just return input???
		final Convolution< NumericType< ? > > convolution = Convolution.concat( convolutions );

		final int numThreads = 1;
		final ExecutorService service = Executors.newFixedThreadPool( numThreads );
		convolution.setExecutor( service );
		convolution.process( Views.translate( sourceImg, shift ), targetImg );
		service.shutdown();
	}
}

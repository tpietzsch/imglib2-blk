package net.imglib2.blk.derivative;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import java.util.Arrays;
import java.util.List;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blk.GaussDoubleBlocked;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.util.Intervals;

public class DerivativeExample
{
	final Kernel1D[] kernels;

	final int[] targetSize;
	final double[] source;
	final double[] target;

	public static void main( String[] args )
	{
		final double[] sigmas = { 8, 8, 8 };
		final int[] targetSize = { 32, 32, 32 };

		final Kernel1D[] kernels = Kernel1D.symmetric( Gauss3.halfkernels( sigmas ) );
		final RandomSourceData sourceData = new RandomSourceData( targetSize, kernels );

		final ExpectedResults expected = new ExpectedResults( targetSize, kernels, sourceData.source, sourceData.sourceSize );
		final DerivativeExample actual = new DerivativeExample( targetSize, kernels, sourceData.source );

		expected.compute();
		actual.compute();

		double diff = 0;
		for ( int i = 0; i < actual.target.length; i++ )
			diff += Math.abs( actual.target[ i ] - expected.target[ i ] );


//		Bdv bdv = BdvFunctions.show( ArrayImgs.doubles( expected.target, 32, 32, 32 ), "expected" );
//		BdvFunctions.show( ArrayImgs.doubles( actual.target, 32, 32, 32 ), "actual", Bdv.options().addTo( bdv ) );

		System.out.println( "diff = " + diff );
	}

	public DerivativeExample( final int[] targetSize, final Kernel1D[] kernels, final double[] source )
	{
		this.targetSize = targetSize;
		this.source = source;
		this.kernels = kernels;
		target = new double[ ( int ) Intervals.numElements( targetSize ) ];
	}

	public void compute()
	{
//		final double[] sigmas = { 8, 8, 8 };
//		final GaussDoubleBlocked convolver = new GaussDoubleBlocked( sigmas );
		final ConvolveDoubleBlocked convolver = new ConvolveDoubleBlocked( kernels );
		convolver.setTargetSize( targetSize );
		convolver.compute( source, target );
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

	public static final List< Kernel1D > SIMPLE_KERNELS = Arrays.asList(
			Kernel1D.centralAsymmetric( 1 ),
			Kernel1D.centralAsymmetric( 0.5, 0, -0.5 ),
			Kernel1D.centralAsymmetric( 1, -2, 1 ) );

	private Kernel1D multiply( final Kernel1D kernel1D, final double scaleFactor )
	{
		double[] fullKernel = multiply( kernel1D.fullKernel(), scaleFactor );
		int originIndex = ( int ) -kernel1D.min();
		return Kernel1D.asymmetric( fullKernel, originIndex );
	}

	private double[] multiply( final double[] array, final double scaleFactor )
	{
		double[] result = new double[ array.length ];
		for ( int i = 0; i < array.length; i++ )
			result[ i ] = array[ i ] * scaleFactor;
		return result;
	}
}

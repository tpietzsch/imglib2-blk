package net.imglib2.blk;

import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blk.derivative.ConvolveDoubleBlocked;

public class GaussDoubleBlocked
{
	private final ConvolveDoubleBlocked convolve;

	public GaussDoubleBlocked( final double[] sigmas )
	{
		convolve = new ConvolveDoubleBlocked( Kernel1D.symmetric( Gauss3.halfkernels( sigmas ) ) );
	}

	public void setTargetSize( final int[] targetSize )
	{
		convolve.setTargetSize( targetSize );
	}

	public int[] getSourceSize()
	{
		return convolve.getSourceSize();
	}

	public int[] getSourceOffset()
	{
		return convolve.getSourceOffset();
	}

	// optional. also other arrays can be passed to compute()
	public double[] getSourceBuffer()
	{
		return convolve.getSourceBuffer();
	}

	public void compute( final double[] source, final double[] target )
	{
		convolve.compute( source, target );
	}
}

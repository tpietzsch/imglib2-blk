package net.imglib2.blk;

import java.util.Arrays;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blk.derivative.ConvolveFloatBlocked;
import net.imglib2.util.Intervals;

public class GaussFloatBlocked1D
{
	private final int n;

	private final int[] targetSize;
	private final int[] sourceSize;
	private final int[] sourceOffset;

	private static final int blockSize = 2048;

	private final int dim;
	private final Kernel1D kernel;
	private float[] sourceBuffer;

	private int ol;
	private int il;
	private int kstep;

	/**
	 * @param n number of dimensions
	 * @param dim dimension along which to convolve
	 * @param sigma sigma along {@code dim}
	 */
	public GaussFloatBlocked1D( final int n, final int dim, final double sigma )
	{
		this.n = n;
		this.dim = dim;
		kernel = Kernel1D.symmetric( Gauss3.halfkernels( new double[] { sigma } )[ 0 ] );

		targetSize = new int[ n ];
		sourceSize = new int[ n ];
		sourceOffset = new int[ n ];
		sourceOffset[ dim ] = - ( kernel.size() - 1 ) / 2;
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

		il = 1;
		for ( int dd = 0; dd < dim + 1; ++dd )
			il *= targetSize[ dd ];

		ol = 1;
		for ( int dd = dim + 1; dd < n; ++dd )
			ol *= targetSize[ dd ];

		kstep = 1;
		for ( int dd = 0; dd < dim; ++dd )
			kstep *= targetSize[ dd ];

		sourceSize[ dim ] += kernel.size() - 1;
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
	public float[] getSourceBuffer()
	{
		final int l = ( int ) Intervals.numElements( sourceSize );
		if ( sourceBuffer == null || sourceBuffer.length < l )
			sourceBuffer = new float[ l ];
		return sourceBuffer;
	}

	public void compute( final float[] source, final float[] target )
	{
		ConvolveFloatBlocked.convolve( source, target, kernel, ol, il, kstep, blockSize );
	}
}

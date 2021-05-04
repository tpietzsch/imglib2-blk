package net.imglib2.blk;

import java.util.Arrays;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

class ExpectedResultsFloat
{
	private final double[] sigmas;

	final float[] target;

	final Img< FloatType > sourceImg;
	final Img< FloatType > targetImg;

	private final long[] shift;

	public ExpectedResultsFloat( final int[] targetSize, final double[] sigmas, final float[] source, final int... sourceSize )
	{
		this.sigmas = sigmas;
		final int n = targetSize.length; // expected: 3

		target = new float[ ( int ) Intervals.numElements( targetSize ) ];
		targetImg = ArrayImgs.floats( target, Util.int2long( targetSize ) );
		sourceImg = ArrayImgs.floats( source, Util.int2long( sourceSize ) );

		shift = new long[ n ];
		final int[] sizes = Gauss3.halfkernelsizes( sigmas );
		Arrays.setAll( shift, d -> -( sizes[ d ] - 1 ) );
	}

	public void compute()
	{
		Gauss3.gauss( sigmas, Views.translate( sourceImg, shift ), targetImg, 1 );
	}
}

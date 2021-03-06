package net.imglib2.blk;

import org.junit.Assert;
import org.junit.Test;

public class ConvolveTest
{
	@Test
	public void testConvolve()
	{
		final double[] sigmas = { 8, 8, 8 };
		final int[] targetSize = { 128, 128, 128 };
		final RandomSourceData sourceData = new RandomSourceData( targetSize, sigmas );

		final ExpectedResults expected = new ExpectedResults( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
		expected.compute();
		final ConvolveExample actual = new ConvolveExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
		actual.compute();

		Assert.assertArrayEquals( expected.target, actual.targetZ, 0.000001 );
	}

	@Test
	public void testConvolveBlocked()
	{
		final double[] sigmas = { 8, 8, 8 };
		final int[] targetSize = { 128, 128, 128 };
		final RandomSourceData sourceData = new RandomSourceData( targetSize, sigmas );

		final ExpectedResults expected = new ExpectedResults( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
		expected.compute();
		final ConvolveBlockedExample actual = new ConvolveBlockedExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
		actual.compute( 32 );

		Assert.assertArrayEquals( expected.target, actual.targets[ actual.targets.length - 1 ], 0.000001 );
	}

	@Test
	public void testConvolveFloatBlocked()
	{
		final double[] sigmas = { 8, 8, 8 };
		final int[] targetSize = { 128, 128, 128 };
		final RandomSourceDataFloat sourceData = new RandomSourceDataFloat( targetSize, sigmas );

		final ExpectedResultsFloat expected = new ExpectedResultsFloat( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
		expected.compute();
		final ConvolveFloatBlockedExample actual = new ConvolveFloatBlockedExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
		actual.compute( 32 );

		Assert.assertArrayEquals( expected.target, actual.targets[ actual.targets.length - 1 ], 0.000001f );
	}
}

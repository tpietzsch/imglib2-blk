package net.imglib2.blk;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State( Scope.Benchmark )
@Warmup( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class ConvolveBenchmark
{
	final double[] sigmas = { 8, 8, 8 };
	final int[] targetSize = { 68, 68, 48 };
//	final int[] targetSize = { 128, 128, 128 };

	final RandomSourceData sourceData = new RandomSourceData( targetSize, sigmas );
	final ExpectedResults gauss3 = new ExpectedResults( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
	final ConvolveExample convolve = new ConvolveExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );

	public ConvolveBenchmark()
	{
		System.out.println( "targetSizeX = " + Intervals.numElements( convolve.targetSizeX ));
		System.out.println( "targetSizeY = " + Intervals.numElements( convolve.targetSizeY ));
		System.out.println( "targetSizeZ = " + Intervals.numElements( convolve.targetSizeZ ));
	}

//	@Benchmark
	public void benchmarkGauss3()
	{
		gauss3.compute();
	}

//	@Benchmark
	public void benchmarkConvolve()
	{
		convolve.compute();
	}

	@Benchmark
	public void benchmarkConvolveX()
	{
		convolve.convolveX();
	}

	@Benchmark
	public void benchmarkConvolveY()
	{
		convolve.convolveY();
	}

	@Benchmark
	public void benchmarkConvolveZ()
	{
		convolve.convolveZ();
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( ConvolveBenchmark.class.getSimpleName() ).build();
		new Runner( options ).run();
	}
}

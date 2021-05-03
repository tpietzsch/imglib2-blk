package net.imglib2.blk;

import java.util.concurrent.TimeUnit;
import net.imglib2.util.Intervals;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State( Scope.Benchmark )
@Warmup( iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class ConvolveBlockedBenchmark
{
	final double[] sigmas = { 8, 8, 8 };
	final int[] targetSize = { 68, 68, 48 };
//	final double[] sigmas = { 4, 4, 4 };
//	final int[] targetSize = { 128, 128, 128 };

	final RandomSourceData sourceData = new RandomSourceData( targetSize, sigmas );
	final ExpectedResults gauss3 = new ExpectedResults( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
	final ConvolveExample convolve = new ConvolveExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
	final ConvolveBlockedExample convolveBlocked = new ConvolveBlockedExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );

//	@Param({"4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768", "65536"})
//	@Param({"64", "96", "128", "160", "192", "224", "256", "288", "320", "352", "384", "416", "448", "480", "512"})
	public int blockSize = 192;

	public ConvolveBlockedBenchmark()
	{
	}

	@Benchmark
	public void benchmarkGauss3()
	{
		gauss3.compute();
	}

	@Benchmark
	public void benchmarkConvolve()
	{
		convolve.compute();
	}

	@Benchmark
	public void benchmarkConvolveBlocked()
	{
		convolveBlocked.compute( blockSize );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( ConvolveBlockedBenchmark.class.getSimpleName() ).build();
		new Runner( options ).run();
	}
}

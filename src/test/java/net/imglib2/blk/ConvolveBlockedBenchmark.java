package net.imglib2.blk;

import java.util.concurrent.TimeUnit;
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
//	final double[] sigmas = { 2, 2, 2 };
//	final int[] targetSize = { 128, 128, 128 };

	final RandomSourceData sourceData = new RandomSourceData( targetSize, sigmas );
	final ExpectedResults gauss3 = new ExpectedResults( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
	final ConvolveExample convolve = new ConvolveExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
	final ConvolveBlockedExample convolveBlocked = new ConvolveBlockedExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );

//	@Param({"4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768", "65536"})
//	@Param({"64", "96", "128", "160", "192", "224", "256", "288", "320", "352", "384", "416", "448", "480", "512"})
//	@Param( { "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384" } )
	public int blockSize = 2048;


//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked           64  avgt   20  18,269 ± 1,999  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked          128  avgt   20  14,336 ± 1,231  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked          256  avgt   20  14,250 ± 2,447  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked          512  avgt   20  14,715 ± 2,434  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         1024  avgt   20  13,810 ± 2,262  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         2048  avgt   20  13,842 ± 1,171  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         4096  avgt   20  17,095 ± 2,044  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         8192  avgt   20  15,486 ± 1,192  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked        16384  avgt   20  16,081 ± 1,311  ms/op

//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked           64  avgt   20  28,772 ± 2,101  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked          128  avgt   20  27,331 ± 2,671  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked          256  avgt   20  27,129 ± 1,885  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked          512  avgt   20  28,230 ± 2,126  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         1024  avgt   20  29,271 ± 1,524  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         2048  avgt   20  26,984 ± 2,369  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         4096  avgt   20  29,769 ± 1,354  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked         8192  avgt   20  29,648 ± 3,851  ms/op
//	ConvolveBlockedBenchmark.benchmarkConvolveBlocked        16384  avgt   20  28,903 ± 2,152  ms/op


	public ConvolveBlockedBenchmark()
	{
	}

	@Benchmark
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

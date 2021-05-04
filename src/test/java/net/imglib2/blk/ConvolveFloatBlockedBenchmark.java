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
public class ConvolveFloatBlockedBenchmark
{
	final double[] sigmas = { 8, 8, 8 };
	final int[] targetSize = { 68, 68, 48 };
//	final double[] sigmas = { 2, 2, 2 };
//	final int[] targetSize = { 128, 128, 128 };

	final RandomSourceDataFloat sourceData = new RandomSourceDataFloat( targetSize, sigmas );
	final ExpectedResultsFloat gauss3 = new ExpectedResultsFloat( targetSize, sigmas, sourceData.source, sourceData.sourceSize );
	final ConvolveFloatBlockedExample convolveBlocked = new ConvolveFloatBlockedExample( targetSize, sigmas, sourceData.source, sourceData.sourceSize );

//	@Param( { "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768", "65536", "131072", "262144", "524288", "1048576" } )
//	@Param( { "256", "512", "768", "1024", "1280", "1536", "1792", "2048", "2304", "2560", "2816", "3072", "3328", "3584", "3840", "4096" } )
//	@Param({"64", "96", "128", "160", "192", "224", "256", "288", "320", "352", "384", "416", "448", "480", "512"})
//	@Param({"256", "320", "384", "448", "512", "576", "640", "704", "768", "832", "896", "960", "1024", "1088", "1152", "1216", "1280", "1344", "1408", "1472", "1536", "1600", "1664", "1728", "1792", "1856", "1920", "1984", "2048", "2112", "2176", "2240", "2304", "2368", "2432", "2496", "2560", "2624", "2688", "2752", "2816", "2880", "2944", "3008", "3072", "3136", "3200", "3264", "3328", "3392", "3456", "3520", "3584", "3648", "3712", "3776", "3840", "3904", "3968", "4032", "4096"})
//	@Param( { "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384" } )
	public int blockSize = 2048;

//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked           64  avgt   20  15,169 ± 1,199  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked          128  avgt   20  12,592 ± 0,946  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked          256  avgt   20  12,479 ± 0,835  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked          512  avgt   20  12,269 ± 1,004  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         1024  avgt   20  11,918 ± 0,747  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         2048  avgt   20  11,072 ± 0,331  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         4096  avgt   20  12,381 ± 1,382  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         8192  avgt   20  12,732 ± 0,828  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked        16384  avgt   20  13,069 ± 0,898  ms/op

//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked           64  avgt   20  19,299 ± 1,165  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked          128  avgt   20  17,342 ± 1,196  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked          256  avgt   20  16,225 ± 0,923  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked          512  avgt   20  16,367 ± 1,258  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         1024  avgt   20  14,618 ± 1,371  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         2048  avgt   20  14,222 ± 1,511  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         4096  avgt   20  14,265 ± 1,394  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked         8192  avgt   20  15,965 ± 1,179  ms/op
//	ConvolveFloatBlockedBenchmark.benchmarkConvolveFloatBlocked        16384  avgt   20  16,350 ± 1,479  ms/op

	public ConvolveFloatBlockedBenchmark()
	{
	}

	@Benchmark
	public void benchmarkGauss3()
	{
		gauss3.compute();
	}

	@Benchmark
	public void benchmarkConvolveFloatBlocked()
	{
		convolveBlocked.compute( blockSize );
	}

	public static void main( String... args ) throws RunnerException
	{
//		for ( int i = 256; i <= 4096; i += 256 )
//			System.out.print( "\"" + i + "\", " );
//		for ( int i = 8; i <= 12; i += 1 )
//			System.out.print( "\"" + (1 << i) + "\", " );
		Options options = new OptionsBuilder().include( ConvolveFloatBlockedBenchmark.class.getSimpleName() ).build();
		new Runner( options ).run();
	}
}

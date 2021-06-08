package net.imglib2.blk.derivative;

import java.util.concurrent.TimeUnit;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
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

import static net.imglib2.blk.derivative.DerivativeExample.derivativeKernels;

@State( Scope.Benchmark )
@Warmup( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class DerivativeBenchmark
{
	final int[] order = { 0, 1, 1 };
	final double[] pixelSize = { 1, 1, 1 };

	final int[] targetSize = { 32, 32, 32 };
//	final int[] targetSize = { 128, 128, 128 };

	final Kernel1D[] kernels = derivativeKernels( pixelSize, order );
	final RandomSourceData sourceData = new RandomSourceData( targetSize, kernels );
	final ExpectedResults gauss3 = new ExpectedResults( targetSize, kernels, sourceData.source, sourceData.sourceSize );
	final DerivativeExample convolve = new DerivativeExample( targetSize, kernels, sourceData.source );

	public DerivativeBenchmark()
	{
	}

	@Benchmark
	public void benchmarkConvolution()
	{
		gauss3.compute();
	}

	@Benchmark
	public void benchmarkConvolveDoubleBlocked()
	{
		convolve.compute();
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( DerivativeBenchmark.class.getSimpleName() ).build();
		new Runner( options ).run();
	}
}

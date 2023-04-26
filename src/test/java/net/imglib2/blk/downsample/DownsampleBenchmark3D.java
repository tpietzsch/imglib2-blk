package net.imglib2.blk.downsample;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.imglib2.util.Intervals;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State( Scope.Benchmark )
@Warmup( iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class DownsampleBenchmark3D
{
	final int[] imgSize = { 256, 256, 256 };

	int[] outputSize;
	int[] inputSize;
	float[] input;
	float[] output;

	@Param( { "0", "1", "2" } )
	public int downsampleDim;

	@Setup(Level.Trial)
	public void setUp() {
		final Random random = new Random( 1 );

		final int n = imgSize.length;
		inputSize = new int[ n ];
		outputSize = new int[ n ];
		DownsamplePlayground.getSizes( imgSize, downsampleDim, inputSize, outputSize );
		System.out.println( "inputSize = " + Arrays.toString( inputSize ) );
		System.out.println( "outputSize = " + Arrays.toString( outputSize ) );
		input = new float[ ( int ) Intervals.numElements( inputSize ) ];
		for ( int i = 0; i < input.length; i++ )
			input[ i ] = random.nextFloat();
		output = new float[ ( int ) Intervals.numElements( outputSize ) ];
	}

	@Benchmark
	public void benchmarkDownsampleN()
	{
		DownsampleFloat.downsample( input, outputSize, output, downsampleDim );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( DownsampleBenchmark3D.class.getSimpleName() + "\\." ).build();
		new Runner( options ).run();
	}
}

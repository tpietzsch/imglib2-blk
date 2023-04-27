package net.imglib2.blk.downsample;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
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
public class DownsampleBenchmarkFull
{
	final int[] imgSize = { 64, 64, 64 };
//	final int[] imgSize = { 256, 256, 256 };

	int[] outputSize;
	int[] inputSize;
	float[] input;
	float[] output;
	DownsampleFloat downsampleFloat;

	@Param( { "X", "Y", "Z", "XYZ" } )
	public String scenario;


	@Setup(Level.Trial)
	public void setUp() {
		final Random random = new Random( 1 );

		final int n = imgSize.length;
		inputSize = new int[ n ];
		outputSize = new int[ n ];

		boolean[] downsampleInDim = null;
		switch ( scenario )
		{
		case "X":
			downsampleInDim = new boolean[] { true, false, false };
			break;
		case "Y":
			downsampleInDim = new boolean[] { false, true, false };
			break;
		case "Z":
			downsampleInDim = new boolean[] { false, false, true };
			break;
		case "XYZ":
			downsampleInDim = new boolean[] { true, true, true };
			break;
		}

		final long[] destSize = DownsampleFloat.getDownsampledDimensions( Util.int2long( imgSize ), downsampleInDim );
		Arrays.setAll( outputSize, d -> ( int ) destSize[ d ] );
		downsampleFloat = new DownsampleFloat( downsampleInDim );
		downsampleFloat.setTargetSize( outputSize );
		System.arraycopy( downsampleFloat.getSourceSize(), 0, inputSize, 0, inputSize.length );
		System.out.println( "inputSize = " + Arrays.toString( inputSize ) );
		System.out.println( "outputSize = " + Arrays.toString( outputSize ) );
		input = new float[ ( int ) Intervals.numElements( inputSize ) ];
		for ( int i = 0; i < input.length; i++ )
			input[ i ] = random.nextFloat();
		output = new float[ ( int ) Intervals.numElements( outputSize ) ];
	}

	@Benchmark
	public void benchmarkDownsampleFloat()
	{
		downsampleFloat.compute( input, output );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( DownsampleBenchmarkFull.class.getSimpleName() + "\\." ).build();
		new Runner( options ).run();
	}
}

package net.imglib2.blk.downsample;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.imglib2.util.Intervals;
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

@State( Scope.Benchmark )
@Warmup( iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class DownsampleBenchmark
{
	final int[] imgSize = { 256, 256 * 256 };

	final int[] outputSizeX;
	final int[] inputSizeX;
	final float[] inputX;
	final float[] outputX;

	final int[] outputSizeY;
	final int[] inputSizeY;
	final float[] inputY;
	final float[] outputY;

	public DownsampleBenchmark()
	{
		final Random random = new Random( 1 );

		final int n = imgSize.length;
		inputSizeX = new int[ n ];
		outputSizeX = new int[ n ];
		DownsamplePlayground.getSizes( imgSize, 0, inputSizeX, outputSizeX );
		inputX = new float[ ( int ) Intervals.numElements( inputSizeX ) ];
		for ( int i = 0; i < inputX.length; i++ )
			inputX[ i ] = random.nextFloat();
		outputX = new float[ ( int ) Intervals.numElements( outputSizeX ) ];

		inputSizeY = new int[ n ];
		outputSizeY = new int[ n ];
		DownsamplePlayground.getSizes( imgSize, 1, inputSizeY, outputSizeY );
		inputY = new float[ ( int ) Intervals.numElements( inputSizeY ) ];
		for ( int i = 0; i < inputY.length; i++ )
			inputY[ i ] = random.nextFloat();
		outputY = new float[ ( int ) Intervals.numElements( outputSizeY ) ];
	}

	@Benchmark
	public void benchmarkDownsampleX()
	{
		DownsampleFloat.downsampleX( inputX, outputSizeX, outputX );
	}

	@Benchmark
	public void benchmarkDownsampleY()
	{
		DownsamplePlayground.downsampleY( inputY, outputSizeY, outputY );
	}

	@Benchmark
	public void benchmarkDownsampleY2()
	{
		DownsamplePlayground.downsampleY2( inputY, outputSizeY, outputY );
	}

	@Benchmark
	public void benchmarkDownsampleY3()
	{
		DownsamplePlayground.downsampleY3( inputY, outputSizeY, outputY );
	}

	@Benchmark
	public void benchmarkDownsampleY4()
	{
		DownsamplePlayground.downsampleY4( inputY, outputSizeY, outputY );
	}

	@Benchmark
	public void benchmarkDownsampleN()
	{
		DownsampleFloat.downsample( inputY, outputSizeY, outputY, 1 );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( DownsampleBenchmark.class.getSimpleName() + "\\." ).build();
		new Runner( options ).run();
	}
}

package net.imglib2.blk.downsample;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import static net.imglib2.util.Util.roundToLong;

@State( Scope.Benchmark )
@Warmup( iterations = 10, time = 50, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 50, time = 50, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.NANOSECONDS )
@Fork( 1 )
public class RoundingBenchmark
{
	@State(Scope.Benchmark)
	public static class Value {
		public double x = 1.5;
	}

	double[] input;

	@Setup(Level.Trial)
	public void setUp() {
		final Random random = new Random( 1 );
		input = new double[ 10 ];
		for ( int i = 0; i < input.length; i++ )
			input[ i ] = random.nextDouble();
	}

	@Benchmark
	public long benchmarkMathRound()
	{
		long sum = 0;
		for ( int i = 0; i < input.length; i++ )
		{
			sum += Math.round( input[ i ] );
		}
		return sum;
	}

	@Benchmark
	public long benchmarkRoundToLong()
	{
		long sum = 0;
		for ( int i = 0; i < input.length; i++ )
		{
			sum += roundToLong( input[ i ] );
		}
		return sum;
	}

	@Benchmark
	public long benchmarkMathRoundV( Value value )
	{
		return Math.round( value.x );
	}

	@Benchmark
	public long benchmarkRoundToLongV( Value value)
	{
		return roundToLong( value.x );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( RoundingBenchmark.class.getSimpleName() + "\\." ).build();
		new Runner( options ).run();
	}
}

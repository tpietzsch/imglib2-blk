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

@State( Scope.Benchmark )
@Warmup( iterations = 10, time = 50, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 50, time = 50, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.NANOSECONDS )
@Fork( 1 )
public class MaskingBenchmark
{
	@State(Scope.Benchmark)
	public static class Value {
		public int x = 27;
	}

	int[] input;

	@Setup(Level.Trial)
	public void setUp() {
		final Random random = new Random( 1 );
		input = new int[ 10 ];
		for ( int i = 0; i < input.length; i++ )
			input[ i ] = random.nextInt( 128 );
	}

	@Benchmark
	public byte benchmarkNoMask()
	{
		byte sum = 0;
		for ( int i = 0; i < input.length; i++ )
		{
			sum += getCodedSignedByteNoMask( input[ i ] );
		}
		return sum;
	}

	@Benchmark
	public byte benchmarkMask()
	{
		byte sum = 0;
		for ( int i = 0; i < input.length; i++ )
		{
			sum += getCodedSignedByte( input[ i ] );
		}
		return sum;
	}

	static byte getCodedSignedByte( final int unsignedByte )
	{
		return ( byte ) ( unsignedByte & 0xff );
	}

	static byte getCodedSignedByteNoMask( final int unsignedByte )
	{
		return ( byte ) unsignedByte;
	}

	@Benchmark
	public byte benchmarkNoMaskV( Value value )
	{
		return getCodedSignedByteNoMask( value.x );
	}

	@Benchmark
	public byte benchmarkMaskV( Value value)
	{
		return getCodedSignedByte( value.x );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( MaskingBenchmark.class.getSimpleName() + "\\." ).build();
		new Runner( options ).run();
	}
}

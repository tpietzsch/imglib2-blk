package net.imglib2.blk.copy;

import java.util.concurrent.TimeUnit;
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
@Warmup( iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MICROSECONDS )
@Fork( 1 )
public class ConvertBenchmark
{
	private static final int SIZE = 6553600;
	private static final int LENGTH = 100 * 100 * 100;

	private final short[] uint16src;
	private final float[] src;
	private final float[] dest;

	public ConvertBenchmark()
	{
		src = new float[ SIZE ];
		dest = new float[ SIZE ];
		uint16src = new short[ SIZE ];
	}

	@Benchmark
	public void benchmark1()
	{
		copy1( src, dest, 0, 0, LENGTH );
	}

	@Benchmark
	public void benchmark2()
	{
		copy2( src, dest, LENGTH );
	}

	@Benchmark
	public void benchmarkConvert()
	{
		convert( uint16src, dest, LENGTH );
	}

	static void copy1( float[] src, float[] dest, int src_offset, int dest_offset, int length )
	{
		for ( int i = 0; i < length; i++ )
			dest[ i + dest_offset ] = src[ i + src_offset ];
	}

	static void copy2( float[] src, float[] dest, int length )
	{
		for ( int i = 0; i < length; i++ )
			dest[ i ] = src[ i ];
	}

	static void convert( short[] src, float[] dest, int length )
	{
		for ( int i = 0; i < length; i++ )
			dest[ i ] = src[ i ] & 0xffff;
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( ConvertBenchmark.class.getSimpleName() ).build();
		new Runner( options ).run();
	}
}

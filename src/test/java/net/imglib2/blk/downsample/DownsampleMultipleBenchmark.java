package net.imglib2.blk.downsample;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.imglib2.FinalInterval;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
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
@Warmup( iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class DownsampleMultipleBenchmark
{
	final long[] imgDimensions = { 256, 256, 256 };
	final int[] cellDimensions = { 64, 64, 64 };
	final boolean[] downsampleInDim = { true, true, true };

	int[] inputSize;

	PrimitiveBlocks< FloatType > blocksF32;
	Downsample.Float downsampleF32;
	float[] destF32;

	PrimitiveBlocks< UnsignedByteType > blocksU8;
	DownsampleMultiple.UnsignedByteViaFloat downSampleU8ViaFloat;
	byte[] destU8;

	@Setup(Level.Trial)
	public void setUp() {
		final Random random = new Random( 1 );

		Img< UnsignedByteType > img = new CellImgFactory<>( new UnsignedByteType(), cellDimensions ).create( imgDimensions );
		img.forEach( t -> t.set( random.nextInt( 256 ) ) );

		blocksF32 = PrimitiveBlocks.of( Converters.convert( Views.extendBorder( img ), new RealFloatConverter<>(), new FloatType() ) );
		downsampleF32 = new Downsample.Float( downsampleInDim );
		downsampleF32.setTargetInterval( new FinalInterval( Util.int2long( cellDimensions ) ) );

		blocksU8 = PrimitiveBlocks.of( Views.extendBorder( img ) );
		downSampleU8ViaFloat = new DownsampleMultiple.UnsignedByteViaFloat( downsampleInDim );
		downSampleU8ViaFloat.setTargetInterval( new FinalInterval( Util.int2long( cellDimensions ) ) );

		final int destSize = ( int ) Intervals.numElements( cellDimensions );
		destF32 = new float[ destSize ];
		destU8 = new byte[ destSize ];
	}

	@Benchmark
	public void benchmarkDownsampleF32()
	{
		downsampleF32.compute( downsampleF32.getSourceBuffer(), destF32 );
	}

	@Benchmark
	public void benchmarkDownsampleU8()
	{
		downSampleU8ViaFloat.compute( downSampleU8ViaFloat.getSourceBuffer(), destU8 );
	}

	@Benchmark
	public void benchmarkCopyF32()
	{
		blocksF32.copy( new long[] { 0, 0, 0 }, downsampleF32.getSourceBuffer(), cellDimensions );
	}

	@Benchmark
	public void benchmarkCopyU8()
	{
		blocksU8.copy( new long[] { 0, 0, 0 }, downSampleU8ViaFloat.getSourceBuffer(), cellDimensions );
	}

	@Benchmark
	public void benchmarkCopyAndDownsampleF32()
	{
		blocksF32.copy( new long[] { 0, 0, 0 }, downsampleF32.getSourceBuffer(), cellDimensions );
		downsampleF32.compute( downsampleF32.getSourceBuffer(), destF32 );
	}

	@Benchmark
	public void benchmarkCopyAndDownsampleU8()
	{
		blocksU8.copy( new long[] { 0, 0, 0 }, downSampleU8ViaFloat.getSourceBuffer(), cellDimensions );
		downSampleU8ViaFloat.compute( downSampleU8ViaFloat.getSourceBuffer(), destU8 );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( DownsampleMultipleBenchmark.class.getSimpleName() + "\\." ).build();
		new Runner( options ).run();
	}
}

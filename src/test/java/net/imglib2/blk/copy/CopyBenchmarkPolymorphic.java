package net.imglib2.blk.copy;

import java.util.concurrent.TimeUnit;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
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

import static net.imglib2.blk.copy.Extension.CONSTANT;
import static net.imglib2.blk.copy.Extension.MIRROR_SINGLE;

@State( Scope.Benchmark )
@Warmup( iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class CopyBenchmarkPolymorphic
{
	private final int[] cellDimensions = { 64, 64, 64 };
	private final int[] srcDimensions = { 600, 600, 500 };
	private final int[] destDimensions = { 100, 100, 100 };
	private final int[] pos = { 64, 100, 100 };
	private final int[] oobPos = { -64, -64, -64 };

	private final CellImg< UnsignedByteType, ? > cellImg;

	private final ArrayImg< UnsignedByteType, ? > destArrayImg;

	private final byte[] dest;

	void spoil()
	{
		// byte
		final CellImgFactory< UnsignedByteType > factoryByte = new CellImgFactory<>( new UnsignedByteType(), cellDimensions );
		final CellImg< UnsignedByteType, ? > cellImgByte = factoryByte.create( srcDimensions );
		final byte[] destByte = new byte[ ( int ) Intervals.numElements( destDimensions ) ];
		CellImgBlocks blocksByte = new CellImgBlocks( cellImgByte, CONSTANT, new UnsignedByteType( 0 ) );
		blocksByte.copy( pos, destByte, destDimensions );
		blocksByte = new CellImgBlocks( cellImgByte, MIRROR_SINGLE );
		blocksByte.copy( oobPos, destByte, destDimensions );
		blocksByte = new CellImgBlocks<>( cellImgByte, CONSTANT, new UnsignedByteType( 0 ) );
		blocksByte.copy( oobPos, destByte, destDimensions );

		// float
		final CellImgFactory< FloatType > factoryFloat = new CellImgFactory<>( new FloatType(), cellDimensions );
		final CellImg< FloatType, ? > cellImgFloat = factoryFloat.create( srcDimensions );
		final float[] destFloat = new float[ ( int ) Intervals.numElements( destDimensions ) ];
		CellImgBlocks blocksFloat = new CellImgBlocks( cellImgFloat, CONSTANT, new FloatType( 0 ) );
		blocksFloat.copy( pos, destFloat, destDimensions );
		blocksFloat = new CellImgBlocks( cellImgFloat, MIRROR_SINGLE );
		blocksFloat.copy( oobPos, destFloat, destDimensions );
		blocksFloat = new CellImgBlocks<>( cellImgFloat, CONSTANT, new FloatType( 0 ) );
		blocksFloat.copy( oobPos, destFloat, destDimensions );

		// double
		final CellImgFactory< DoubleType > factoryDouble = new CellImgFactory<>( new DoubleType(), cellDimensions );
		final CellImg< DoubleType, ? > cellImgDouble = factoryDouble.create( srcDimensions );
		final double[] destDouble = new double[ ( int ) Intervals.numElements( destDimensions ) ];
		CellImgBlocks blocksDouble = new CellImgBlocks( cellImgDouble, CONSTANT, new DoubleType( 0 ) );
		blocksDouble.copy( pos, destDouble, destDimensions );
		blocksDouble = new CellImgBlocks( cellImgDouble, MIRROR_SINGLE );
		blocksDouble.copy( oobPos, destDouble, destDimensions );
		blocksDouble = new CellImgBlocks<>( cellImgDouble, CONSTANT, new DoubleType( 0 ) );
		blocksDouble.copy( oobPos, destDouble, destDimensions );
	}

	@Param( value = { "false", "true" } )
	private boolean slowdown;

	@Setup
	public void setup()
	{
		if ( slowdown )
			spoil();
	}

	public CopyBenchmarkPolymorphic()
	{
		final CellImgFactory< UnsignedByteType > cellImgFactory = new CellImgFactory<>( new UnsignedByteType(), cellDimensions );
		cellImg = cellImgFactory.create( srcDimensions );
		destArrayImg = new ArrayImgFactory<>( new UnsignedByteType() ).create( destDimensions );
		dest = new byte[ ( int ) Intervals.numElements( destDimensions ) ];
	}

	@Benchmark
	public void benchmarkLoopBuilder()
	{
		final long[] min = Util.int2long( pos );
		final long[] max = min.clone();
		for ( int d = 0; d < max.length; d++ )
			max[ d ] += destDimensions[ d ] - 1;
		LoopBuilder
				.setImages( Views.interval( cellImg, min, max), destArrayImg )
				.multiThreaded( false )
				.forEachPixel( (i,o) -> o.set( i.get() ) );
	}

	@Benchmark
	public void benchmarkLoopBuilderOobMirrorSingle()
	{
		final long[] min = Util.int2long( oobPos );
		final long[] max = min.clone();
		for ( int d = 0; d < max.length; d++ )
			max[ d ] += destDimensions[ d ] - 1;
		LoopBuilder
				.setImages( Views.interval( Views.extendMirrorSingle( cellImg ), min, max), destArrayImg )
				.multiThreaded( false )
				.forEachPixel( (i,o) -> o.set( i.get() ) );
	}

	@Benchmark
	public void benchmarkLoopBuilderOobConstant()
	{
		final long[] min = Util.int2long( oobPos );
		final long[] max = min.clone();
		for ( int d = 0; d < max.length; d++ )
			max[ d ] += destDimensions[ d ] - 1;
		LoopBuilder
				.setImages( Views.interval( Views.extendZero( cellImg ), min, max), destArrayImg )
				.multiThreaded( false )
				.forEachPixel( (i,o) -> o.set( i.get() ) );
	}

	@Benchmark
	public void benchmarkCellImgBlocks()
	{
		final CellImgBlocks blocks = new CellImgBlocks( cellImg, CONSTANT, new UnsignedByteType( 0 ) );
		blocks.copy( pos, dest, destDimensions );
	}

	@Benchmark
	public void benchmarkCellImgBlocksOobMirrorSingle()
	{
		final CellImgBlocks blocks = new CellImgBlocks( cellImg, MIRROR_SINGLE );
		blocks.copy( oobPos, dest, destDimensions );
	}

	@Benchmark
	public void benchmarkCellImgBlocksOobConstant()
	{
		final CellImgBlocks< ? > blocks = new CellImgBlocks<>( cellImg, CONSTANT, new UnsignedByteType( 0 ) );
		blocks.copy( oobPos, dest, destDimensions );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( CopyBenchmarkPolymorphic.class.getSimpleName() ).build();
		new Runner( options ).run();
	}
}

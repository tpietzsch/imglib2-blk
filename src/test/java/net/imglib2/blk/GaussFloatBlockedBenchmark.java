package net.imglib2.blk;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blk.copy.CellImgBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.parallel.Parallelization;
import net.imglib2.parallel.TaskExecutor;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
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

import static net.imglib2.blk.copy.Extension.CONSTANT;

@State( Scope.Benchmark )
@Warmup( iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class GaussFloatBlockedBenchmark
{
	static final int CELL_SIZE = 64;

	@Param( { "true", "false" } )
	public boolean multiThreaded;


	final double[] sigmas = { 4, 4, 4 };
	final int[] targetSize = { 334, 388, 357 };

	private final CellImg< FloatType, ? > source;

	public GaussFloatBlockedBenchmark()
	{
		final RandomSourceDataFloat sourceData = new RandomSourceDataFloat( targetSize, sigmas );
		final long[] tsl = new long[ targetSize.length ];
		Arrays.setAll( tsl, i -> targetSize[ i ] );
		final ArrayImg< FloatType, ? > img = ArrayImgs.floats( sourceData.source, tsl );
		source = new CellImgFactory<>( new FloatType(), CELL_SIZE ).create( targetSize );
		LoopBuilder.setImages( img, source ).forEachPixel( ( a, b ) -> b.set( a.get() ) );

	}

	@Benchmark
	public void benchmarkGauss3()
	{
		final CachedCellImg< FloatType, ? > gauss3 = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( source ),
				new FloatType(),
				cell -> Gauss3.gauss( sigmas, Views.extendZero( source ), cell, 1 ),
				ReadOnlyCachedCellImgOptions.options().cellDimensions( CELL_SIZE ) );

		if ( multiThreaded )
			Parallelization.runMultiThreaded( () -> touchAllCells( gauss3 ) );
		else
			Parallelization.runSingleThreaded( () -> touchAllCells( gauss3 ) );
	}

	@Benchmark
	public void benchmarkGaussFloatBlocked()
	{
		final CellImgBlocks blocks = new CellImgBlocks( source, CONSTANT, new FloatType( 0 ) );
		final ThreadLocal< GaussFloatBlocked > tlgauss = ThreadLocal.withInitial( () -> new GaussFloatBlocked( sigmas ) );
		final CellLoader< FloatType > loader = new CellLoader< FloatType >()
		{
			@Override
			public void load( final SingleCellArrayImg< FloatType, ? > cell ) throws Exception
			{
				final int[] srcPos = Intervals.minAsIntArray( cell );
				final float[] dest = ( float[] ) cell.getStorageArray();

				final GaussFloatBlocked gauss = tlgauss.get();
				gauss.setTargetSize( Intervals.dimensionsAsIntArray( cell ) );

				final int[] sourceOffset = gauss.getSourceOffset();
				for ( int d = 0; d < srcPos.length; d++ )
					srcPos[ d ] += sourceOffset[ d ];
				final int[] size = gauss.getSourceSize();
				final float[] src = gauss.getSourceBuffer();;
				blocks.copy( srcPos, src, size );
				gauss.compute( src, dest );
			}
		};
		final CachedCellImg< FloatType, ? > gaussFloatBlocked = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( source ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( CELL_SIZE ) );

		if ( multiThreaded )
			Parallelization.runMultiThreaded( () -> touchAllCells( gaussFloatBlocked ) );
		else
			Parallelization.runSingleThreaded( () -> touchAllCells( gaussFloatBlocked ) );
	}

	static CachedCellImg< FloatType, ? > smoothed(
			final CellImgBlocks blocks,
			final int dim,
			final double sigma )
	{
		final ThreadLocal< GaussFloatBlocked1D > tlgauss = ThreadLocal.withInitial( () -> new GaussFloatBlocked1D( blocks.source().numDimensions(), dim, sigma ) );
		final CellLoader< FloatType > loader = new CellLoader< FloatType >()
		{
			@Override
			public void load( final SingleCellArrayImg< FloatType, ? > cell ) throws Exception
			{
				final int[] srcPos = Intervals.minAsIntArray( cell );
				final float[] dest = ( float[] ) cell.getStorageArray();

				final GaussFloatBlocked1D gauss = tlgauss.get();
				gauss.setTargetSize( Intervals.dimensionsAsIntArray( cell ) );

				final int[] sourceOffset = gauss.getSourceOffset();
				for ( int d = 0; d < srcPos.length; d++ )
					srcPos[ d ] += sourceOffset[ d ];
				final int[] size = gauss.getSourceSize();
				final float[] src = gauss.getSourceBuffer();;
				blocks.copy( srcPos, src, size );
				gauss.compute( src, dest );
			}
		};

		return new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( blocks.source() ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( CELL_SIZE ) );
	}

	@Benchmark
	public void benchmarkGaussFloatBlocked1D()
	{
		AbstractCellImg< FloatType, ?, ?, ? > smoothed = source;
		for ( int d = 0; d < source.numDimensions(); d++ )
			smoothed = smoothed( new CellImgBlocks( smoothed, CONSTANT, new FloatType( 0 ) ), d, sigmas[ d ] );
		final AbstractCellImg< FloatType, ?, ?, ? > gaussFloatBlocked1D = smoothed;

		if ( multiThreaded )
			Parallelization.runMultiThreaded( () -> touchAllCells( gaussFloatBlocked1D ) );
		else
			Parallelization.runSingleThreaded( () -> touchAllCells( gaussFloatBlocked1D ) );
	}

	private static void touchAllCells( final AbstractCellImg< ?, ?, ?, ? > img )
	{
		final IterableInterval< ? > cells = img.getCells();

		final TaskExecutor te = Parallelization.getTaskExecutor();
		final int numThreads = te.getParallelism();
		final long size = cells.size();
		final AtomicLong nextIndex = new AtomicLong();
		te.forEach( IntStream.range( 0, numThreads ).boxed().collect( Collectors.toList() ), workerIndex -> {
			final Cursor< ? > cursor = cells.cursor();
			long iCursor = -1;
			for ( long i = nextIndex.getAndIncrement(); i < size; i = nextIndex.getAndIncrement() )
			{
				cursor.jumpFwd( i - iCursor );
				cursor.get();
				iCursor = i;
			}
		} );
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( GaussFloatBlockedBenchmark.class.getSimpleName() ).build();
		new Runner( options ).run();
	}
}

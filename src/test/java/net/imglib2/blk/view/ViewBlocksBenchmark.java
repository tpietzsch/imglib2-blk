package net.imglib2.blk.view;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blk.GaussFloatBlocked;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.converter.Converters;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.parallel.Parallelization;
import net.imglib2.parallel.TaskExecutor;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.MixedTransformView;
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

@State( Scope.Benchmark )
@Warmup( iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class ViewBlocksBenchmark
{
	static final int CELL_SIZE = 64;

//	@Param( { "true", "false" } )
	@Param( { "true" } )
//	@Param( { "false" } )
	public boolean multiThreaded;

	final double[] sigmas = { 8, 8, 8 };
	final int[] sourceImgSize = { 334, 388, 357, 2 };
	final long[] outputSize;

	private final RandomAccessible< FloatType > source;

	public ViewBlocksBenchmark()
	{
		outputSize = new long[ sourceImgSize.length - 1 ];
		Arrays.setAll( outputSize, i -> sourceImgSize[ i ] );

		final CellImgFactory< UnsignedShortType > imgFactory = new CellImgFactory<>(
				new UnsignedShortType(), 256, 256, 8, 1 );
		final CellImg< UnsignedShortType, ? > cellImg = imgFactory.create( sourceImgSize );
		final Random random = new Random( 1 );
		cellImg.forEach( t -> t.set( random.nextInt() & 0xffff ) );
		final ImgPlus< UnsignedShortType > imgPlus = new ImgPlus<>( cellImg );
		final RandomAccessible< UnsignedShortType > extended = Views.extendBorder( imgPlus );
		final RandomAccessible< FloatType > converted = Converters.convert(	extended,
				( in, out ) -> out.setReal( in.getRealFloat() ),
				new FloatType() );
		final MixedTransformView< FloatType > slice = Views.hyperSlice( converted, 3, 1 );
		source = slice;
	}

	@Benchmark
	public void benchmarkGauss3()
	{
		final CachedCellImg< FloatType, ? > gauss3 = new ReadOnlyCachedCellImgFactory().create(
				outputSize,
				new FloatType(),
				cell -> Gauss3.gauss( sigmas, source, cell, 1 ),
				ReadOnlyCachedCellImgOptions.options().cellDimensions( CELL_SIZE ) );

		if ( multiThreaded )
			Parallelization.runMultiThreaded( () -> touchAllCells( gauss3 ) );
		else
			Parallelization.runSingleThreaded( () -> touchAllCells( gauss3 ) );
	}

	@Benchmark
	public void benchmarkGaussViewBlocks()
	{
		final ViewProps props = new ViewProps( source );
		final ThreadLocal< ViewBlocks > blocks = ThreadLocal.withInitial( () -> new ViewBlocks( props, new FloatType() ) );
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
				blocks.get().copy( srcPos, src, size );
				gauss.compute( src, dest );

			}
		};
		final CachedCellImg< FloatType, ? > gaussFloatBlocked = new ReadOnlyCachedCellImgFactory().create(
				outputSize,
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( CELL_SIZE ) );

		if ( multiThreaded )
			Parallelization.runMultiThreaded( () -> touchAllCells( gaussFloatBlocked ) );
		else
			Parallelization.runSingleThreaded( () -> touchAllCells( gaussFloatBlocked ) );
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
		Options options = new OptionsBuilder().include( ViewBlocksBenchmark.class.getSimpleName() ).build();
		new Runner( options ).run();
//		new ViewBlocksBenchmark().benchmarkGaussViewBlocks();
	}
}

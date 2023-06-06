package net.imglib2.algorithm;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.blocks.BlockProcessor;
import net.imglib2.algorithm.blocks.util.BlockProcessorSourceInterval;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.blocks.TempArray;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.CloseableThreadLocal;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
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
@Measurement( iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@Fork( 1 )
public class TransformBenchmark
{
	final long[] min = { 804, 110 };
	final int[] size = { 128, 128 };
	final AffineTransform2D affine = new AffineTransform2D();
	final RandomAccessibleInterval< UnsignedByteType > img;

	public TransformBenchmark()
	{
		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
		final ImagePlus imp = IJ.openImage( fn );
		img = ImageJFunctions.wrapByte( imp );
		affine.rotate( 0.1 );
		affine.scale( 1.4 );

		realviewsSetup();
		blocksnaiveSetup();
	}

	RandomAccessible< UnsignedByteType > transformed;

	public void realviewsSetup()
	{
		RealRandomAccessible< UnsignedByteType > interpolated = Views.interpolate( Views.extendZero( img ), new ClampingNLinearInterpolatorFactory<>() );
		transformed = RealViews.affine( interpolated, affine );
	}

	@Benchmark
	public Object realviews()
	{
		final RandomAccessibleInterval< UnsignedByteType > copy = copy( transformed, new UnsignedByteType(), min, size );
		return copy;
	}

	PrimitiveBlocks< FloatType > blocks;
	AffineBlockProcessor processor;

	public void blocksnaiveSetup()
	{
		blocks = PrimitiveBlocks.of(
				Converters.convert(
						Views.extendZero( img ),
						new RealFloatConverter<>(),
						new FloatType() ) );
		processor = new AffineBlockProcessor( affine.inverse() );
	}

	@Benchmark
	public Object blocksnaive()
	{
		long[] max = new long[ size.length ];
		Arrays.setAll( max, d -> min[ d ] + size[ d ] - 1 );
		processor.setTargetInterval( FinalInterval.wrap( min, max ) );
		blocks.copy( processor.getSourcePos(), processor.getSourceBuffer(), processor.getSourceSize() );
		final float[] dest = new float[ ( int ) Intervals.numElements( size ) ];
		processor.compute( processor.getSourceBuffer(), dest );
		final RandomAccessibleInterval< FloatType > destImg = ArrayImgs.floats( dest, size[ 0 ], size[ 1 ] );
		return destImg;
	}


	public static void main( String[] args ) throws RunnerException
	{
		Options options = new OptionsBuilder().include( TransformBenchmark.class.getSimpleName() + "\\." ).build();
		new Runner( options ).run();
	}


	// ------------------------------------------------------------------------


	static class AffineBlockProcessor implements BlockProcessor< float[], float[] >
	{
		private static final int n = 2;

		private final AffineTransform2D transformToSource;

		private final long[] destPos;
		private final int[] destSize;
		private final long[] sourcePos;
		private final int[] sourceSize;
		private int sourceLength;

		private final BlockProcessorSourceInterval sourceInterval;

		private final TempArray< float[] > tempArray;

		private Supplier< AffineBlockProcessor > threadSafeSupplier;

		public AffineBlockProcessor( final AffineTransform2D transformToSource )
		{
			this.transformToSource = transformToSource;
			destPos = new long[ n ];
			destSize = new int[ n ];
			sourcePos = new long[ n ];
			sourceSize = new int[ n ];
			sourceInterval = new BlockProcessorSourceInterval( this );
			tempArray = TempArray.forPrimitiveType( PrimitiveType.FLOAT );
		}

		private AffineBlockProcessor( final AffineBlockProcessor affine )
		{
			transformToSource = affine.transformToSource;
			destPos = new long[ n ];
			destSize = new int[ n ];
			sourcePos = new long[ n ];
			sourceSize = new int[ n ];
			sourceInterval = new BlockProcessorSourceInterval( this );
			tempArray = affine.tempArray.newInstance();
			threadSafeSupplier = affine.threadSafeSupplier;
		}

		private AffineBlockProcessor newInstance()
		{
			return new AffineBlockProcessor( this );
		}

		@Override
		public synchronized Supplier< AffineBlockProcessor > threadSafeSupplier()
		{
			if ( threadSafeSupplier == null )
				threadSafeSupplier = CloseableThreadLocal.withInitial( this::newInstance )::get;
			return threadSafeSupplier;
		}

		@Override
		public void setTargetInterval( final Interval interval )
		{
			destPos[ 0 ] = interval.min( 0 );
			destPos[ 1 ] = interval.min( 1 );
			destSize[ 0 ] = ( int ) interval.dimension( 0 );
			destSize[ 1 ] = ( int ) interval.dimension( 1 );

			final RealInterval bounds = transformToSource.estimateBounds( interval );
			sourcePos[ 0 ] = ( long ) Math.floor( bounds.realMin( 0 ) );
			sourcePos[ 1 ] = ( long ) Math.floor( bounds.realMin( 1 ) );
			sourceSize[ 0 ] = ( int ) ( ( long ) Math.floor( bounds.realMax( 0 ) ) - sourcePos[ 0 ] ) + 2;
			sourceSize[ 1 ] = ( int ) ( ( long ) Math.floor( bounds.realMax( 1 ) ) - sourcePos[ 1 ] ) + 2;

			sourceLength = safeInt( Intervals.numElements( sourceSize ) );
		}

		private static int safeInt( final long value )
		{
			if ( value > Integer.MAX_VALUE )
				throw new IllegalArgumentException( "value too large" );
			return ( int ) value;
		}

		@Override
		public long[] getSourcePos()
		{
			return sourcePos;
		}

		@Override
		public int[] getSourceSize()
		{
			return sourceSize;
		}

		@Override
		public Interval getSourceInterval()
		{
			return sourceInterval;
		}

		@Override
		public float[] getSourceBuffer()
		{
			return tempArray.get( sourceLength );
		}

		@Override
		public void compute( final float[] src, final float[] dest )
		{
			// straightforward implementation first ...

			double pdest[] = new double[ 2 ];
			double psrc[] = new double[ 2 ];
			for ( int y = 0; y < destSize[ 1 ]; ++y )
			{
				for ( int x = 0; x < destSize[ 0 ]; ++x )
				{
					pdest[ 0 ] = x + destPos[ 0 ];
					pdest[ 1 ] = y + destPos[ 1 ];
					transformToSource.apply( pdest, psrc );
					psrc[ 0 ] -= sourcePos[ 0 ];
					psrc[ 1 ] -= sourcePos[ 1 ];
					final int sx = ( int ) Math.floor( psrc[ 0 ] );
					final int sy = ( int ) Math.floor( psrc[ 1 ] );
					final double rx = psrc[ 0 ] - sx;
					final double ry = psrc[ 1 ] - sy;

					final int odst = y * destSize[ 0 ] + x;

					// offset (0,0)
					int osrc = sy * sourceSize[ 0 ] + sx;
					double w = ( 1.0 - rx ) * ( 1.0 - ry );
					dest[ odst ] = ( float ) ( src[ osrc ] * w );

					// offset (1,0)
					osrc = sy * sourceSize[ 0 ] + sx + 1;
					w = rx * ( 1.0 - ry );
					dest[ odst ] += ( float ) ( src[ osrc ] * w );

					// offset (0,1)
					osrc = (sy + 1 ) * sourceSize[ 0 ] + sx;
					w = ( 1.0 - rx ) * ry;
					dest[ odst ] += ( float ) ( src[ osrc ] * w );

					// offset (1,1)
					osrc = (sy + 1 ) * sourceSize[ 0 ] + sx + 1;
					w = rx * ry;
					dest[ odst ] += ( float ) ( src[ osrc ] * w );
				}
			}
		}
	}


	private static < T extends NativeType< T > > RandomAccessibleInterval< T > copy(
			final RandomAccessible< T > ra,
			final T type,
			final long[] min,
			final int[] size )
	{
		final ArrayImg< T, ? > img = new ArrayImgFactory<>( type ).create( size );
		long[] max = new long[ size.length ];
		Arrays.setAll( max, d -> min[ d ] + size[ d ] - 1 );
		final Cursor< T > cin = Views.flatIterable( Views.interval( ra, min, max ) ).cursor();
		final Cursor< T > cout = img.cursor();
		while ( cout.hasNext() )
			cout.next().set( cin.next() );
		return img;
	}
}

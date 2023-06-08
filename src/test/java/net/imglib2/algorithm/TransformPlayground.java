package net.imglib2.algorithm;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.UnifyPlayground.Affine2DProcessor;
import net.imglib2.algorithm.UnifyPlayground.Interpolation;
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

public class TransformPlayground
{


	public static void main( String[] args )
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		// -- open 2D image -----------

//		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
//		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );



		// -- show image -----------

		final BdvSource bdv = BdvFunctions.show(
				img,
				"input",
				Bdv.options().is2D() );
		bdv.setColor( new ARGBType( 0xffffff ) );
		bdv.setDisplayRange( 0, 255 );


		final AffineTransform2D affine = new AffineTransform2D();
		affine.rotate( 0.1 );
		affine.scale( 1.4 );

		final RealRandomAccessible< UnsignedByteType > interpolated = Views.interpolate( Views.extendZero( img ), new ClampingNLinearInterpolatorFactory<>() );
		final RandomAccessible< UnsignedByteType > transformed = RealViews.affine( interpolated, affine );


		final BdvSource sourceTransformed = BdvFunctions.show(
				transformed,
				img,
				"transformed",
				Bdv.options().addTo( bdv ) );
		sourceTransformed.setColor( new ARGBType( 0xffffff ) );
		sourceTransformed.setDisplayRange( 0, 255 );


		final long[] min = { 804, 110 };
		final int[] size = { 128, 128 };
		final RandomAccessibleInterval< UnsignedByteType > copy = copy( transformed, new UnsignedByteType(), min, size );
		final BdvSource sourceCopy = BdvFunctions.show(
				copy,
				"copy",
				Bdv.options().addTo( bdv ) );
		sourceCopy.setColor( new ARGBType( 0xffffff ) );
		sourceCopy.setDisplayRange( 0, 255 );


		final PrimitiveBlocks< FloatType > blocks = PrimitiveBlocks.of(
				Converters.convert(
						Views.extendZero( img ),
						new RealFloatConverter<>(),
						new FloatType() ) );
		Affine2DProcessor<float[]> processor = new Affine2DProcessor<>( affine.inverse(), Interpolation.NLINEAR, PrimitiveType.FLOAT );
		long[] max = new long[ size.length ];
		Arrays.setAll( max, d -> min[ d ] + size[ d ] - 1 );
		processor.setTargetInterval( FinalInterval.wrap( min, max ) );
		blocks.copy( processor.getSourcePos(), processor.getSourceBuffer(), processor.getSourceSize() );
		final float[] dest = new float[ ( int ) Intervals.numElements( size ) ];
		processor.compute( processor.getSourceBuffer(), dest );
		final RandomAccessibleInterval< FloatType > destImg = ArrayImgs.floats( dest, size[ 0 ], size[ 1 ] );

		final BdvSource sourceDest = BdvFunctions.show(
				destImg,
				"dest",
				Bdv.options().addTo( bdv ) );
		sourceDest.setColor( new ARGBType( 0xffffff ) );
		sourceDest.setDisplayRange( 0, 255 );
	}


	static class Affine2DBlockProcessor implements BlockProcessor< float[], float[] >
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

		private Supplier< Affine2DBlockProcessor > threadSafeSupplier;

		public Affine2DBlockProcessor( final AffineTransform2D transformToSource )
		{
			this.transformToSource = transformToSource;
			destPos = new long[ n ];
			destSize = new int[ n ];
			sourcePos = new long[ n ];
			sourceSize = new int[ n ];
			sourceInterval = new BlockProcessorSourceInterval( this );
			tempArray = TempArray.forPrimitiveType( PrimitiveType.FLOAT );
		}

		private Affine2DBlockProcessor( final Affine2DBlockProcessor affine )
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

		private Affine2DBlockProcessor newInstance()
		{
			return new Affine2DBlockProcessor( this );
		}

		@Override
		public synchronized Supplier< Affine2DBlockProcessor > threadSafeSupplier()
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
			double pdest[] = new double[ 2 ];
			double psrc[] = new double[ 2 ];
			final float dx = transformToSource.d( 0 ).getFloatPosition( 0 );
			final float dy = transformToSource.d( 0 ).getFloatPosition( 1 );
			pdest[ 0 ] = destPos[ 0 ];
			for ( int y = 0; y < destSize[ 1 ]; ++y )
			{
				pdest[ 1 ] = y + destPos[ 1 ];
				transformToSource.apply( pdest, psrc );
				float sfx = ( float ) ( psrc[ 0 ] - sourcePos[ 0 ] );
				float sfy = ( float ) ( psrc[ 1 ] - sourcePos[ 1 ] );
				for ( int x = 0; x < destSize[ 0 ]; ++x )
				{
					final int sx = ( int ) sfx;
					final int sy = ( int ) sfy;
					final float rx = sfx - sx;
					final float ry = sfy - sy;
					final int o = sy * sourceSize[ 0 ] + sx;
					final float a = src[ o ];
					final float b = src[ o + 1 ];
					final float c = src[ o + sourceSize[ 0 ] ];
					final float d = src[ o + sourceSize[ 0 ] + 1];
					dest[ y * destSize[ 0 ] + x ] = a + rx * ( b - a ) + ry * ( c - a ) + rx * ry * ( a - c - b + d );
					sfx += dx;
					sfy += dy;
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

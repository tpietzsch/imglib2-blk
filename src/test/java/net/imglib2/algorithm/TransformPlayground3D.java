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
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.CloseableThreadLocal;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class TransformPlayground3D
{


	public static void main( String[] args )
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		// -- open 2D image -----------

		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
//		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
//		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );



		// -- show image -----------

		final BdvSource bdv = BdvFunctions.show( img, "input" );
		bdv.setColor( new ARGBType( 0xffffff ) );
		bdv.setDisplayRange( 0, 255 );


		final AffineTransform3D affine = new AffineTransform3D();
		affine.rotate( 2,0.3 );
		affine.rotate( 1,0.1 );
		affine.rotate( 0,1.5 );
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


		final long[] min = { 200, -330, 120 };
		final int[] size = { 64, 64, 64 };
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
		Affine3DBlockProcessor processor = new Affine3DBlockProcessor( affine.inverse() );
		long[] max = new long[ size.length ];
		Arrays.setAll( max, d -> min[ d ] + size[ d ] - 1 );
		processor.setTargetInterval( FinalInterval.wrap( min, max ) );
		blocks.copy( processor.getSourcePos(), processor.getSourceBuffer(), processor.getSourceSize() );
		final float[] dest = new float[ ( int ) Intervals.numElements( size ) ];
		processor.compute( processor.getSourceBuffer(), dest );
		final RandomAccessibleInterval< FloatType > destImg = ArrayImgs.floats( dest, size[ 0 ], size[ 1 ], size[ 2 ] );

		final BdvSource sourceDest = BdvFunctions.show(
				destImg,
				"dest",
				Bdv.options().addTo( bdv ) );
		sourceDest.setColor( new ARGBType( 0xffffff ) );
		sourceDest.setDisplayRange( 0, 255 );


		// ----------------------------------------------
		final BdvSource bdv2 = BdvFunctions.show(
				copy,
				"copy");
		bdv2.setColor( new ARGBType( 0xffffff ) );
		bdv2.setDisplayRange( 0, 255 );
		final BdvSource sourceDest2 = BdvFunctions.show(
				destImg,
				"dest",
				Bdv.options().addTo( bdv2 ) );
		sourceDest2.setColor( new ARGBType( 0xffffff ) );
		sourceDest2.setDisplayRange( 0, 255 );
	}


	static class Affine3DBlockProcessor implements BlockProcessor< float[], float[] >
	{
		private static final int n = 3;

		private final AffineTransform3D transformToSource;

		private final long[] destPos;
		private final int[] destSize;
		private final long[] sourcePos;
		private final int[] sourceSize;
		private int sourceLength;

		private final BlockProcessorSourceInterval sourceInterval;

		private final TempArray< float[] > tempArray;

		private Supplier< Affine3DBlockProcessor > threadSafeSupplier;

		public Affine3DBlockProcessor( final AffineTransform3D transformToSource )
		{
			this.transformToSource = transformToSource;
			destPos = new long[ n ];
			destSize = new int[ n ];
			sourcePos = new long[ n ];
			sourceSize = new int[ n ];
			sourceInterval = new BlockProcessorSourceInterval( this );
			tempArray = TempArray.forPrimitiveType( PrimitiveType.FLOAT );
		}

		private Affine3DBlockProcessor( final Affine3DBlockProcessor affine )
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

		private Affine3DBlockProcessor newInstance()
		{
			return new Affine3DBlockProcessor( this );
		}

		@Override
		public synchronized Supplier< Affine3DBlockProcessor > threadSafeSupplier()
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
			destPos[ 2 ] = interval.min( 2 );
			destSize[ 0 ] = ( int ) interval.dimension( 0 );
			destSize[ 1 ] = ( int ) interval.dimension( 1 );
			destSize[ 2 ] = ( int ) interval.dimension( 2 );

			final RealInterval bounds = transformToSource.estimateBounds( interval );
			sourcePos[ 0 ] = ( long ) Math.floor( bounds.realMin( 0 ) );
			sourcePos[ 1 ] = ( long ) Math.floor( bounds.realMin( 1 ) );
			sourcePos[ 2 ] = ( long ) Math.floor( bounds.realMin( 2 ) );
			sourceSize[ 0 ] = ( int ) ( ( long ) Math.floor( bounds.realMax( 0 ) ) - sourcePos[ 0 ] ) + 2;
			sourceSize[ 1 ] = ( int ) ( ( long ) Math.floor( bounds.realMax( 1 ) ) - sourcePos[ 1 ] ) + 2;
			sourceSize[ 2 ] = ( int ) ( ( long ) Math.floor( bounds.realMax( 2 ) ) - sourcePos[ 2 ] ) + 2;

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
			double pdest[] = new double[ 3 ];
			double psrc[] = new double[ 3 ];
			final float dx = transformToSource.d( 0 ).getFloatPosition( 0 );
			final float dy = transformToSource.d( 0 ).getFloatPosition( 1 );
			final float dz = transformToSource.d( 0 ).getFloatPosition( 2 );
			final int ss0 = sourceSize[ 0 ];
			final int ss1 = sourceSize[ 1 ] * ss0;
			pdest[ 0 ] = destPos[ 0 ];
			int i = 0;
			for ( int z = 0; z < destSize[ 2 ]; ++z )
			{
				pdest[ 2 ] = z + destPos[ 2 ];
				for ( int y = 0; y < destSize[ 1 ]; ++y )
				{
					pdest[ 1 ] = y + destPos[ 1 ];
					transformToSource.apply( pdest, psrc );
					float sfx = ( float ) ( psrc[ 0 ] - sourcePos[ 0 ] );
					float sfy = ( float ) ( psrc[ 1 ] - sourcePos[ 1 ] );
					float sfz = ( float ) ( psrc[ 2 ] - sourcePos[ 2 ] );
					for ( int x = 0; x < destSize[ 0 ]; ++x )
					{
						final int sx = ( int ) sfx;
						final int sy = ( int ) sfy;
						final int sz = ( int ) sfz;
						final float r0 = sfx - sx;
						final float r1 = sfy - sy;
						final float r2 = sfz - sz;
						final int o = sz * ss1 + sy * ss0 + sx;
						final float a000 = src[ o ];
						final float a001 = src[ o + 1 ];
						final float a010 = src[ o + ss0 ];
						final float a011 = src[ o + ss0 + 1 ];
						final float a100 = src[ o + ss1 ];
						final float a101 = src[ o + ss1 + 1 ];
						final float a110 = src[ o + ss1 + ss0 ];
						final float a111 = src[ o + ss1 + ss0 + 1 ];
						dest[ i++ ] = a000 +
								r0 * ( -a000 + a001 ) +
								r1 * ( ( -a000 + a010 ) +
										r0 * ( a000 - a001 - a010 + a011 ) ) +
								r2 * ( ( -a000 + a100 ) +
										r0 * ( a000 - a001 - a100 + a101 ) +
										r1 * ( ( a000 - a010 - a100 + a110 ) +
												r0 * ( -a000 + a001 + a010 - a011 + a100 - a101 - a110 + a111 ) ) );

						sfx += dx;
						sfy += dy;
						sfz += dz;
					}
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

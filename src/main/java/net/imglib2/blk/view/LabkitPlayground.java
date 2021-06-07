package net.imglib2.blk.view;

import java.util.function.IntUnaryOperator;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class LabkitPlayground
{
	public static void main( String[] args )
	{
		final RandomAccessible< FloatType > original = createOriginalWithTime();
		final ViewProps props = new ViewProps( original );
		System.out.println( "props = " + props );
	}

	static RandomAccessible< FloatType > createOriginal()
	{
		final CellImgFactory< UnsignedShortType > imgFactory = new CellImgFactory<>(
				new UnsignedShortType(), 256, 256, 8, 1 );
		final CellImg< UnsignedShortType, ? > cellImg = imgFactory.create( 154, 146, 44, 2 );
		final ImgPlus< UnsignedShortType > imgPlus = new ImgPlus<>( cellImg );
		final RandomAccessible< UnsignedShortType > extended = Views.extendBorder( imgPlus );
		final RandomAccessible< FloatType > converted = Converters.convert(	extended,
				( in, out ) -> out.setReal( in.getRealFloat() ),
				new FloatType() );
		final MixedTransformView< FloatType > slice = Views.hyperSlice( converted, 3, 1 );
		return slice;
	}

	static RandomAccessible< FloatType > createOriginalWithTime()
	{
		final CellImgFactory< UnsignedShortType > imgFactory = new CellImgFactory<>(
				new UnsignedShortType(), 64, 64, 128, 1, 1 );
		final CellImg< UnsignedShortType, ? > cellImg = imgFactory.create( 180, 180, 90, 2, 14 );
		final ImgPlus< UnsignedShortType > imgPlus = hyperSlice( new ImgPlus<>( cellImg ), 4, 0 );
		final RandomAccessible< UnsignedShortType > extended = Views.extendBorder( imgPlus );
		final RandomAccessible< FloatType > converted = Converters.convert(	extended,
				( in, out ) -> out.setReal( in.getRealFloat() ),
				new FloatType() );
		final MixedTransformView< FloatType > slice = Views.hyperSlice( converted, 3, 1 );
		return slice;
	}

	/**
	 * Same as {@link Views#hyperSlice(RandomAccessible, int, long)}. But works
	 * on {@link ImgPlus} and manages axes information too.
	 */
	private static < T extends Type< T > > ImgPlus< T > hyperSlice( final ImgPlus< T > image, final int d, final long position )
	{
		final IntUnaryOperator axesMapping = i -> ( i < d ) ? i : i + 1;
		return newImgPlus( image, Views.hyperSlice( image.getImg(), d, position ), axesMapping );
	}

	private static < T extends Type< T > > ImgPlus< T > newImgPlus( final ImgPlus< ? > image, final RandomAccessibleInterval< T > newContent, final IntUnaryOperator axesMapping )
	{
		final T type = Util.getTypeFromInterval( newContent );
		final Img< T > newImg = net.imglib2.img.ImgView.wrap( newContent, image.factory().imgFactory( type ) );
		final ImgPlus< T > result = new ImgPlus<>( newImg, image.getName() );
		for ( int i = 0; i < result.numDimensions(); i++ )
			result.setAxis( image.axis( axesMapping.applyAsInt( i ) ).copy(), i );
		return result;
	}
}

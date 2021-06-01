package net.imglib2.blk.view;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import com.bitplane.xt.DefaultImarisService;
import com.bitplane.xt.ImarisService;
import java.util.Arrays;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.GaussFloatBlocked;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class ImarisInputTest
{
	public static void main( String[] args )
	{
		ImarisService imaris = new DefaultImarisService();
		final ImgPlus< UnsignedShortType > imgPlus = ( ImgPlus< UnsignedShortType > ) imaris.getImarisDataset().getImgPlus();
		final RandomAccessible< UnsignedShortType > extended = Views.extendBorder( imgPlus );
		final RandomAccessible< FloatType > converted = Converters.convert(	extended,
				( in, out ) -> out.setReal( in.getRealFloat() ),
				new FloatType() );
		final MixedTransformView< FloatType > slice = Views.hyperSlice( converted, 3, 1 );
		Bdv bdv = BdvFunctions.show( slice, imgPlus, "slice" );

		showRetiled( Intervals.hyperSlice( imgPlus, 3 ), slice, bdv );
		showSmoothed( Intervals.hyperSlice( imgPlus, 3 ), slice, bdv );
	}

	private static void showRetiled( final Interval interval, final MixedTransformView< FloatType > original, final Bdv bdv )
	{
		final ViewProps props = new ViewProps( original );
		final ThreadLocal< ViewBlocks > blocks = ThreadLocal.withInitial( () -> new ViewBlocks( props, new FloatType() ) );
		final CellLoader< FloatType > loader = new CellLoader< FloatType >()
		{
			@Override
			public void load( final SingleCellArrayImg< FloatType, ? > cell ) throws Exception
			{
				final int[] srcPos = Intervals.minAsIntArray( cell );
				final float[] dest = ( float[] ) cell.getStorageArray();
				final int[] size = Intervals.dimensionsAsIntArray( cell );
				blocks.get().copy( srcPos, dest, size );
			}
		};
		final CachedCellImg< FloatType, ? > retiled = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( interval ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 100, 100 ) );
		BdvFunctions.show(
				retiled,
				"retiled",
				Bdv.options().addTo( bdv ) );
	}

	private static void showSmoothed( final Interval interval, final MixedTransformView< FloatType > original, final Bdv bdv )
	{
		final double[] sigmas = { 8, 8, 8 };
		final ViewProps props = new ViewProps( original );
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
		final CachedCellImg< FloatType, ? > smoothed = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( interval ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 100, 100 ) );
		BdvFunctions.show(
				smoothed,
				String.format( "smoothed (%s)", Arrays.toString(sigmas)),
				Bdv.options().addTo( bdv ) );
	}
}

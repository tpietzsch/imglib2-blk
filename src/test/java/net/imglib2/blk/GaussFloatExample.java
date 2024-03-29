package net.imglib2.blk;

import bdv.cache.SharedQueue;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.DisplayMode;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class GaussFloatExample
{
	public static void main( String[] args )
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
//		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
//		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		final CellImg< FloatType, ? > cellImg = new CellImgFactory<>( new FloatType(), 64, 64, 64 ).create( img );
		LoopBuilder.setImages( img, cellImg ).forEachPixel( ( a, b ) -> b.set( a.get() ) );

		final BdvSource bdv = BdvFunctions.show(
				cellImg,
				"input",
				Bdv.options() );
		bdv.setColor( new ARGBType( 0xffffff ) );
		bdv.setDisplayRange( 0, 512 );
		bdv.getBdvHandle().getViewerPanel().setDisplayMode( DisplayMode.SINGLE );

//		final double[] sigmas = { 4, 4, 4 };
		final double[] sigmas = { 8, 8, 8 };
		final PrimitiveBlocks< FloatType > blocks = PrimitiveBlocks.of( Views.extendZero( cellImg ) );
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

		final CachedCellImg< FloatType, ? > smoothed = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( cellImg ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 64, 64, 64 ) );

		final CachedCellImg< FloatType, ? > gauss3 = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( cellImg ),
				new FloatType(),
				cell -> Gauss3.gauss( sigmas, Views.extendZero( cellImg ), cell, 1 ),
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 64, 64, 64 ) );

		final BdvSource smoothedSrc = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( smoothed, new SharedQueue( 1 ) ), //Runtime.getRuntime().availableProcessors() ) ),
				"smoothed",
				Bdv.options().addTo( bdv ) );

		final BdvSource gauss3Src = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( gauss3, new SharedQueue( 1 ) ), //Runtime.getRuntime().availableProcessors() ) ),
				"gauss3",
				Bdv.options().addTo( bdv ) );

		smoothedSrc.setColor( new ARGBType( 0x44ff00 ) );
		smoothedSrc.setDisplayRange( 0, 255 );
		gauss3Src.setColor( new ARGBType( 0xff4400 ) );
		gauss3Src.setDisplayRange( 0, 255 );
	}
}

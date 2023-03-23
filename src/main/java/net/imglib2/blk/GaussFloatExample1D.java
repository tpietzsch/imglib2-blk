package net.imglib2.blk;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import net.imglib2.Interval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.blk.copy.Extension;
import net.imglib2.blk.copy.NativeImgPrimitiveBlocks;
import net.imglib2.blk.copy.PrimitiveBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class GaussFloatExample1D
{
	static final int CELL_SIZE = 64;

	static CachedCellImg< FloatType, ? > smoothed(
			final Interval sourceInterval,
			final PrimitiveBlocks< FloatType > blocks,
			final int dim,
			final double sigma )
	{
		final ThreadLocal< GaussFloatBlocked1D > tlgauss = ThreadLocal.withInitial( () -> new GaussFloatBlocked1D( sourceInterval.numDimensions(), dim, sigma ) );
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
				Intervals.dimensionsAsLongArray( sourceInterval ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( CELL_SIZE ) );
	}

	public static void main( String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
//		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
//		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		final CellImg< FloatType, ? > cellImg = new CellImgFactory<>( new FloatType(), CELL_SIZE ).create( img );
		LoopBuilder.setImages( img, cellImg ).forEachPixel( ( a, b ) -> b.set( a.get() ) );

		System.out.println( "img size = " + Arrays.toString( Intervals.dimensionsAsIntArray( img ) ) );

		final BdvSource bdv = BdvFunctions.show(
				cellImg,
				"input",
				Bdv.options() );
		bdv.setColor( new ARGBType( 0xff0000 ) );
		bdv.setDisplayRange( 0, 255 );

//		final double[] sigmas = { 4, 4, 4 };
		final double[] sigmas = { 8, 8, 8 };

		AbstractCellImg< FloatType, ?, ?, ? > smoothed = cellImg;
		for ( int d = 0; d < cellImg.numDimensions(); ++d )
			smoothed = smoothed(
					cellImg,
					new NativeImgPrimitiveBlocks<>( smoothed, Extension.constant( new FloatType( 0 ) ) ),
					d,
					sigmas[ d ] );

		final CachedCellImg< FloatType, ? > gauss3 = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( cellImg ),
				new FloatType(),
				cell -> Gauss3.gauss( sigmas, Views.extendZero( cellImg ), cell, 1 ),
				ReadOnlyCachedCellImgOptions.options().cellDimensions( CELL_SIZE ) );

		final BdvSource smoothedSrc = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( smoothed, new SharedQueue( 1 ) ), //Runtime.getRuntime().availableProcessors() ) ),
				"smoothed",
				Bdv.options().addTo( bdv ) );

		final BdvSource gauss3Src = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( gauss3, new SharedQueue( 1 ) ), //Runtime.getRuntime().availableProcessors() ) ),
				"gauss3",
				Bdv.options().addTo( bdv ) );

		smoothedSrc.setColor( new ARGBType( 0x00ff00 ) );
		smoothedSrc.setDisplayRange( 0, 255 );
		gauss3Src.setColor( new ARGBType( 0x0000ff ) );
		gauss3Src.setDisplayRange( 0, 255 );
	}
}

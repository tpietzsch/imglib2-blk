package net.imglib2.blk.copy;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

public class ReTilingFloatExample
{
	public static void main( String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
//		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		final CellImg< FloatType, ? > cellImg = new CellImgFactory<>( new FloatType(), 64, 64 ).create( img );
		LoopBuilder.setImages( img, cellImg ).forEachPixel( ( a, b ) -> b.set( a.get() ) );

		Bdv bdv = null;
		bdv = BdvFunctions.show(
				cellImg,
				"input",
				Bdv.options().is2D().addTo( bdv ) );

		final PrimitiveBlocks< FloatType > blocks = new NativeImgPrimitiveBlocks<>( cellImg, Extension.constant( new FloatType( 0 ) ) );
		final CellLoader< FloatType > loader = new CellLoader< FloatType >()
		{
			@Override
			public void load( final SingleCellArrayImg< FloatType, ? > cell ) throws Exception
			{
				final int[] srcPos = Intervals.minAsIntArray( cell );
				final float[] dest = ( float[] ) cell.getStorageArray();
				final int[] size = Intervals.dimensionsAsIntArray( cell );
				blocks.copy( srcPos, dest, size );
			}
		};

		final CachedCellImg< FloatType, ? > retiled = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( cellImg ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 100, 100 ) );

		BdvFunctions.show(
				retiled,
				"output",
				Bdv.options().is2D().addTo( bdv ) );
	}
}

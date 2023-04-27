package net.imglib2.blk.downsample;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.blk.downsample.algo.AlgoUtils;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class DownsamplePlaygroundFull
{


	public static void main( String[] args )
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		final BdvSource bdv = BdvFunctions.show(
				img,
				"img",
				Bdv.options() );
		bdv.setColor( new ARGBType( 0xffffff ) );
		bdv.setDisplayRange( 0, 255 );

		final PrimitiveBlocks< FloatType > blocks = PrimitiveBlocks.of(
				Converters.convert( Views.extendBorder( img ), new RealFloatConverter<>(), new FloatType() )
		).threadSafe();

		final boolean[] downsampleInDim = { true, true, true };
		final CellLoader< FloatType > loader = AlgoUtils.cellLoader( blocks, new DownsampleFloat( downsampleInDim ) );

		final long[] downsampledDimensions = DownsampleFloat.getDownsampledDimensions( img.dimensionsAsLongArray(), downsampleInDim );
		System.out.println( "downsampledDimensions = " + Arrays.toString( downsampledDimensions ) );
		final CachedCellImg< FloatType, ? > downsampled = new ReadOnlyCachedCellImgFactory().create(
				downsampledDimensions,
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 64, 64, 64 ) );

		final double[] calib = new double[ 3 ];
		Arrays.setAll(calib, d -> downsampleInDim[ d ] ? 2 : 1 );
		final BdvSource out = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( downsampled ),
				"downsampled",
				Bdv.options()
						.addTo( bdv )
						.sourceTransform( calib ) );
		out.setDisplayRange( 0, 255 );
	}
}

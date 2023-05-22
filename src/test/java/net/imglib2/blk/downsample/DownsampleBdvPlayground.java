package net.imglib2.blk.downsample;

import bdv.export.DownsampleBlock;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.DisplayMode;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import net.imglib2.RandomAccess;
import net.imglib2.blk.downsample.Downsample.ComputationType;
import net.imglib2.blk.downsample.Downsample.Offset;
import net.imglib2.blk.downsample.algo.AlgoUtils;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import static net.imglib2.blk.downsample.Downsample.downsample;

public class DownsampleBdvPlayground
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
		bdv.getBdvHandle().getViewerPanel().setDisplayMode( DisplayMode.SINGLE );

		final boolean[] downsampleInDim = { true, true, true };
		final long[] downsampledDimensions = Downsample.getDownsampledDimensions( img.dimensionsAsLongArray(), downsampleInDim );
		final int[] cellDimensions = { 64, 64, 64 };

		final ExtendedRandomAccessibleInterval< UnsignedByteType, Img< UnsignedByteType > > extended = Views.extendBorder( img );
		final PrimitiveBlocks< UnsignedByteType > blocks = PrimitiveBlocks.of( extended );
		final UnsignedByteType type = new UnsignedByteType();

		final double[] calib = new double[ 3 ];
		Arrays.setAll(calib, d -> downsampleInDim[ d ] ? 2 : 1 );



		int[] downsamplingFactors = new int[ 3 ];
		Arrays.setAll(downsamplingFactors, d -> downsampleInDim[ d ] ? 2 : 1 );
		final DownsampleBlock< UnsignedByteType > downsampleBlock = DownsampleBlock.create( cellDimensions, downsamplingFactors, UnsignedByteType.class, RandomAccess.class );
		final RandomAccess< UnsignedByteType > in = extended.randomAccess();
		final long[] currentCellMin = new long[ 3 ];
		final int[] currentCellDim = new int[ 3 ];
		CellLoader< UnsignedByteType > downsampleBlockLoader = cell -> {
			Arrays.setAll( currentCellMin, d -> cell.min( d ) * downsamplingFactors[ d ] );
			Arrays.setAll( currentCellDim, d -> ( int ) cell.dimension( d ) );
			in.setPosition( currentCellMin );
			downsampleBlock.downsampleBlock( in, cell.cursor(), currentCellDim );
		};
		final CachedCellImg< UnsignedByteType, ? > downsampled = new ReadOnlyCachedCellImgFactory().create(
				downsampledDimensions,
				type,
				downsampleBlockLoader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions) );



		final BdvSource out = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( downsampled ),
				"downsampled bdv",
				Bdv.options()
						.addTo( bdv )
						.sourceTransform( calib ) );
		out.setDisplayRange( 0, 255 );
		out.setColor( new ARGBType( 0xff0000 ) );



		final CachedCellImg< UnsignedByteType, ? > downsampled2 = AlgoUtils.cellImg(
				blocks,
				downsample( type, ComputationType.AUTO, Offset.HALF_PIXEL, downsampleInDim ),
				type,
				downsampledDimensions,
				cellDimensions );
		final BdvSource out2 = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( downsampled2 ),
				"downsampled half-pixel",
				Bdv.options()
						.addTo( bdv )
						.sourceTransform( calib ) );
		out2.setDisplayRange( 0, 255 );
		out2.setColor( new ARGBType( 0x00ff00 ) );
	}
}

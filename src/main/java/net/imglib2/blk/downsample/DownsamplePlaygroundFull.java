package net.imglib2.blk.downsample;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvSource;
import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
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
		final ThreadLocal< DownsampleFloat > tlDownsample = ThreadLocal.withInitial(
				() -> new DownsampleFloat( downsampleInDim ) );
		final CellLoader< FloatType > loader = new CellLoader< FloatType >()
		{
			@Override
			public void load( final SingleCellArrayImg< FloatType, ? > cell ) throws Exception
			{
				final DownsampleFloat downsample = tlDownsample.get();
				final int[] destSize = Intervals.dimensionsAsIntArray( cell );
				downsample.setTargetSize( destSize );
				final int[] srcPos = Intervals.minAsIntArray( cell );
				final int[] sourceOffset = downsample.getSourceOffset();
				for ( int d = 0; d < srcPos.length; d++ )
				{
					if ( downsampleInDim[ d ] )
						srcPos[ d ] *= 2;
					srcPos[ d ] += sourceOffset[ d ];
				}
				final int[] size = downsample.getSourceSize();
				synchronized ( DownsamplePlaygroundFull.class )
				{
					System.out.println( "destSize       = " + Arrays.toString( destSize ) );
					System.out.println( "  sourceOffset = " + Arrays.toString( sourceOffset ) );
					System.out.println( "  size         = " + Arrays.toString( size ) );
				}
				final float[] src = downsample.getSourceBuffer();
				final float[] dest = ( float[] ) cell.getStorageArray();
				blocks.copy( srcPos, src, size );
				float[] copy = src.clone();
				final ArrayImg< FloatType, FloatArray > img1 = ArrayImgs.floats( copy, Util.int2long( size ) );
				downsample.compute( src, dest );
				float[] copyd = dest.clone();
				final ArrayImg< FloatType, FloatArray > img2 = ArrayImgs.floats( copyd, Util.int2long( destSize ) );
				SwingUtilities.invokeLater( () -> {
					BdvSource out1 = BdvFunctions.show( img1, "s", BdvOptions.options().addTo( bdv ) );
					out1.setDisplayRange( 0, 255 );
					BdvSource out2 = BdvFunctions.show( img2, "d", BdvOptions.options().addTo( bdv ) );
					out2.setDisplayRange( 0, 255 );
				} );
			}
		};

		final long[] downsampledDimensions = DownsampleFloat.getDownsampledDimensions( img.dimensionsAsLongArray(), downsampleInDim );
		System.out.println( "downsampledDimensions = " + Arrays.toString( downsampledDimensions ) );
		final CachedCellImg< FloatType, ? > downsampled = new ReadOnlyCachedCellImgFactory().create(
				downsampledDimensions,
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 256, 256, 256 ) );

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

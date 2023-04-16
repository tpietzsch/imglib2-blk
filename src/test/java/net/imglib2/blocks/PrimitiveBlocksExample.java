package net.imglib2.blocks;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import ij.IJ;
import ij.ImagePlus;
import java.nio.file.Paths;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * How to use {@code PrimitiveBlocks} to copy from ImgLib2 {@code
 * RandomAccessible} into a flat primitive array.
 */
public class PrimitiveBlocksExample
{
	public static void main( String[] args )
	{
		// load 3D input image
		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );
		BdvFunctions.show( img, Paths.get( fn ).getFileName().toString() );

		// do a few transformations, extend, and convert to FloatType
		RandomAccessibleInterval< UnsignedByteType > img1 = Views.rotate( img, 1, 0 );
		RandomAccessibleInterval< UnsignedByteType > img2 = Views.zeroMin( img1 );
		RandomAccessibleInterval< UnsignedByteType > img3 = Views.hyperSlice( img2, 2, 80 );
		RandomAccessible< UnsignedByteType > img4 = Views.extendBorder( img3 );
		RandomAccessible< FloatType > img5 = Converters.convert( img4, new RealFloatConverter<>(), new FloatType() );

		// show the resulting RandomAccessible<FloatType>
		final BdvSource viewSource = BdvFunctions.show( img5, img3, "view", Bdv.options().is2D() );
		Bdv bdv = viewSource;

		// Get PrimitiveBlocks of the view
		final PrimitiveBlocks< FloatType > blocks = PrimitiveBlocks.of( img5 ).threadSafe();

		// PrimitiveBlocks has one method
		//      void copy( int[] srcPos, Object dest, int[] size );
		// to copy a block out of the source of the PrimitiveBlocks into a primitive array.
		// dest is the array to copy into. In this example, we have
		// PrimitiveBlocks<FloatType>, so dest must be a float[] array.
		// srcPos and size

		// .threadSafe() means we

		final CellLoader< FloatType > loader = cell -> {
			final int[] srcPos = Intervals.minAsIntArray( cell );
			final float[] dest = ( float[] ) cell.getStorageArray();
			final int[] size = Intervals.dimensionsAsIntArray( cell );
			blocks.copy( srcPos, dest, size );
		};

		final CachedCellImg< FloatType, ? > retiled = new ReadOnlyCachedCellImgFactory().create(
				img3.dimensionsAsLongArray(),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 64, 64 ) );

		BdvSource retiledSource = BdvFunctions.show(
				retiled,
				"retiled",
				Bdv.options().is2D().addTo( bdv ) );

		viewSource.setDisplayRangeBounds( 0, 255 );
		retiledSource.setDisplayRangeBounds( 0, 255 );
	}
}

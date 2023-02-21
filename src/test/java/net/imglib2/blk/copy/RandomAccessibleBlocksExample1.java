package net.imglib2.blk.copy;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.blk.copy.ViewBlocksPlayground.ViewProperties;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class RandomAccessibleBlocksExample1
{
	public static void main( String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		RandomAccessibleInterval< UnsignedByteType > img1 = Views.hyperSlice( img, 2, 80 );
		RandomAccessible< UnsignedByteType > img2 = Views.extendMirrorSingle( img1 );
		RandomAccessible< UnsignedByteType > img3 = Views.translate( img2, 200, 150 );

		final FinalInterval interval = Intervals.hyperSlice( img, 2 );
		RandomAccessibleInterval< UnsignedByteType > img4 = Views.interval( img3, interval );

//		Bdv bdv = BdvFunctions.show( img, "img" );
		Bdv bdv = BdvFunctions.show( img4, "view", Bdv.options().is2D() );


		final ArrayImg< UnsignedByteType, ByteArray > target = ArrayImgs.unsignedBytes( interval.dimensionsAsLongArray() );
		final byte[] dest = target.update( null ).getCurrentStorageArray();
		final int[] srcPos = Util.long2int( interval.minAsLongArray() );
		final int[] size = Util.long2int( interval.dimensionsAsLongArray() );

		final ViewProperties< ?, ? > props = ViewBlocksPlayground.properties( img4 );
		final PrimitiveBlocks< ? > blocks = new RandomAccessibleBlocks<>( props );
		blocks.copy( srcPos, dest, size );

		BdvFunctions.show( target, "copied blocks", Bdv.options().addTo( bdv ) );


		System.out.println("done");
	}
}

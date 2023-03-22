package net.imglib2.blk.copy;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class RandomAccessibleBlocksExample2
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


//		final ArrayImg< UnsignedByteType, ByteArray > target = ArrayImgs.unsignedBytes( interval.dimensionsAsLongArray() );
//		final byte[] dest = target.update( null ).getCurrentStorageArray();
//		final int[] srcPos = Util.long2int( interval.minAsLongArray() );
//		final int[] size = Util.long2int( interval.dimensionsAsLongArray() );

		final ViewProperties< ?, ? > props = ViewAnalyzer.getViewProperties( img4 ).getViewProperties();
		final PrimitiveBlocks< ? > blocks = new ViewPrimitiveBlocks<>( props ).threadSafe();
//		blocks.copy( srcPos, dest, size );

		final CellLoader< UnsignedByteType > loader = cell -> {
			final int[] srcPos = Intervals.minAsIntArray( cell );
			final byte[] dest = ( byte[] ) cell.getStorageArray();
			final int[] size = Intervals.dimensionsAsIntArray( cell );
			blocks.copy( srcPos, dest, size );
		};

		final CachedCellImg< UnsignedByteType, ? > retiled = new ReadOnlyCachedCellImgFactory().create(
				img4.dimensionsAsLongArray(),
				new UnsignedByteType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 64, 64 ) );

		BdvFunctions.show(
				retiled,
				"output",
				Bdv.options().is2D().addTo( bdv ) );
	}
}

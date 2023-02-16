package net.imglib2.blk.copy;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

public class CellImgBlocksExample
{
	public static void main( String[] args )
	{
//		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		final CellImg< UnsignedByteType, ? > cellImg = new CellImgFactory<>( new UnsignedByteType(), 64, 64 ).create( img );
		LoopBuilder.setImages( img, cellImg ).forEachPixel( ( a, b ) -> b.set( a ) );

		BdvFunctions.show(
				Views.extendMirrorSingle( cellImg ),
				cellImg,
				"input",
				Bdv.options().is2D() );

//		System.out.println( "Intervals.toString(cellImg) = " + Intervals.toString( cellImg ) );
		final CellGrid cellGrid = cellImg.getCellGrid();
//		System.out.println( "cellGrid = " + cellGrid );

//		final int ox = -300;
//		final int oy = -300;
//		final int bw = 1000;
//		final int bh = 1000;

		final int ox = 54;
		final int oy = 54;
		final int bw = 84;
		final int bh = 84;

		Bdv bdv = null;
		Extension[] extensions = {
				Extension.constant( new UnsignedByteType( 128 ) ),
				Extension.border(),
				Extension.mirrorSingle(),
				Extension.mirrorDouble()
				// TODO: use non-specialized OutOfBoundsFactory
		};
		for ( Extension extension : extensions )
		{
			final byte[] data = new byte[ bw * bh ];
			final Img< UnsignedByteType > output = ArrayImgs.unsignedBytes( data, bw, bh );

			final CellImgBlocks< ? > blocks = new CellImgBlocks<>( cellImg, extension );
			blocks.copy( new int[] { ox, oy }, data, new int[] { bw, bh } );

			bdv = BdvFunctions.show(
					output,
					"output",
					Bdv.options().is2D().addTo( bdv ) );
		}
	}
}

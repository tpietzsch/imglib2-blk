package net.imglib2.blk.copy;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

public class PlanarImgBlocksExample
{
	public static void main( String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/e002_stack_fused-8bit.tif";
//		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
//		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		BdvFunctions.show(
				Views.extendMirrorSingle( img ),
				img,
				"input",
				Bdv.options() );

		final int ox = -100;
		final int oy = -100;
		final int oz = -30;
		final int bw = 300;
		final int bh = 300;
		final int bd = 300;

		Bdv bdv = null;
		for ( Extension method : Extension.values() )
		{
			final byte[] data = new byte[ bw * bh * bd ];
			final Img< UnsignedByteType > output = ArrayImgs.unsignedBytes( data, bw, bh, bd );

			final PlanarImgBlocks< ? > blocks = new PlanarImgBlocks<>( ( PlanarImg ) img, method, new UnsignedByteType( 128 ) );
			blocks.copy( new int[] { ox, oy, oz }, data, new int[] { bw, bh, bd } );

			bdv = BdvFunctions.show(
					output,
					"output",
					Bdv.options().addTo( bdv ) );
		}
	}
}

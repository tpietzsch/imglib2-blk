package net.imglib2.blk.downsample;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import static net.imglib2.blk.downsample.DownsamplePlayground.getSizes;

public class DownsamplePlayground3D
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

		final PrimitiveBlocks< FloatType > blocks = PrimitiveBlocks.of( Converters.convert( Views.extendBorder( img ), new RealFloatConverter<>(), new FloatType() ) );


		final int[] imgSize = Util.long2int( img.dimensionsAsLongArray() );
		final int n = imgSize.length;
		final int[] outputSize = new int[ n ];
		final int[] inputSize = new int[ n ];

		final int downsampleDim = 1;
		getSizes( imgSize, downsampleDim, inputSize, outputSize );

		System.out.println( "outputSize = " + Arrays.toString( outputSize ) );
		System.out.println( "inputSize = " + Arrays.toString( inputSize ) );

		// TODO: when we have an output pos, this needs to be outputPos + ...
		final int[] srcPos = new int[ n ];
		Arrays.setAll(srcPos, d -> d == downsampleDim ? -1 : 0 );

		final float input[] = new float[ ( int ) Intervals.numElements( inputSize ) ];
		blocks.copy( srcPos, input, inputSize );

		final float output[] = new float[ ( int ) Intervals.numElements( outputSize ) ];
		DownsampleFloat.downsample( input, outputSize, output, downsampleDim );

		final Img< FloatType > outputImg = ArrayImgs.floats( output, Util.int2long( outputSize ) );
		final double[] calib = new double[ n ];
		Arrays.setAll(calib, d -> d == downsampleDim ? 2 : 1 );
		final BdvSource out = BdvFunctions.show( outputImg, "output", Bdv.options()
				.addTo( bdv )
				.sourceTransform( calib ) );
		out.setDisplayRange( 0, 255 );
	}
}

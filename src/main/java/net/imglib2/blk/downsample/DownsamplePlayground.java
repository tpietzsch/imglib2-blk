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

import static net.imglib2.blk.downsample.DownsampleFloat.downsample;

public class DownsamplePlayground
{


	public static void main( String[] args )
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );
		img.getAt( 50, 50 ).set( 0 );

		final BdvSource bdv = BdvFunctions.show(
				img,
				"input",
				Bdv.options().is2D() );
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
		downsample( input, outputSize, output, 1 );

		final Img< FloatType > outputImg = ArrayImgs.floats( output, Util.int2long( outputSize ) );
		final BdvSource out = BdvFunctions.show( outputImg, "output", Bdv.options()
				.addTo( bdv )
				.sourceTransform( 1, 2 ) );
		out.setDisplayRange( 0, 255 );
	}

	static void getSizes( final int[] imgSize, final int downsampleDim, final int[] inputSize, final int[] outputSize )
	{
		Arrays.setAll( outputSize, d -> d == downsampleDim ? ( imgSize[ d ] + 1 ) / 2 : imgSize[ d ] );
		Arrays.setAll( inputSize, d -> d == downsampleDim ? outputSize[ d ] * 2 + 1 : outputSize[ d ] );
	}

	static void downsampleY4( final float[] input, final int[] outputSize, final float[] output )
	{
		final int sx = outputSize[ 0 ];
		final int sy = outputSize[ 1 ];
		final int sdx = sx;
		for ( int y = 0; y < sy; ++y )
		{
			final int so = 2 * y * sx;
			for ( int x = 0; x < sx; ++x )
			{
				output[ y * sx + x ] =
						0.25f * input[ so + x ] +
								0.5f * input[ so + x + sdx ] +
								0.25f * input[ so + x + 2 * sdx ];
			}
		}
	}

	static void downsampleY( final float[] input, final int[] outputSize, final float[] output )
	{
		final int sx = outputSize[ 0 ];
		final int sy = outputSize[ 1 ];
		for ( int y = 0; y < sy; ++y )
		{
			for ( int x = 0; x < sx; ++x )
			{
				output[ y * sx + x ] =
						0.25f * input[ 2 * y * sx + x ] +
								0.5f * input[ 2 * y * sx + x + sx ] +
								0.25f * input[ 2 * y * sx + x + 2 * sx ];
			}
		}
	}

	static void downsampleY2( final float[] input, final int[] outputSize, final float[] output )
	{
		final int sx = outputSize[ 0 ];
		final int tsy = outputSize[ 1 ];
		final int sy = tsy * 2;
		for ( int y = 0; y < sy; ++y )
		{
			if ( ( y & 0x01 ) != 0 )
			{
				final int ty = y >> 1;
				for ( int x = 0; x < sx; ++x )
					output[ ty * sx + x ] += 0.5f * input[ y * sx + x ];
			}
			else
			{
				final int ty = y >> 1;
				if ( y > 0 )
				{
					// * 0.25f to line ty - 1
					for ( int x = 0; x < sx; ++x )
						output[ ( ty - 1 ) * sx + x ] += 0.25f * input[ y * sx + x ];
				}
				if ( y < sy - 1 )
				{
					// * 0.25f to line ty
					for ( int x = 0; x < sx; ++x )
						output[ ty * sx + x ] += 0.25f * input[ y * sx + x ];
				}
			}
		}
	}

	private static void line( final float f, final float[] source, final int sourceOffset, final int length, final float[] dest, final int destOffset )
	{
		for ( int x = 0; x < length; ++x )
			dest[ destOffset + x ] += f * source[ sourceOffset + x ];
	}

	static void downsampleY3( final float[] input, final int[] outputSize, final float[] output )
	{
		final int sx = outputSize[ 0 ];
		final int tsy = outputSize[ 1 ];
		final int sy = tsy * 2;
		for ( int y = 0; y < sy; ++y )
		{
			if ( ( y & 0x01 ) != 0 )
			{
				line( 0.5f, input, y * sx, sx, output, y / 2 * sx );
			}
			else
			{
				if ( y > 0 )
				{
					line( 0.25f, input, y * sx, sx, output, ( y / 2 - 1 ) * sx );
				}
				if ( y < sy - 1 )
				{
					line( 0.25f, input, y * sx, sx, output, y / 2 * sx );
				}
			}
		}
	}

}

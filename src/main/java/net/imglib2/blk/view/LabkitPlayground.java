package net.imglib2.blk.view;

import java.util.Arrays;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.copy.CellImgBlocks;
import net.imglib2.blk.copy.MemCopy;
import net.imglib2.blk.copy.RangeCopier;
import net.imglib2.blk.copy.Ranges;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.transform.integer.Mixed;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.TransformBuilder;
import net.imglib2.view.Views;

public class LabkitPlayground
{
	public static void main( String[] args )
	{
		final RandomAccessible< FloatType > original = createOriginal();
		final ViewProps props = new ViewProps( original );
		System.out.println( "props = " + props );
	}

	static RandomAccessible< FloatType > createOriginal()
	{
		final CellImgFactory< UnsignedShortType > imgFactory = new CellImgFactory<>(
				new UnsignedShortType(), 256, 256, 8, 1 );
		final CellImg< UnsignedShortType, ? > cellImg = imgFactory.create( 154, 146, 44, 2 );
		final ImgPlus< UnsignedShortType > imgPlus = new ImgPlus<>( cellImg );
		final RandomAccessible< UnsignedShortType > extended = Views.extendBorder( imgPlus );
		final RandomAccessible< FloatType > converted = Converters.convert(	extended,
				( in, out ) -> out.setReal( in.getRealFloat() ),
				new FloatType() );
		final MixedTransformView< FloatType > slice = Views.hyperSlice( converted, 3, 1 );
		return slice;
	}

}

package net.imglib2.blk.view;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import com.bitplane.xt.DefaultImarisService;
import com.bitplane.xt.ImarisService;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.copy.CellImgBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

import static net.imglib2.blk.copy.Extension.CONSTANT;

public class ImarisInputTest
{
	public static void main( String[] args )
	{
		ImarisService imaris = new DefaultImarisService();
		final ImgPlus< UnsignedShortType > imgPlus = ( ImgPlus< UnsignedShortType > ) imaris.getImarisDataset().getImgPlus();
		final RandomAccessible< UnsignedShortType > extended = Views.extendBorder( imgPlus );
		final RandomAccessible< FloatType > converted = Converters.convert(	extended,
				( in, out ) -> out.setReal( in.getRealFloat() ),
				new FloatType() );
		final MixedTransformView< FloatType > slice = Views.hyperSlice( converted, 3, 1 );
		BdvFunctions.show( slice, imgPlus, "slice" );

		final ViewProps props = new ViewProps( slice );
		final ThreadLocal< ViewBlocks > blocks = ThreadLocal.withInitial( () -> new ViewBlocks( props, new FloatType() ) );
		final CellLoader< FloatType > loader = new CellLoader< FloatType >()
		{
			@Override
			public void load( final SingleCellArrayImg< FloatType, ? > cell ) throws Exception
			{
				final int[] srcPos = Intervals.minAsIntArray( cell );
				final float[] dest = ( float[] ) cell.getStorageArray();
				final int[] size = Intervals.dimensionsAsIntArray( cell );
				blocks.get().copy( srcPos, dest, size );
			}
		};

		final CachedCellImg< FloatType, ? > retiled = new ReadOnlyCachedCellImgFactory().create(
				Intervals.dimensionsAsLongArray( Intervals.hyperSlice( imgPlus, 3 ) ),
				new FloatType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( 100, 100 ) );

		BdvFunctions.show(
				retiled,
				"output" );
	}
}

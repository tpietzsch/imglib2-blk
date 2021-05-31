package net.imglib2.blk.view;

import java.util.Arrays;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.copy.CellImgBlocks;
import net.imglib2.blk.copy.Extension;
import net.imglib2.converter.Converters;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.transform.integer.Mixed;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class LabkitPlayground
{
	public static void main( String[] args )
	{
		final RandomAccessible< FloatType > original = createOriginal();
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

		analyze( slice, cellImg, Extension.BORDER );

		return slice;
	}

	private static < T extends NativeType< T > > void analyze(
			final RandomAccessible< FloatType > original,
			final AbstractCellImg< T, ?, ? extends Cell< ? >, ? > cellImg, // TEMP, until we figure it out ourselves
			final Extension extension // TEMP, until we figure it out ourselves
			)
	{
		if ( original instanceof MixedTransformView )
		{
			final MixedTransformView< FloatType > mixed = ( MixedTransformView< FloatType > ) original;
			final MixedTransform transform = mixed.getTransformToSource();
			checkTransform( transform );
			final TransformedCopy blocks = new TransformedCopy( new CellImgBlocks<>( cellImg, extension ), transform );
		}
		else if ( original instanceof ConvertedRandomAccessible )
		{
			System.out.println( "TODO" );
		}
		else if ( original instanceof ConvertedRandomAccessibleInterval )
		{
			System.out.println( "TODO" );
		}
		else if ( original instanceof ExtendedRandomAccessibleInterval )
		{
			System.out.println( "TODO" );
		}
		else if ( original instanceof ExtendedRandomAccessibleInterval )
		{
			System.out.println( "TODO" );
		}
		else if ( original instanceof ImgPlus )
		{
			System.out.println( "TODO" );
		}
		else if ( original instanceof AbstractCellImg )
		{
			System.out.println( "TODO" );
		}
	}


	static class TransformedCopy
	{
		private final CellImgBlocks< ? > blocks;

		private final Mixed transform;

		public TransformedCopy( CellImgBlocks<?> blocks, Mixed transformToSource )
		{
			this.blocks = blocks;
			this.transform = transformToSource;
		}

		public void copy( final int[] srcPos, final Object dest, final int[] size )
		{
			final int n = transform.numSourceDimensions();
			final int m = transform.numTargetDimensions();
			assert srcPos.length == n;
			final int[] tSrcPos = new int[ m ];
			final int[] tSize = new int[ m ];

			transform.apply( srcPos, tSrcPos );
			transformSize( size, tSize );
			blocks.copy( tSrcPos, dest, tSize );
		}

		private void transformSize( final int[] size, final int[] transformedSize )
		{
			Arrays.setAll( transformedSize,
					d -> transform.getComponentZero( d ) ? 1 : size[ transform.getComponentMapping( d ) ] );
		}
	}

	private static boolean checkTransform( final Mixed t )
	{
		// We allow translation and slicing, but not axis permutation or inverting.
		final int n = t.numSourceDimensions();
		final int m = t.numTargetDimensions();
		if ( n > m )
			return false;
		for ( int d = 0; d < m; ++d )
			if ( t.getComponentInversion( d ) )
				return false;
		int sourceComponent = -1;
		for ( int d = 0; d < m; ++d )
		{
			if ( !t.getComponentZero( d ) )
			{
				final int s = t.getComponentMapping( d );
				if ( s <= sourceComponent )
					return false;
				sourceComponent = s;
			}
		}
		return true;
	}
}

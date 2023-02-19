package net.imglib2.blk.copy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.NativeImg;
import net.imglib2.img.WrappedImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.transform.integer.BoundingBox;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class TransformPlayground
{
	/**
	 * Computes the inverse of (@code transform}. The {@code MixedTransform
	 * transform} is a pure axis permutation followed by inversion of some axes,
	 * that is
	 * <ul>
	 * <li>{@code numSourceDimensions == numTargetDimensions},</li>
	 * <li>the translation vector is zero, and</li>
	 * <li>no target component is zeroed out.</li>
	 * </ul>
	 * The computed inverse {@code MixedTransform} concatenates with {@code transform} to identity.
	 * @return the inverse {@code MixedTransform}
	 */
	private static MixedTransform invPermutationInversion( MixedTransform transform )
	{
		final int n = transform.numTargetDimensions();
		final int[] component = new int[ n ];
		final boolean[] invert = new boolean[ n ];
		final boolean[] zero = new boolean[ n ];
		transform.getComponentMapping( component );
		transform.getComponentInversion( invert );
		transform.getComponentZero( zero );

		final int m = transform.numSourceDimensions();
		final int[] invComponent = new int[ m ];
		final boolean[] invInvert = new boolean[ m ];
		final boolean[] invZero = new boolean[ m ];
		Arrays.fill( invZero, true );
		for ( int i = 0; i < n; i++ )
		{
			if ( transform.getComponentZero( i ) == false )
			{
				final int j = component[ i ];
				invComponent[ j ] = i;
				invInvert[ j ] = invert[ i ];
				invZero[ j ] = false;
			}
		}
		MixedTransform invTransform = new MixedTransform( n, m );
		invTransform.setComponentMapping( invComponent );
		invTransform.setComponentInversion( invInvert );
		invTransform.setComponentZero( invZero );
		return invTransform;
	}

	private static MixedTransform[] split( MixedTransform transform )
	{
		final int n = transform.numTargetDimensions();
		final int[] component = new int[ n ];
		final boolean[] invert = new boolean[ n ];
		final boolean[] zero = new boolean[ n ];
		final long[] translation = new long[ n ];
		transform.getComponentMapping( component );
		transform.getComponentInversion( invert );
		transform.getComponentZero( zero );
		transform.getTranslation( translation );

		final int m = transform.numSourceDimensions();
		final int[] splitComponent = new int[ m ];
		final boolean[] splitInvert = new boolean[ m ];

		int j = 0;
		for ( int i = 0; i < n; i++ )
		{
			if ( !zero[ i ] )
			{
				splitComponent[ j ] = component[ i ];
				splitInvert[ j ] = invert[ i ];
				component[ i ] = j++;
			}
		}

		final MixedTransform permuteInvert = new MixedTransform( m, m );
		permuteInvert.setComponentMapping( splitComponent );
		permuteInvert.setComponentInversion( splitInvert );

		final MixedTransform remainder = new MixedTransform( m, n );
		remainder.setComponentMapping( component );
		remainder.setComponentZero( zero );
		remainder.setTranslation( translation );

		return new MixedTransform[] { permuteInvert, remainder };
	}

	public static void main( String[] args )
	{
		{
			final MixedTransform t = new MixedTransform( 4, 4 );
			t.setComponentMapping( new int[] { 3, 0, 1, 2 } );
			t.setComponentInversion( new boolean[] { false, true, true, false } );

			System.out.println( "t    = " + t );

			final MixedTransform tinv = invPermutationInversion( t );
			System.out.println( "tinv = " + tinv );

			System.out.println( "t * tinv = " + t.concatenate( tinv ) );
			System.out.println( "tinv * t = " + tinv.concatenate( t ) );
			System.out.println();
			System.out.println();
		}

		{
			final MixedTransform t = new MixedTransform( 2, 3 );
			t.setComponentMapping( new int[] { 1, 0, 0 } );
			t.setComponentInversion( new boolean[] { false, false, true } );
			t.setComponentZero( new boolean[] { false, true, false } );

			System.out.println( "t    = " + t );

			final MixedTransform tinv = invPermutationInversion( t );
			System.out.println( "tinv = " + tinv );

			System.out.println( "t * tinv = " + t.concatenate( tinv ) );
			System.out.println( "tinv * t = " + tinv.concatenate( t ) );
			System.out.println();
			System.out.println();

		}

		{
			final MixedTransform t = new MixedTransform( 2, 3 );
			t.setComponentMapping( new int[] { 1, 0, 0 } );
			t.setComponentInversion( new boolean[] { false, false, true } );
			t.setComponentZero( new boolean[] { false, true, false } );
			t.setTranslation( new long[] { 10, 20, 30 } );

			System.out.println( "t                         = " + t );

			final MixedTransform[] split = split( t );
			final MixedTransform permuteInvert = split[ 0 ];
			final MixedTransform remainder = split[ 1 ];

			System.out.println( "remainder * permuteInvert = " + remainder.concatenate( permuteInvert ) );
			System.out.println( "remainder = " + remainder );
			System.out.println( "permuteInvert    = " + permuteInvert );
			System.out.println( "permuteInvert^-1 = " + invPermutationInversion( permuteInvert ) );
			System.out.println();
			System.out.println();

		}
	}

}

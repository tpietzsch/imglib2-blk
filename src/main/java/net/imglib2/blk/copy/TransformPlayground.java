package net.imglib2.blk.copy;

import net.imglib2.transform.integer.MixedTransform;

public class TransformPlayground
{
	public static void main( String[] args )
	{
		{
			final MixedTransform t = new MixedTransform( 4, 4 );
			t.setComponentMapping( new int[] { 3, 0, 1, 2 } );
			t.setComponentInversion( new boolean[] { false, true, true, false } );

			System.out.println( "t    = " + t );

			final MixedTransform tinv = PrimitiveBlocksUtils.invPermutationInversion( t );
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

			final MixedTransform tinv = PrimitiveBlocksUtils.invPermutationInversion( t );
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

			final MixedTransform[] split = PrimitiveBlocksUtils.split( t );
			final MixedTransform permuteInvert = split[ 0 ];
			final MixedTransform remainder = split[ 1 ];

			System.out.println( "remainder * permuteInvert = " + remainder.concatenate( permuteInvert ) );
			System.out.println( "remainder = " + remainder );
			System.out.println( "permuteInvert    = " + permuteInvert );
			System.out.println( "permuteInvert^-1 = " + PrimitiveBlocksUtils.invPermutationInversion( permuteInvert ) );
			System.out.println();
			System.out.println();

		}
	}

}

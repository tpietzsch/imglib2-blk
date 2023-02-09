package net.imglib2.blk.copy;

public class RangesTest
{

//	(from RangePlayground, 9cbcc5ee)
//  block to copy is within bounds of source image.
//
//	public static void main( String[] args )
//	{
//		int[][] data = {
//				{ 0, 1, 2, 3, 4 },
//				{ 5, 6, 7, 8, 9 },
//				{ 10, 11, 12, 13, 14 }
//		};
//		final int iw = 15;
//		final int cw = 5;
//
//		final int[] dest = new int[ 9 ];
//		final int bw = dest.length;
//		final List< RangePlayground.Range > ranges = findRanges( 3, bw, iw, cw );
//
//		copy( ranges, data, dest );
//		System.out.println( "dest = " + Arrays.toString( dest ) );
//	}
//
//	prints:
//
//	range = Range{gridx=0, cellx=3, w=2, dir=FORWARD, x=0}
//	range = Range{gridx=1, cellx=0, w=5, dir=FORWARD, x=2}
//	range = Range{gridx=2, cellx=0, w=2, dir=FORWARD, x=7}
//	dest = [3, 4, 5, 6, 7, 8, 9, 10, 11]






//	(from RangePlayground2, 9cbcc5ee)
//	mirror extension
//
//	public static void main( String[] args )
//	{
//		int[][] data = {
//				{ 0, 1, 2, 3 },
//				{ 4, 5 }
//		};
//		final int iw = 6;
//		final int cw = 4;
//
//		final int[] dest = new int[ 12 ];
//		final int bw = dest.length;
//		for ( int i = 0; i > -20; --i )
//		{
////			final List< Range > ranges = findRanges_mirror_double( i, bw, iw, cw );
//			final List< RangePlayground2.Range > ranges = findRanges_mirror_single( i, bw, iw, cw );
//			copy( ranges, data, dest );
//			System.out.println( "dest = " + Arrays.toString( dest ) );
//		}
//	}
//
//	prints:
//
//	dest = [0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 1]
//	dest = [1, 0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0]
//	dest = [2, 1, 0, 1, 2, 3, 4, 5, 4, 3, 2, 1]
//	dest = [3, 2, 1, 0, 1, 2, 3, 4, 5, 4, 3, 2]
//	dest = [4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 4, 3]
//	dest = [5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 4]
//	dest = [4, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5]
//	dest = [3, 4, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4]
//	dest = [2, 3, 4, 5, 4, 3, 2, 1, 0, 1, 2, 3]
//	dest = [1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 1, 2]
//	dest = [0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 1]
//	dest = [1, 0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0]
//	dest = [2, 1, 0, 1, 2, 3, 4, 5, 4, 3, 2, 1]
//	dest = [3, 2, 1, 0, 1, 2, 3, 4, 5, 4, 3, 2]
//	dest = [4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 4, 3]
//	dest = [5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 4]
//	dest = [4, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5]
//	dest = [3, 4, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4]
//	dest = [2, 3, 4, 5, 4, 3, 2, 1, 0, 1, 2, 3]
//	dest = [1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 1, 2]








//	(from RangePlayground3, 9cbcc5ee)
//	border extension
//
//	public static void main( String[] args )
//	{
//		int[][] data = {
//				{ 0, 1, 2, 3 },
//				{ 4, 5 }
//		};
//		final int iw = 6;
//		final int cw = 4;
//
//		final int[] dest = new int[ 20 ];
//		final int bw = dest.length;
//		for ( int i = 10; i > -10; --i )
//		{
//			final List< Range > ranges = findRanges_border( i, bw, iw, cw );
//			copy( ranges, data, dest );
//			System.out.println( "dest = " + Arrays.toString( dest ) + "          i = " + i );
//		}
//	}
//
//	prints:
//
//	dest = [5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 10
//	dest = [5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 9
//	dest = [5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 8
//	dest = [5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 7
//	dest = [5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 6
//	dest = [5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 5
//	dest = [4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 4
//	dest = [3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 3
//	dest = [2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 2
//	dest = [1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 1
//	dest = [0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = 0
//	dest = [0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = -1
//	dest = [0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = -2
//	dest = [0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = -3
//	dest = [0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = -4
//	dest = [0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = -5
//	dest = [0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5]          i = -6
//	dest = [0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 5]          i = -7
//	dest = [0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5]          i = -8
//	dest = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5]          i = -9



	public static void main( String[] args )
	{
		System.out.println( "TODO" );
	}
}

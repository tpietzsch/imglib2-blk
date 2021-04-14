package net.imglib2.blk.copy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.imglib2.blk.copy.RangePlayground2.Direction.BACKWARD;
import static net.imglib2.blk.copy.RangePlayground2.Direction.FORWARD;

public class RangePlayground2
{
	enum Direction
	{
		FORWARD,
		BACKWARD;
	}

	/**
	 * Copy {@code w} elements to coordinates {@code x} through {@code x + w}
	 * (exclusive) in destination, from source cell with {@code gridx} grid
	 * coordinate, starting at coordinate {@code cellx} within cell, and from
	 * there moving in {@code dir} for successive source elements.
	 *
	 * It is guaranteed that all {@code w} elements fall within the same cell.
	 */
	// TODO: should be flattened out instead of creating objects
	static class Range
	{
		final int gridx;
		final int cellx;
		final int w;
		final Direction dir;
		final int x;

		public Range( final int gridx, final int cellx, final int w, final Direction dir, final int x )
		{
			this.gridx = gridx;
			this.cellx = cellx;
			this.w = w;
			this.dir = dir;
			this.x = x;
		}

		@Override
		public String toString()
		{
			return "Range{" +
					"gridx=" + gridx +
					", cellx=" + cellx +
					", w=" + w +
					", dir=" + dir +
					", x=" + x +
					'}';
		}
	}

	// TODO: for now assumes only FORWARD direction and that block to copy is within bounds of source image.
	static List< Range >  findRanges(
			int bx, // start of block in source coordinates (in pixels)
			int bw, // width of block to copy (in pixels)
			int iw, // source image width (in pixels)
			int cw  // source cell width (in pixels)
	)
	{
		List< Range > ranges = new ArrayList<>();
		int gx = bx / cw;
		int cx = bx - ( gx * cw );
		int x = 0;
		while ( bw > 0 )
		{
			final int w = Math.min( bw, cellWidth( gx, cw, iw ) - cx );
			final Range range = new Range( gx, cx, w, FORWARD, x );
			System.out.println( "range = " + range );
			ranges.add( range );

			bw -= w;
			x += w;
			++gx;
			cx = 0;
		}
		return ranges;
	}

	static List< Range >  findRanges_mirror_double(
			int bx, // start of block in source coordinates (in pixels)
			int bw, // width of block to copy (in pixels)
			int iw, // source image width (in pixels)
			int cw  // source cell width (in pixels)
	)
	{
		List< Range > ranges = new ArrayList<>();

		final int pi = 2 * iw;
		bx = ( bx < 0 )
				? ( bx + 1 ) % pi + pi - 1
				: bx % pi;
		Direction dir = FORWARD;
		if ( bx >= iw )
		{
			bx = pi - 1 - bx;
			dir = BACKWARD;
		}

		int gx = bx / cw;
		int cx = bx - ( gx * cw );
		int x = 0;
		while ( bw > 0 )
		{
			if ( dir == FORWARD)
			{
				final int gxw = cellWidth( gx, cw, iw );
				final int w = Math.min( bw, gxw - cx );
				final Range range = new Range( gx, cx, w, FORWARD, x );
				System.out.println( "range = " + range );
				ranges.add( range );

				bw -= w;
				x += w;

				if ( ++gx * cw >= iw ) // moving out of bounds
				{
					--gx;
					cx = gxw - 1;
					dir = BACKWARD;
				}
				else
				{
					cx = 0;
				}
			}
			else // dir == BACKWARD
			{
				final int w = Math.min( bw, cx + 1 );
				final Range range = new Range( gx, cx, w, BACKWARD, x );
				System.out.println( "range = " + range );
				ranges.add( range );

				bw -= w;
				x += w;

				if ( gx == 0 ) // moving into bounds
				{
					cx = 0;
					dir = FORWARD;
				}
				else
				{
					cx = cellWidth( --gx, cw, iw ) - 1;
				}
			}

		}
		return ranges;
	}

	private static int cellWidth( final int gx, final int cw, final int iw )
	{
		final int gw = iw / cw;
		if ( gx < gw )
			return cw;
		else if ( gx == gw )
			return iw - cw * gw;
		else
			throw new IllegalArgumentException();
	}

	private static void copy( final List< Range > ranges, final int[][] data, final int[] dest )
	{
		int x = 0;
		for ( Range range : ranges )
		{
			final int[] cell = data[ range.gridx ];
			if ( range.dir == FORWARD )
			{
				for ( int i = 0; i < range.w; ++i )
					dest[ x++ ] = cell[ range.cellx + i ];
			}
			else
			{
				for ( int i = 0; i < range.w; ++i )
					dest[ x++ ] = cell[ range.cellx - i ];
			}
		}
	}

	public static void main( String[] args )
	{
		int[][] data = {
				{ 0, 1, 2, 3 },
				{ 4, 5 }
		};
		final int iw = 6;
		final int cw = 4;

		final int[] dest = new int[ 12 ];
		final int bw = dest.length;
		final List< Range > ranges = findRanges_mirror_double( -8, bw, iw, cw );

		copy( ranges, data, dest );
		System.out.println( "dest = " + Arrays.toString( dest ) );
	}
}

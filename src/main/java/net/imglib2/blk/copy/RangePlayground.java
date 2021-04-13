package net.imglib2.blk.copy;

import java.util.List;

public class RangePlayground
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
	}

	// TODO: for now assumes only FORWARD direction and that block to copy is within bounds of source image.
	static List< Range >  findRanges(
			int bx, // start of block in source coordinates (in pixels)
			int bw, // width of block to copy (in pixels)
			int iw, // source image width (in pixels)
			int cw  // source cell width (in pixels)
	)
	{
		int gx = bx / cw;
		int cx = bx - ( gx * cw );
		System.out.println( "gx = " + gx );
		System.out.println( "cx = " + cx );

		int x = 0;
		return null;
	}

	public static void main( String[] args )
	{
		findRanges( 3, 9, 15, 5 );
	}
}

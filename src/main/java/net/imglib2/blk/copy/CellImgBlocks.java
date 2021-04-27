package net.imglib2.blk.copy;

import java.util.Arrays;
import java.util.List;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.copy.Ranges.Range;
import net.imglib2.img.basictypeaccess.array.AbstractByteArray;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

import static net.imglib2.blk.copy.Ranges.Direction.CONSTANT;

public class CellImgBlocks
{
	private final AbstractCellImg< ?, ?, ?, ? > cellImg;
	private final ExtensionMethod extensionMethod;
	private final byte oobValue;

	private final CellGrid cellGrid;
	private final int n;
	private final int[] srcDims;
	private final RandomAccessible< ? extends Cell< ? > > cells;

	private FindRanges findRanges;

	// TODO: the following fields would be required per thread to make CellImgBlocks threadsafe
	private final List< Range >[] rangesPerDimension;
	private final Range[] ranges;
	private final RangeCopier copier;

	public enum ExtensionMethod
	{
		CONSTANT,
		BORDER,
		MIRROR_SINGLE,
		MIRROR_DOUBLE
	}

	public CellImgBlocks( final AbstractCellImg< ?, ?, ? extends Cell< ? >, ? > cellImg, ExtensionMethod extensionMethod )
	{
		this( cellImg, extensionMethod, ( byte ) 0 );
	}

	// TODO: CONSTANT extension method should have value parameter. Would be good use-case for sealed classes instead of enum.
	public CellImgBlocks( final AbstractCellImg< ?, ?, ? extends Cell< ? >, ? > cellImg, ExtensionMethod extensionMethod, final byte oobValue )
	{
		this.cellImg = cellImg;
		this.extensionMethod = extensionMethod;
		this.oobValue = oobValue;

		cellGrid = cellImg.getCellGrid();
		n = cellGrid.numDimensions();
		srcDims = new int[ n ];
		for ( int d = 0; d < n; d++ )
		{
			// TODO check whether it fits into Integer
			srcDims[ d ] = ( int ) cellGrid.imgDimension( d );
		}
		cells = cellImg.getCells();

		switch ( extensionMethod )
		{
		case CONSTANT:
			findRanges = Ranges::findRanges_constant;
			break;
		case BORDER:
			findRanges = Ranges::findRanges_border;
			break;
		case MIRROR_SINGLE:
			findRanges = Ranges::findRanges_mirror_single;
			break;
		case MIRROR_DOUBLE:
			findRanges = Ranges::findRanges_mirror_double;
			break;
		}

 		ranges = new Range[ n ];
		rangesPerDimension = new List[ n ];
		copier = new RangeCopier();
	}

	@FunctionalInterface
	private interface FindRanges
	{
		List< Range > findRanges(
				int bx, // start of block in source coordinates (in pixels)	}
				int bw, // width of block to copy (in pixels)
				int iw, // source image width (in pixels)
				int cw  // source cell width (in pixels)
		);
	}

	public void copy( final int[] srcPos, final byte[] dest, final int[] size )
	{
		// find ranges
		for ( int d = 0; d < n; ++d )
			rangesPerDimension[ d ] = findRanges.findRanges( srcPos[ d ], size[ d ], srcDims[ d ], cellGrid.cellDimension( d ) );

		// copy data
		copier.setupDestSize( size );
		copy1( dest, n - 1 );
	}

	private void copy1( final byte[] dest, final int d )
	{
		for ( Range range : rangesPerDimension[ d ] )
		{
			ranges[ d ] = range;
			copier.updateRange( d );
			if ( range.dir == CONSTANT )
				copier.fill( dest, d );
			else if ( d > 0 )
				copy1( dest, d - 1 );
			else
				copier.copy( dest );
		}
	}

	class RangeCopier
	{
		private final RandomAccess< ? extends Cell< ? > > cells = cellImg.getCells().randomAccess();

		private final int[] dsteps = new int[ n ];
		private final int[] doffsets = new int[ n + 1 ];
		private final int[] cdims = new int[ n ];
		private final int[] csteps = new int[ n ];
		private final int[] lengths = new int[ n ];

		void setupDestSize( final int[] size )
		{
			dsteps[ 0 ] = 1;
			for ( int d = 0; d < n - 1; ++d )
				dsteps[ d + 1 ] = dsteps[ d ] * size[ d ];
		}

		void updateRange( final int d )
		{
			final Range r = ranges[ d ];
			cells.setPosition( r.gridx, d );
			lengths[ d ] = r.w;
			doffsets[ d ] = doffsets[ d + 1 ] + dsteps[ d ] * r.x; // doffsets[ n ] == 0
			cdims[ d ] = cellGrid.getCellDimension( d, r.gridx );
		}

		void copy( final byte[] dest )
		{
			csteps[ 0 ] = 1;
			for ( int d = 0; d < n - 1; ++d )
				csteps[ d + 1 ] = csteps[ d ] * cdims[ d ];

			int sOffset = 0;
			for ( int d = 0; d < n; ++d )
			{
				final Range r = ranges[ d ];
				sOffset += csteps[ d ] * r.cellx;
				switch( r.dir )
				{
				case BACKWARD:
					csteps[ d ] = -csteps[ d ];
					break;
				case STAY:
					csteps[ d ] = 0;
					break;
				}
			}

			final int dOffset = doffsets[ 0 ];

			// TODO: generic type:
			//    Object           ArrayDataAccess< A >
			final byte[] src = ( ( AbstractByteArray< ? > ) cells.get().getData() ).getCurrentStorageArray();
			if ( n > 1 )
				copy1( src, sOffset, dest, dOffset, n - 1 );
			else
				copy0( src, sOffset, dest, dOffset );
		}

		private void copy1( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int d )
		{
			final int length = lengths[ d ];
			final int cstep = csteps[ d ];
			final int dstep = dsteps[ d ];
			if ( d > 1 )
				for ( int i = 0; i < length; ++i )
					copy1( src, srcPos + i * cstep, dest, destPos + i * dstep, d - 1 );
			else
				for ( int i = 0; i < length; ++i )
					copy0( src, srcPos + i * cstep, dest, destPos + i * dstep );
		}

		private void copy0( final byte[] src, final int srcPos, final byte[] dest, final int destPos )
		{
			final int length = lengths[ 0 ];
			final int cstep = csteps[ 0 ];
			if ( cstep == 1 )
			{
				System.arraycopy( src, srcPos, dest, destPos, length );
			}
			else
			{
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i ] = src[ srcPos + i * cstep ];
			}
		}

		void fill( final byte[] dest, final int dConst )
		{
			final int dOffset = doffsets[ dConst ];
			lengths[ dConst ] *= dsteps[ dConst ];

			if ( n - 1 > dConst )
				fill1( dest, dOffset, n - 1, dConst );
			else
				fill0( dest, dOffset, dConst );
		}

		private void fill1( final byte[] dest, final int destPos, final int d, final int dConst )
		{
			final int length = lengths[ d ];
			final int dstep = dsteps[ d ];
			if ( d > dConst + 1 )
				for ( int i = 0; i < length; ++i )
					fill1( dest, destPos + i * dstep, d - 1, dConst );
			else
				for ( int i = 0; i < length; ++i )
					fill0( dest, destPos + i * dstep, dConst );
		}

		private void fill0( final byte[] dest, final int destPos, final int dConst )
		{
			final int length = lengths[ dConst ];
			Arrays.fill( dest, destPos, destPos + length, oobValue );
//			for ( int i = 0; i < length; ++i )
//				dest[ destPos + i ] = oobValue;
		}
	}
}

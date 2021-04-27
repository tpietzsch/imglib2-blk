package net.imglib2.blk.copy;

import java.util.List;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.copy.Ranges.Range;
import net.imglib2.img.basictypeaccess.array.AbstractByteArray;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

public class CellImgBlocks
{
	private final AbstractCellImg< ?, ?, ?, ? > cellImg;
	private final ExtensionMethod extensionMethod;

	private final CellGrid cellGrid;
	private final int n;
	private final int[] srcDims;
	private final RandomAccessible< ? extends Cell< ? > > cells;

	private FindRanges findRanges;

	public enum ExtensionMethod
	{
		BORDER,
		MIRROR_SINGLE,
		MIRROR_DOUBLE
	}

	public CellImgBlocks( final AbstractCellImg< ?, ?, ? extends Cell< ? >, ? > cellImg, ExtensionMethod extensionMethod )
	{
		this.cellImg = cellImg;
		this.extensionMethod = extensionMethod;

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
		case BORDER:
			findRanges = Ranges::findRanges_border;
			break;
		case MIRROR_SINGLE:
			findRanges = Ranges::findRanges_mirror_single;
			break;
		case MIRROR_DOUBLE:
		default:
			findRanges = Ranges::findRanges_mirror_double;
			break;
		}
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
		final List< Range >[] rangesPerDimension = new List[ n ];
		for ( int d = 0; d < n; ++d )
			rangesPerDimension[ d ] = findRanges.findRanges( srcPos[ d ], size[ d ], srcDims[ d ], cellGrid.cellDimension( d ) );

		for ( int d = 0; d < n; ++d )
			System.out.println( String.format( "rangesPerDimension[%d].size() = %d", d, rangesPerDimension[ d ].size() ) );

		// copy data
		final Range[] ranges = new Range[ n ];
		final RangeCopier copier = new RangeCopier();
		copier.setupDestSize( size );
		copy1( rangesPerDimension, ranges, copier, dest, n - 1 );
		// TODO: rangesPerDimension, ranges, copier should be fields, not passed as parameters
	}

	private void copy1( final List< Range >[] rangesPerDimension, final Range[] ranges, final RangeCopier copier, final byte[] dest, final int d )
	{
		for ( Range range : rangesPerDimension[ d ] )
		{
			ranges[ d ] = range;
			if ( d > 0 )
				copy1( rangesPerDimension, ranges, copier, dest, d - 1 );
			else
				copier.copy( ranges, dest );
		}
	}

	class RangeCopier
	{
		private final RandomAccess< ? extends Cell< ? > > cells = cellImg.getCells().randomAccess();

		private final int[] dsteps = new int[ n ];
		private final int[] csteps = new int[ n ];
		private final int[] lengths = new int[ n ];

		void setupDestSize( final int[] size )
		{
			dsteps[ 0 ] = 1;
			for ( int d = 0; d < n - 1; ++d )
				dsteps[ d + 1 ] = dsteps[ d ] * size[ d ];
		}

		void copy( final Range[] ranges, final byte[] dest )
		{
			int sOffset = 0;
			int dOffset = 0;
			csteps[ 0 ] = 1;
			for ( int d = 0; d < n; ++d )
			{
				final Range r = ranges[ d ];
				cells.setPosition( r.gridx, d );
				lengths[ d ] = r.w;

				if ( d < n - 1 )
				{
					final int cdim = cellGrid.getCellDimension( d, r.gridx );
					csteps[ d + 1 ] = csteps[ d ] * cdim;
				}
				sOffset += csteps[ d ] * r.cellx;
				dOffset += dsteps[ d ] * r.x;

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
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos + i * cstep ];
		}
	}
}

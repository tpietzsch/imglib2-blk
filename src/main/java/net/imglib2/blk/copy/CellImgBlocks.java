package net.imglib2.blk.copy;

import java.util.Arrays;
import java.util.List;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.copy.Ranges.Range;
import net.imglib2.img.basictypeaccess.array.AbstractByteArray;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import static net.imglib2.blk.copy.Ranges.Direction.CONSTANT;

public class CellImgBlocks
{
	private final AbstractCellImg< ?, ?, ?, ? > cellImg;
	private final ExtensionMethod extensionMethod;
	private final UnsignedByteType oobValue;

	private final CellGrid cellGrid;
	private final int n;
	private final int[] srcDims;
	private final RandomAccessible< ? extends Cell< ? > > cells;

	private final FindRanges findRanges;
	private final ThreadLocal< RangeCopier > copier = ThreadLocal.withInitial( RangeCopier::new );

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
		this.oobValue = new UnsignedByteType( oobValue );


		// TODO: store type, verify dest array type in copy(...)
		final NativeType< ? > type = cellImg.createLinkedType();
//		System.out.println( "type = " + type.getClass() );


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
		default:
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

	/**
	 * @param srcPos
	 * 		min coordinates of block to copy from src Img.
	 * @param dest
	 * 		destination array. Type is {@code byte[]}, {@code float[]},
	 * 		etc, corresponding to the src Img's native type.
	 * @param size
	 * 		dimensions of block to copy from src Img.
	 */
	public void copy( final int[] srcPos, final Object dest, final int[] size )
	{
		copier.get().copy( srcPos, dest, size );
	}

	class RangeCopier
	{
		private final List< Range >[] rangesPerDimension = new List[ n ];
		private final Range[] ranges = new Range[ n ];

		/**
		 * @param srcPos
		 * 		min coordinates of block to copy from src Img.
		 * @param dest
		 * 		destination array. Type is {@code byte[]}, {@code float[]},
		 * 		etc, corresponding to the src Img's native type.
		 * @param size
		 * 		dimensions of block to copy from src Img.
		 */
		public void copy( final int[] srcPos, final Object dest, final int[] size )
		{
			// find ranges
			for ( int d = 0; d < n; ++d )
				rangesPerDimension[ d ] = findRanges.findRanges( srcPos[ d ], size[ d ], srcDims[ d ], cellGrid.cellDimension( d ) );

			// copy data
			setupDestSize( size );
			copyRanges( dest, n - 1 );
		}

		/**
		 * @param dest
		 * 		destination array. Type is {@code byte[]}, {@code float[]},
		 * 		etc, corresponding to the src Img's native type.
		 * @param d
		 * 		current dimension. This method calls itself recursively with
		 * 		{@code d-1} until {@code d==0} is reached.
		 */
		private void copyRanges( final Object dest, final int d )
		{
			for ( Range range : rangesPerDimension[ d ] )
			{
				ranges[ d ] = range;
				updateRange( d );
				if ( range.dir == CONSTANT )
					fill( dest, d );
				else if ( d > 0 )
					copyRanges( dest, d - 1 );
				else
					copy( dest );
			}
		}

		private final RandomAccess< ? extends Cell< ? > > cellAccess = cells.randomAccess();

		private final int[] dsteps = new int[ n ];
		private final int[] doffsets = new int[ n + 1 ];
		private final int[] cdims = new int[ n ];
		private final int[] csteps = new int[ n ];
		private final int[] lengths = new int[ n ];

		private void setupDestSize( final int[] size )
		{
			dsteps[ 0 ] = 1;
			for ( int d = 0; d < n - 1; ++d )
				dsteps[ d + 1 ] = dsteps[ d ] * size[ d ];
		}

		private void updateRange( final int d )
		{
			final Range r = ranges[ d ];
			cellAccess.setPosition( r.gridx, d );
			lengths[ d ] = r.w;
			doffsets[ d ] = doffsets[ d + 1 ] + dsteps[ d ] * r.x; // doffsets[ n ] == 0
			cdims[ d ] = cellGrid.getCellDimension( d, r.gridx );
		}

		private void copy( final Object dest )
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

			final Object src = ( ( ArrayDataAccess< ? > ) cellAccess.get().getData() ).getCurrentStorageArray();
			if ( n > 1 )
				copyRanges( src, sOffset, dest, dOffset, n - 1 );
			else
				copy0( src, sOffset, dest, dOffset, lengths[ 0 ], csteps[ 0 ] );
		}

		private void copyRanges( final Object src, final int srcPos, final Object dest, final int destPos, final int d )
		{
			final int length = lengths[ d ];
			final int cstep = csteps[ d ];
			final int dstep = dsteps[ d ];
			if ( d > 1 )
				for ( int i = 0; i < length; ++i )
					copyRanges( src, srcPos + i * cstep, dest, destPos + i * dstep, d - 1 );
			else
				for ( int i = 0; i < length; ++i )
					copy0( src, srcPos + i * cstep, dest, destPos + i * dstep, lengths[ 0 ], csteps[ 0 ] );
		}

		// T is a primitive array type
		interface MemCopy< T >
		{
			void copyForward( final T src, final int srcPos, final T dest, final int destPos, final int length );
			void copyReverse( final T src, final int srcPos, final T dest, final int destPos, final int length );
			void copyValue( final T src, final int srcPos, final T dest, final int destPos, final int length );

			MemCopyByte BYTE = new MemCopyByte();
		}

		static class MemCopyByte implements MemCopy< byte[] >
		{
			@Override
			public void copyForward( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length )
			{
				System.arraycopy( src, srcPos, dest, destPos, length );
			}

			@Override
			public void copyReverse( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length )
			{
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i ] = src[ srcPos - i ];
			}

			@Override
			public void copyValue( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length )
			{
				Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
			}
		}

		private MemCopy< byte[] > memCopy = MemCopy.BYTE;

		// TODO: move to interface, below is implementation for T == byte[]
		// TODO: special case implementation for cstep==0
		private void copy0( final Object gsrc, final int srcPos, final Object gdest, final int destPos, final int length, final int cstep )
		{
			final byte[] src = ( byte[] ) gsrc;
			final byte[] dest = ( byte[] ) gdest;
			if ( cstep == 1 )
			{
				memCopy.copyForward( src, srcPos, dest, destPos, length );
			}
			else if ( cstep == -1 )
			{
				memCopy.copyReverse( src, srcPos, dest, destPos, length );
			}
			else if ( cstep == 0 )
			{
				memCopy.copyValue( src, srcPos, dest, destPos, length );
			}
			else
			{
				throw new IllegalArgumentException();
			}
		}

		void fill( final Object dest, final int dConst )
		{
			final int dOffset = doffsets[ dConst ];
			lengths[ dConst ] *= dsteps[ dConst ];

			if ( n - 1 > dConst )
				fill1( dest, dOffset, n - 1, dConst );
			else
				fill0( dest, dOffset, lengths[ dConst ] );
		}

		private void fill1( final Object dest, final int destPos, final int d, final int dConst )
		{
			final int length = lengths[ d ];
			final int dstep = dsteps[ d ];
			if ( d > dConst + 1 )
				for ( int i = 0; i < length; ++i )
					fill1( dest, destPos + i * dstep, d - 1, dConst );
			else
				for ( int i = 0; i < length; ++i )
					fill0( dest, destPos + i * dstep, lengths[ dConst ] );
		}

		final byte[] oob = new byte[] { oobValue.getByte() };

		// TODO: express as copy0(...) with cstep = 0 and oobValue.getStorageArray...?
		// TODO: move to interface
		//       below is implementation for T == byte[]
		//       oobValue is field of the implementing class
		private void fill0( final Object gdest, final int destPos, final int length )
		{
			copy0( oob, 0, gdest, destPos, length, 0 );

//			final byte[] dest = ( byte[] ) gdest;
//			Arrays.fill( dest, destPos, destPos + length, oobValue.getByte() );

//			for ( int i = 0; i < length; ++i )
//				dest[ destPos + i ] = oobValue;
		}
	}
}

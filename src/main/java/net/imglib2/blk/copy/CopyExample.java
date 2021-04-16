package net.imglib2.blk.copy;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import ij.IJ;
import ij.ImagePlus;
import java.util.Arrays;
import java.util.List;
import net.imglib2.RandomAccess;
import net.imglib2.blk.copy.RangePlayground3.Range;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.AbstractByteArray;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.list.ListRandomAccess;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import net.imglib2.blk.copy.RangePlayground3;

public class CopyExample
{
	public static void main( String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/DrosophilaWing.tif";
//		final String fn = "/Users/pietzsch/workspace/data/leafcrop.tif";
		final ImagePlus imp = IJ.openImage( fn );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		final CellImg< UnsignedByteType, ? > cellImg = new CellImgFactory<>( new UnsignedByteType(), 32, 64 ).create( img );
		LoopBuilder.setImages( img, cellImg ).forEachPixel( ( a, b ) -> b.set( a ) );

		BdvFunctions.show(
				Views.extendMirrorSingle( cellImg ),
				cellImg,
				"input",
				Bdv.options().is2D() );

		System.out.println( "Intervals.toString(cellImg) = " + Intervals.toString( cellImg ) );
		final CellGrid cellGrid = cellImg.getCellGrid();
		System.out.println( "cellGrid = " + cellGrid );

		final int ox = -134;
		final int oy = -57;
		final int bw = 1600;
		final int bh = 1600;

		FindRanges[] methods = {
				RangePlayground3::findRanges_mirror_single,
				RangePlayground3::findRanges_mirror_double,
				RangePlayground3::findRanges_border	};
		Bdv bdv = null;
		for ( FindRanges method : methods )
		{
			final byte[] data = new byte[ bw * bh ];
			final Img< UnsignedByteType > output = ArrayImgs.unsignedBytes( data, bw, bh );

			copyFast( cellImg, new int[] { ox, oy }, new int[] { bw, bh }, data, method );

			bdv = BdvFunctions.show(
					output,
					"output",
					Bdv.options().is2D().addTo( bdv ) );
		}
	}

	@FunctionalInterface
	interface FindRanges
	{
		List< Range > findRanges(
				int bx, // start of block in source coordinates (in pixels)	}
				int bw, // width of block to copy (in pixels)
				int iw, // source image width (in pixels)
				int cw  // source cell width (in pixels)
		);
	}

	static class RangeCopier
	{
		private final int n;
		private final CellGrid grid;
		private final RandomAccess< ? extends Cell< ? > > cells;

		private final int[] csteps;
		private final int[] dsteps;
		private final int[] lengths;

		public RangeCopier( final CellImg< UnsignedByteType, ? > src )
		{
			n = src.numDimensions();
			grid = src.getCellGrid();
			cells = src.getCells().randomAccess();

			csteps = new int[ n ];
			dsteps = new int[ n ];
			lengths = new int[ n ];
		}

		public void copy( final Range[] ranges, final int[] dimensions, final byte[] dest )
		{
			int sOffset = 0;
			int dOffset = 0;
			csteps[ 0 ] = 1;
			dsteps[ 0 ] = 1;
			for ( int d = 0; d < n; ++d )
			{
				cells.setPosition( ranges[ d ].gridx, d );
				lengths[ d ] = ranges[ d ].w;

				final int cdim = grid.getCellDimension( d, ranges[ d ].gridx );
				if ( d < n - 1 )
				{
					csteps[ d + 1 ] = csteps[ d ] * cdim;
					dsteps[ d + 1 ] = dsteps[ d ] * dimensions[ d ];
				}
				sOffset += csteps[ d ] * ranges[ d ].cellx;
				dOffset += dsteps[ d ] * ranges[ d ].x;

				switch( ranges[ d ].dir )
				{
				case BACKWARD:
					csteps[ d ] = -csteps[ d ];
					break;
				case STAY:
					csteps[ d ] = 0;
					break;
				}
			}

			final byte[] src = ( ( AbstractByteArray< ? > ) cells.get().getData() ).getCurrentStorageArray();
			copy1( src, sOffset, dest, dOffset );
		}

		private void copy1( final byte[] src, final int srcPos, final byte[] dest, final int destPos )
		{
			final int length = lengths[ 1 ];
			final int cstep = csteps[ 1 ];
			final int dstep = dsteps[ 1 ];
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

	private static void copyFast( final CellImg< UnsignedByteType, ? > src, final int[] min, final int[] dimensions, final byte[] dest,
			final FindRanges findRanges )
	{
		final CellGrid grid = src.getCellGrid();
		final List< Range > ranges0 = findRanges.findRanges( min[ 0 ], dimensions[ 0 ], ( int ) grid.imgDimension( 0 ), grid.cellDimension( 0 ) );
		final List< Range > ranges1 = findRanges.findRanges( min[ 1 ], dimensions[ 1 ], ( int ) grid.imgDimension( 1 ), grid.cellDimension( 1 ) );

		final RangeCopier copier = new RangeCopier( src );
		final Range[] ranges = new Range[ 2 ];
		for ( Range range1 : ranges1 )
		{
			ranges[ 1 ] = range1;
			for ( Range range0 : ranges0 )
			{
				ranges[ 0 ] = range0;
				copier.copy( ranges, dimensions, dest );
			}
		}
	}

	private static void copy( final CellImg< UnsignedByteType, ? > src, final int[] min, final int[] dimensions, final byte[] dest,
			final FindRanges findRanges )
	{
		final CellGrid grid = src.getCellGrid();
		final List< Range > ranges0 = findRanges.findRanges( min[ 0 ], dimensions[ 0 ], ( int ) grid.imgDimension( 0 ), grid.cellDimension( 0 ) );
		final List< Range > ranges1 = findRanges.findRanges( min[ 1 ], dimensions[ 1 ], ( int ) grid.imgDimension( 1 ), grid.cellDimension( 1 ) );
//		System.out.println( "ranges0 = " + ranges0 );
//		System.out.println( "ranges1 = " + ranges1 );

		for ( Range range1 : ranges1 )
		{
			for ( Range range0 : ranges0 )
			{
				final int g0 = range0.gridx;
				final int g1 = range1.gridx;
				final Cell cell = src.getCells().getAt( g0, g1 );
				final byte[] srcData = ( byte[] ) ( ( ( ArrayDataAccess< ? > ) cell.getData() ).getCurrentStorageArray() );

				final int c0 = range0.cellx;
				final int c1 = range1.cellx;

				final int[] cdims = new int[ 2 ];
				cell.dimensions( cdims );
				final int[] csteps = new int[ 2 ];
				IntervalIndexer.createAllocationSteps( cdims, csteps );

				final int cstep0;
				switch( range0.dir )
				{
				case FORWARD:
					cstep0 = csteps[ 0 ];
					break;
				case BACKWARD:
					cstep0 = -csteps[ 0 ];
					break;
				default:
				case STAY:
					cstep0 = 0;
					break;
				}
				final int cstep1;
				switch( range1.dir )
				{
				case FORWARD:
					cstep1 = csteps[ 1 ];
					break;
				case BACKWARD:
					cstep1 = -csteps[ 1 ];
					break;
				default:
				case STAY:
					cstep1 = 0;
					break;
				}

				final int sw0 = range0.w;
				final int sw1 = range1.w;

				final int x0 = range0.x;
				final int x1 = range1.x;
				final int dstep1 = dimensions[ 0 ];

				final int sOffset = csteps[ 1 ] * c1 + csteps[ 0 ] * c0;
				final int dOffset = x1 * dimensions[ 0 ] + x0;

				for ( int j = 0; j < sw1; ++j )
				{
					for ( int i = 0; i < sw0; ++i )
					{
						dest[ dOffset + j * dstep1 + i ] =
								srcData[ sOffset + j * cstep1 + i * cstep0 ];
					}
				}
			}
		}
	}
}

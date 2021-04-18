package net.imglib2.blk.copy;

import net.imglib2.RandomAccessible;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

public class CellImgBlocks
{
	private final AbstractCellImg< ?, ?, ?, ? > cellImg;

	private final ExtensionMethod extensionMethod;

	public enum ExtensionMethod
	{
		BORDER,
		MIRROR_SINGLE,
		MIRROR_DOUBLE
	}

	public CellImgBlocks( final AbstractCellImg<?, ?, ? extends Cell< ? >, ?> cellImg, ExtensionMethod extensionMethod )
	{
		this.cellImg = cellImg;
		this.extensionMethod = extensionMethod;

		final CellGrid cellGrid = cellImg.getCellGrid();
		final int n = cellGrid.numDimensions();
		final int[] srcDims = new int[ n ];
		for ( int d = 0; d < n; d++ )
		{
			// TODO check whether it fits into Integer
			srcDims[ d ] = ( int ) cellGrid.imgDimension( d );
		}
		final RandomAccessible< ? extends Cell< ? > > cells = cellImg.getCells();
	}

	public void copy( final int[] srcPos, final int[] dest, final int[] size )
	{
		// find ranges
	}


	public static void main( String[] args )
	{

	}


}

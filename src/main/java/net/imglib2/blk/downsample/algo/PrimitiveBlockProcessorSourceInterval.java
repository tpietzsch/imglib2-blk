package net.imglib2.blk.downsample.algo;

import net.imglib2.Interval;

/**
 * Helper class that wraps {@link BlockProcessor#getSourcePos()} and {@link
 * BlockProcessor#getSourceSize()} as an {@code Interval}.
 */
public class PrimitiveBlockProcessorSourceInterval implements Interval
{
	private BlockProcessor< ?, ? > p;

	public PrimitiveBlockProcessorSourceInterval( BlockProcessor< ?, ? > blockProcessor )
	{
		this.p = blockProcessor;
	}

	@Override
	public int numDimensions()
	{
		return p.getSourcePos().length;
	}

	@Override
	public long min( final int d )
	{
		return p.getSourcePos()[ d ];
	}

	@Override
	public long max( final int d )
	{
		return min( d ) + dimension( d ) - 1;
	}

	@Override
	public long dimension( final int d )
	{
		return p.getSourceSize()[ d ];
	}
}

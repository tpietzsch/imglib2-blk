package net.imglib2.blk.downsample.algo;

import java.util.function.Supplier;
import net.imglib2.Interval;

/**
 * @param <I> input primitive array type, e.g., float[]
 * @param <O> output primitive array type, e.g., float[]
 */
// TODO rename
// TODO rename package
// TODO javadoc
public interface BlockProcessor< I, O >
{
	Supplier< ? extends BlockProcessor< I, O > > threadSafeSupplier();

	void setTargetInterval( Interval interval );

	long[] getSourcePos();

	int[] getSourceSize();

	I getSourceBuffer();

	void compute( I src, O dest );
}
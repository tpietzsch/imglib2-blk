package net.imglib2.blk.downsample.algo;

import java.util.function.Supplier;
import net.imglib2.Interval;
import net.imglib2.blk.downsample.DownsampleFloat;

/**
 * @param <P> primitive array type, e.g., float[]
 */
// TODO rename
// TODO rename package
// TODO javadoc
public interface BlockProcessor< P >
{
	Supplier< ? extends BlockProcessor< P > > threadSafeSupplier();

	void setTargetInterval( Interval interval );

	long[] getSourcePos();

	int[] getSourceSize();

	P getSourceBuffer();

	void compute( P src, P dest );
}

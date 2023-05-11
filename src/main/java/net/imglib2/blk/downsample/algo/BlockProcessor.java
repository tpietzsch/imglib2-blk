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

	// TODO: Its cumbersome to have both getSourcePos()/getSourceSize() *and* getSourceInterval()
	//       Only have getSourcePos()/getSourceSize() ?
	//       Have a modifiable SourceInterval class exposing getSourcePos()/getSourceSize() ?
	Interval getSourceInterval();

	I getSourceBuffer();

	void compute( I src, O dest );

	default < P > BlockProcessor< I, P > andThen( BlockProcessor< O, P > processor )
	{
		return new ConcatenatedBlockProcessor<>( this, processor );
	}
}

package net.imglib2.blk.downsample;

@FunctionalInterface
interface ConvertLoop< I, O >
{
	void apply( final I src, final O dest, final int length );

}

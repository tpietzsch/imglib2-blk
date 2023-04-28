package net.imglib2.blk.downsample.algo;

import java.util.function.Supplier;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.type.NativeType;

// TODO rename
// TODO rename package
// TODO javadoc
public class AlgoUtils
{
	public static < T extends NativeType< T >, I, O >
	CellLoader< T > cellLoader( final PrimitiveBlocks< T > blocks, BlockProcessor< I, O > blockProcessor )
	{
		final PrimitiveBlocks< T > threadSafeBlocks = blocks.threadSafe();
		final Supplier< ? extends BlockProcessor< I, O > > processorSupplier = blockProcessor.threadSafeSupplier();
		return cell -> {
			final BlockProcessor< I, O > processor = processorSupplier.get();
			processor.setTargetInterval( cell );
			final I src = processor.getSourceBuffer();
			threadSafeBlocks.copy( processor.getSourcePos(), src, processor.getSourceSize() );
			@SuppressWarnings( { "unchecked" } )
			final O dest = ( O ) cell.getStorageArray();
			processor.compute( src, dest );
		};
	}

	public static < T extends NativeType< T >, I, O >
	CachedCellImg< T, ? > cellImg(
			final PrimitiveBlocks< T > blocks,
			BlockProcessor< I, O > blockProcessor,
			final T type,
			final long[] dimensions,
			final int[] cellDimensions )
	{
		final CellLoader< T > loader = cellLoader( blocks, blockProcessor );
		return new ReadOnlyCachedCellImgFactory().create(
				dimensions,
				type,
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions) );
	}
}

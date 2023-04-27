package net.imglib2.blk.downsample.algo;

import java.util.function.Supplier;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.type.NativeType;

// TODO rename
// TODO rename package
// TODO javadoc
public class AlgoUtils
{
	public static < T extends NativeType< T >, P >
	CellLoader< T > cellLoader( final PrimitiveBlocks< T > blocks, BlockProcessor< P > blockProcessor )
	{
		final PrimitiveBlocks< T > threadSafeBlocks = blocks.threadSafe();
		final Supplier< ? extends BlockProcessor< P > > processorSupplier = blockProcessor.threadSafeSupplier();
		return cell -> {
			final BlockProcessor< P > processor = processorSupplier.get();
			processor.setTargetInterval( cell );
			final P src = processor.getSourceBuffer();
			@SuppressWarnings( { "unchecked" } )
			final P dest = ( P ) cell.getStorageArray();
			threadSafeBlocks.copy( processor.getSourcePos(), src, processor.getSourceSize() );
			processor.compute( src, dest );
		};
	}

}

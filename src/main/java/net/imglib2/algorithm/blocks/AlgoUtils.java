package net.imglib2.algorithm.blocks;

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
	public static < S extends NativeType< S >, T extends NativeType< T >, I, O >
	CellLoader< T > cellLoader( final PrimitiveBlocks< S > blocks, final UnaryBlockOperator< S, T > operator )
	{
		final PrimitiveBlocks< S > threadSafeBlocks = blocks.threadSafe();
		final UnaryBlockOperator< S, T > threadSafeOperator = operator.threadSafe();
		return cell -> {
			final BlockProcessor< I, O > processor = threadSafeOperator.blockProcessor();
			processor.setTargetInterval( cell );
			final I src = processor.getSourceBuffer();
			threadSafeBlocks.copy( processor.getSourcePos(), src, processor.getSourceSize() );
			@SuppressWarnings( { "unchecked" } )
			final O dest = ( O ) cell.getStorageArray();
			processor.compute( src, dest );
		};
	}

	public static < S extends NativeType< S >, T extends NativeType< T >, I, O >
	CachedCellImg< T, ? > cellImg(
			final PrimitiveBlocks< S > blocks,
			final UnaryBlockOperator< S, T > operator,
			final T type,
			final long[] dimensions,
			final int[] cellDimensions )
	{
		final CellLoader< T > loader = cellLoader( blocks, operator );
		return new ReadOnlyCachedCellImgFactory().create(
				dimensions,
				type,
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions) );
	}

	// TODO remove
	@Deprecated
	public static < S extends NativeType< S >, T extends NativeType< T >, I, O >
	CellLoader< T > cellLoader( final PrimitiveBlocks< S > blocks, BlockProcessor< I, O > blockProcessor )
	{
		final PrimitiveBlocks< S > threadSafeBlocks = blocks.threadSafe();
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

	// TODO remove
	@Deprecated
	public static < S extends NativeType< S >, T extends NativeType< T >, I, O >
	CachedCellImg< T, ? > cellImg(
			final PrimitiveBlocks< S > blocks,
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

package net.imglib2.blk.copy;

import java.util.function.Supplier;
import net.imglib2.type.NativeType;


/**
 * Copy data out of a {@code T}-typed source into primitive arrays (of the appropriate type).
 * <p>
 * Implementations are not thread-safe in general. Use {@link #threadSafe()} to
 * get a thread-safe instance.
 *
 * @param <T>
 * 		pixel type
 */
public interface PrimitiveBlocks< T extends NativeType< T > >
{
	T getType();

	void copy( final int[] srcPos, final Object dest, final int[] size );

	PrimitiveBlocks< T > threadSafe();
}

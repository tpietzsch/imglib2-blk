package net.imglib2.blk.copy;

import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.view.TransformBuilder;

import static net.imglib2.blk.copy.PrimitiveBlocks.OnFallback.FAIL;
import static net.imglib2.blk.copy.PrimitiveBlocks.OnFallback.WARN;


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

	/**
	 * Copy a block from the ({@code T}-typed) source into primitive arrays (of the appropriate type).
	 *
	 * @param srcPos
	 * 		min coordinate of the block to copy
	 * @param dest
	 * 		primitive array to copy into. Must correspond to {@code T}, for
	 *      example, if {@code T} is {@code UnsignedByteType} then {@code dest} must
	 *      be {@code byte[]}.
	 * @param size
	 * 		the size of the block to copy
	 */
	void copy( int[] srcPos, Object dest, int[] size );

	PrimitiveBlocks< T > threadSafe();

	enum OnFallback
	{
		ACCEPT,
		WARN,
		FAIL
	}

	static < T extends NativeType< T >, R extends NativeType< R > > PrimitiveBlocks< T > of(
			RandomAccessible< T > ra )
	{
		return of( ra, WARN );
	}

	static < T extends NativeType< T >, R extends NativeType< R > > PrimitiveBlocks< T > of(
			RandomAccessible< T > ra,
			OnFallback onFallback )
	{
		final ViewPropertiesOrError< T, R > props = ViewAnalyzer.getViewProperties( ra );
		if ( props.isFullySupported() )
		{
			return new ViewPrimitiveBlocks<>( props.getViewProperties() );
		}
		else if ( props.isSupported() && onFallback != FAIL )
		{
			if ( onFallback == WARN )
				System.err.println( props.getErrorMessage() );
			return new FallbackPrimitiveBlocks<>( props.getFallbackProperties() );
		}
		throw new IllegalArgumentException( props.getErrorMessage() );
	}
}

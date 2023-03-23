package net.imglib2.blk.copy;

import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;

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

	void copy( final int[] srcPos, final Object dest, final int[] size );

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

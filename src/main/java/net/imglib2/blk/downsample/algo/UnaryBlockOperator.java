package net.imglib2.blk.downsample.algo;

import net.imglib2.blk.downsample.ClampType;
import net.imglib2.blk.downsample.Convert;
import net.imglib2.type.NativeType;
import net.imglib2.util.Cast;

public interface UnaryBlockOperator< S extends NativeType< S >, T extends NativeType< T > >
{
	/**
	 * TODO javadoc
	 *
	 * unchecked cast!
	 *
	 * @return
	 * @param <I> input primitive array type, e.g., float[]. Must correspond to S.
	 * @param <O> output primitive array type, e.g., float[]. Must correspond to T.
	 */
	< I, O > BlockProcessor< I, O > blockProcessor();

	S getSourceType();

	T getTargetType();

	/**
	 * Get a thread-safe version of this {@code BlockProcessor}.
	 * (Implemented as a wrapper that makes {@link ThreadLocal} copies).
	 */
	UnaryBlockOperator< S, T > threadSafe();

	default < U extends NativeType< U > > UnaryBlockOperator< S, U > andThen( UnaryBlockOperator< T, U > op )
	{
		return new DefaultUnaryBlockOperator<>(
				getSourceType(),
				op.getTargetType(),
				blockProcessor().andThen( op.blockProcessor() ) );
	}

	default < U extends NativeType< U > > UnaryBlockOperator< U, T > adaptSourceType( U newSourceType, ClampType clamp )
	{
		if ( newSourceType.getClass().isInstance( getSourceType() ) )
			return Cast.unchecked( this );
		else
			return convert( newSourceType, getSourceType(), clamp ).andThen( this );
	}

	default  < U extends NativeType< U > > UnaryBlockOperator< S, U > adaptTargetType( U newTargetType, ClampType clamp )
	{
		if ( newTargetType.getClass().isInstance( getTargetType() ) )
			return Cast.unchecked( this );
		else
			return this.andThen( convert( getTargetType(), newTargetType, clamp ) );
	}

	// TODO: MOVE somewhere else
	static < S extends NativeType< S >, T extends NativeType< T > > UnaryBlockOperator< S, T > convert( S sourceType, T targetType, ClampType clamp )
	{
		return new DefaultUnaryBlockOperator<>( sourceType, targetType, new Convert<>( sourceType, targetType, clamp ) );
	}
}

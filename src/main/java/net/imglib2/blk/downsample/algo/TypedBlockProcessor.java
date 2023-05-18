package net.imglib2.blk.downsample.algo;

import net.imglib2.blk.downsample.ClampType;
import net.imglib2.blk.downsample.Convert;
import net.imglib2.type.NativeType;
import net.imglib2.util.Cast;

public class TypedBlockProcessor< S extends NativeType< S >, T extends NativeType< T > >
{
	private final S sourceType;
	private final T targetType;
	private final BlockProcessor< ?, ? > processor;

	public TypedBlockProcessor( S sourceType, T targetType, BlockProcessor< ?, ? > processor )
	{
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.processor = processor;
	}

	public < I, O > BlockProcessor< I, O > processor()
	{
		return Cast.unchecked( processor );
	}

	public < U extends NativeType< U > > TypedBlockProcessor< S, U > andThen( TypedBlockProcessor< T, U > p )
	{
		return new TypedBlockProcessor<>( sourceType, p.targetType, processor.andThen( p.processor() ) );
	}

	public < U extends NativeType< U > > TypedBlockProcessor< U, T > adaptSourceType( U newSourceType, ClampType clamp )
	{
		if ( newSourceType.getClass().isInstance( sourceType ) )
			return Cast.unchecked( this );
		else
			return convert( newSourceType, sourceType, clamp ).andThen( this );
	}

	public < U extends NativeType< U > > TypedBlockProcessor< S, U > adaptTargetType( U newTargetType, ClampType clamp )
	{
		if ( newTargetType.getClass().isInstance( targetType ) )
			return Cast.unchecked( this );
		else
			return this.andThen( convert( targetType, newTargetType, clamp ) );
	}

	static < S extends NativeType< S >, T extends NativeType< T > > TypedBlockProcessor< S, T > convert( S sourceType, T targetType, ClampType clamp )
	{
		return new TypedBlockProcessor<>( sourceType, targetType, new Convert<>( sourceType, targetType, clamp ) );
	}
}

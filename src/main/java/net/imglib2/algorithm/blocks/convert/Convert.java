package net.imglib2.algorithm.blocks.convert;

import net.imglib2.algorithm.blocks.DefaultUnaryBlockOperator;
import net.imglib2.algorithm.blocks.UnaryBlockOperator;
import net.imglib2.type.NativeType;

public class Convert
{
	public static < S extends NativeType< S >, T extends NativeType< T > >
	UnaryBlockOperator< S, T > convert( final S sourceType, final T targetType )
	{
		return convert( sourceType, targetType, ClampType.NONE );
	}

	public static < S extends NativeType< S >, T extends NativeType< T > >
	UnaryBlockOperator< S, T > convert( final S sourceType, final T targetType, final ClampType clamp )
	{
		return new DefaultUnaryBlockOperator<>( sourceType, targetType, new ConvertBlockProcessor<>( sourceType, targetType, clamp ) );
	}
}

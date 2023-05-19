package net.imglib2.blk.downsample;

import net.imglib2.blk.downsample.algo.DefaultUnaryBlockOperator;
import net.imglib2.blk.downsample.algo.UnaryBlockOperator;
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
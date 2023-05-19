package net.imglib2.blk.downsample;

import java.util.Arrays;
import net.imglib2.blk.downsample.DownsampleBlockProcessors.CenterDouble;
import net.imglib2.blk.downsample.DownsampleBlockProcessors.CenterFloat;
import net.imglib2.blk.downsample.DownsampleBlockProcessors.HalfPixelDouble;
import net.imglib2.blk.downsample.DownsampleBlockProcessors.HalfPixelFloat;
import net.imglib2.blk.downsample.algo.DefaultUnaryBlockOperator;
import net.imglib2.blk.downsample.algo.UnaryBlockOperator;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import static net.imglib2.type.PrimitiveType.FLOAT;

public class Downsample
{
	public static long[] getDownsampledDimensions( final long[] imgDimensions, final boolean[] downsampleInDim )
	{
		final int n = imgDimensions.length;
		if ( downsampleInDim.length != n )
			throw new IllegalArgumentException();
		final long[] destSize = new long[ n ];
		Arrays.setAll( destSize, d -> downsampleInDim[ d ] ? ( imgDimensions[ d ] + 1 ) / 2 : imgDimensions[ d ] );
		return destSize;
	}

	public enum ComputationType
	{
		FLOAT, DOUBLE, AUTO
	}

	public enum Offset
	{
		CENTERED, HALF_PIXEL
	}

	public static < T extends NativeType< T > >
	UnaryBlockOperator< T, T > downsample( final T type, final ComputationType computationType, final Offset offset, final boolean[] downsampleInDim )
	{
		final boolean processAsFloat;
		switch ( computationType )
		{
		case FLOAT:
			processAsFloat = true;
			break;
		case DOUBLE:
			processAsFloat = false;
			break;
		default:
		case AUTO:
			final PrimitiveType pt = type.getNativeTypeFactory().getPrimitiveType();
			processAsFloat = pt.equals( FLOAT ) || pt.getByteCount() < FLOAT.getByteCount();
			break;
		}
		final UnaryBlockOperator< ?, ? > op = processAsFloat
				? downsampleFloat( offset, downsampleInDim )
				: downsampleDouble( offset, downsampleInDim );
		return op.adaptSourceType( type, ClampType.NONE ).adaptTargetType( type, ClampType.NONE );
	}

	public static < T extends NativeType< T > >
	UnaryBlockOperator< T, T > downsample( final T type, final ComputationType computationType, final Offset offset, final int numDimensions )
	{
		final boolean[] downsampleInDim = new boolean[ numDimensions ];
		Arrays.fill( downsampleInDim, true );
		return downsample( type, computationType, offset, downsampleInDim );
	}

	public static < T extends NativeType< T > >
	UnaryBlockOperator< T, T > downsample( final T type, final Offset offset, final int numDimensions )
	{
		return downsample( type, ComputationType.AUTO, offset, numDimensions );
	}

	public static < T extends NativeType< T > >
	UnaryBlockOperator< T, T > downsample( final T type, final int numDimensions )
	{
		return downsample( type, ComputationType.AUTO, Offset.HALF_PIXEL, numDimensions );
	}









	private static UnaryBlockOperator< FloatType, FloatType > downsampleFloat( Offset offset, final boolean[] downsampleInDim )
	{
		final FloatType type = new FloatType();
		return new DefaultUnaryBlockOperator<>( type, type,
				offset == Offset.HALF_PIXEL
						? new HalfPixelFloat( downsampleInDim )
						: new CenterFloat( downsampleInDim ) );
	}

	private static UnaryBlockOperator< DoubleType, DoubleType > downsampleDouble( Offset offset, final boolean[] downsampleInDim )
	{
		final DoubleType type = new DoubleType();
		return new DefaultUnaryBlockOperator<>( type, type,
				offset == Offset.HALF_PIXEL
						? new HalfPixelDouble( downsampleInDim )
						: new CenterDouble( downsampleInDim ) );
	}
}

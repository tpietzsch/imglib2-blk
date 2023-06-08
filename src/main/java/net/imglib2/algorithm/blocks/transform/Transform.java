package net.imglib2.algorithm.blocks.transform;

import net.imglib2.algorithm.blocks.DefaultUnaryBlockOperator;
import net.imglib2.algorithm.blocks.UnaryBlockOperator;
import net.imglib2.algorithm.blocks.convert.ClampType;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import static net.imglib2.type.PrimitiveType.FLOAT;

/**
 * TODO: javadoc
 */
public class Transform
{
	/**
	 * Specify in which precision should intermediate values be computed. (For
	 * {@code AUTO}, the type that can represent the input/output type without
	 * loss of precision is picked. That is, {@code FLOAT} for u8, i8, u16, i16,
	 * i32, f32, and otherwise {@code DOUBLE} for u32, i64, f64.
	 */
	public enum ComputationType
	{
		FLOAT, DOUBLE, AUTO
	}

	public enum Interpolation
	{
		NEARESTNEIGHBOR,
		NLINEAR;
	}




	// only 2D and 3D !

	/**
	 * TODO: javadoc
	 *
	 * @param type
	 * @param transformFromSource
	 * @param interpolation
	 * @param computationType
	 * @return
	 * @param <T>
	 */
	public static < T extends NativeType< T > >
	UnaryBlockOperator< T, T > affine( final T type, final AffineGet transformFromSource, Interpolation interpolation, final ComputationType computationType )
	{
		final int n = transformFromSource.numDimensions();
		if ( n < 2 || n > 3 ) {
			throw new IllegalArgumentException( "Only 2D and 3D affine transforms are supported currently" );
		}

		final AffineGet transformToSource = invert( transformFromSource );

		if ( interpolation == Interpolation.NLINEAR )
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
					? _affine( transformToSource, interpolation, new FloatType() )
					: _affine( transformToSource, interpolation, new DoubleType() );
			return op.adaptSourceType( type, ClampType.NONE ).adaptTargetType( type, ClampType.CLAMP );
		}
		else // if ( interpolation == Interpolation.NEARESTNEIGHBOR )
		{
			return _affine( transformToSource, interpolation, type );
		}
	}

	private static < T extends NativeType< T > > UnaryBlockOperator< T, T > _affine( final AffineGet transform, final Interpolation interpolation, final T type )
	{
		return new DefaultUnaryBlockOperator<>( type, type,
				transform.numDimensions() == 2
						? new Affine2DProcessor<>( ( AffineTransform2D ) transform, interpolation, type.getNativeTypeFactory().getPrimitiveType() )
						: new Affine3DProcessor<>( ( AffineTransform3D ) transform, interpolation, type.getNativeTypeFactory().getPrimitiveType() ) );
	}

	private static AffineGet invert( final AffineGet transformFromSource )
	{
		switch ( transformFromSource.numDimensions() )
		{
		case 2:
		{
			final AffineTransform2D transform = new AffineTransform2D();
			transform.set( transformFromSource.inverse().getRowPackedCopy() );
			return transform;
		}
		case 3:
		{
			final AffineTransform3D transform = new AffineTransform3D();
			transform.set( transformFromSource.inverse().getRowPackedCopy() );
			return transform;
		}
		default:
			throw new IllegalArgumentException();
		}
	}
}

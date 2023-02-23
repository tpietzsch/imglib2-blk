package net.imglib2.blk.view;

import java.util.Arrays;
import net.imglib2.blk.copy.MemCopy;
import net.imglib2.blk.copy.TempArray;
import net.imglib2.blk.copy.RangeCopier;
import net.imglib2.blk.copy.Ranges;
import net.imglib2.converter.Converter;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.transform.integer.Mixed;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.TransformBuilder;

public class ViewBlocks< T extends NativeType< T > >
{
	private final MemCopy memCopy;
	private final TempArray tempMem;
	private final ConvertBlock convertBlock;
	private final RangeCopier copier;
	private final TransformBlockCoords tCoords;

	public ViewBlocks( final ViewProps props, final T viewType )
	{
		final Object type = props.img.createLinkedType();
		final Object oob;
		if ( type instanceof UnsignedByteType )
		{
			memCopy = MemCopy.BYTE;
			tempMem = TempArray.forPrimitiveType( PrimitiveType.BYTE );
			final byte v = props.oobValue == null ? 0 : ( ( UnsignedByteType ) props.oobValue ).getByte();
			oob = new byte[] { v };
		}
		else if ( type instanceof UnsignedShortType )
		{
			memCopy = MemCopy.SHORT;
			tempMem = TempArray.forPrimitiveType( PrimitiveType.SHORT );
			final short v = props.oobValue == null ? 0 : ( ( UnsignedShortType ) props.oobValue ).getShort();
			oob = new short[] { v };
		}
		else if ( type instanceof FloatType )
		{
			memCopy = MemCopy.FLOAT;
			tempMem = TempArray.forPrimitiveType( PrimitiveType.FLOAT );
			final float v = props.oobValue == null ? 0 : ( ( FloatType ) props.oobValue ).get();
			oob = new float[] { v };
		}
		else if ( type instanceof DoubleType )
		{
			memCopy = MemCopy.DOUBLE;
			tempMem = TempArray.forPrimitiveType( PrimitiveType.DOUBLE );
			final double v = props.oobValue == null ? 0 : ( ( DoubleType ) props.oobValue ).get();
			oob = new double[] { v };
		}
		else
			throw new IllegalArgumentException( type.getClass() + " is not supported" );

		final Ranges findRanges = Ranges.forExtension( props.extension );
		copier = RangeCopier.create( props.img, findRanges, memCopy, oob );
		tCoords = new TransformBlockCoords( props.transform );
		convertBlock = ( props.converter == null )
				? null
				: new ConvertBlock( ( NativeType ) type, viewType, props.converter );
	}

	public void copy( final int[] srcPos, final Object dest, final int[] size )
	{
		if ( convertBlock == null )
			copier.copy( tCoords.transformSrcPos( srcPos ), dest, tCoords.transformSize( size ) );
		else
		{
			final int length = ( int ) Intervals.numElements( size );
			final Object temp = tempMem.get( length );
			copier.copy( tCoords.transformSrcPos( srcPos ), temp, tCoords.transformSize( size ) );
			convertBlock.convert( temp, dest, length );
		}
	}

	static class ConvertBlock< I extends NativeType< I >, O extends NativeType< O > >
	{
		private final ConvertBlock.Wrapper< I > inWrapper;

		private final ConvertBlock.Wrapper< O > outWrapper;

		private final Converter< I, O > converter;

		ConvertBlock( final I inType, final O outType, final Converter< I, O > converter )
		{
			inWrapper = ConvertBlock.Wrapper.forType( inType );
			outWrapper = ConvertBlock.Wrapper.forType( outType );
			this.converter = converter;
		}

		void convert( final Object src, final Object dest, final int length )
		{
			I in = inWrapper.wrap( src );
			O out = outWrapper.wrap( dest );
			for ( int i = 0; i < length; i++ )
			{
				in.index().set( i );
				out.index().set( i );
				converter.convert( in, out );
			}
		}

		interface Wrapper< T extends NativeType< T > >
		{
			T wrap( Object array );

			static < T extends NativeType< T > > ConvertBlock.Wrapper< T > forType( T type )
			{
				if ( type instanceof UnsignedByteType )
				{
					return array -> ( T ) new UnsignedByteType( new ByteArray( ( byte[] ) array ) );
				}
				else if ( type instanceof UnsignedShortType )
				{
					return array -> ( T ) new UnsignedShortType( new ShortArray( ( short[] ) array ) );
				}
				else if ( type instanceof FloatType )
				{
					return array -> ( T ) new FloatType( new FloatArray( ( float[] ) array ) );
				}
				else if ( type instanceof DoubleType )
				{
					return array -> ( T ) new DoubleType( new DoubleArray( ( double[] ) array ) );
				}
				else
					throw new UnsupportedOperationException( "not implemented yet" );

			}
		}
	}

	static class TransformBlockCoords
	{
		private final Mixed t;

		private final int[] tpos;

		private final int[] tsize;

		TransformBlockCoords( final Mixed transform )
		{
			if ( TransformBuilder.isIdentity( transform ) )
			{
				t = null;
				tpos = null;
				tsize = null;
			}
			else
			{
				t = transform;
				tpos = new int[ t.numTargetDimensions() ];
				tsize = new int[ t.numTargetDimensions() ];
			}
		}

		int[] transformSrcPos( final int[] srcPos )
		{
			if ( t == null )
				return srcPos;
			else
			{
				t.apply( srcPos, tpos );
				return tpos;
			}
		}

		int[] transformSize( final int[] size )
		{
			if ( t == null )
				return size;
			else
			{
				Arrays.setAll( tsize,
						d -> t.getComponentZero( d ) ? 1 : size[ t.getComponentMapping( d ) ] );
				return tsize;
			}
		}
	}

}

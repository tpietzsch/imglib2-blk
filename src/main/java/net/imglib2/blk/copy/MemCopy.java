package net.imglib2.blk.copy;

import java.util.Arrays;
import net.imglib2.type.PrimitiveType;

// TODO javadoc
// low-level copying methods
// implementations for all primitive types
// T is a primitive array type
public interface MemCopy< T >
{
	/**
	 * Copy {@code length} components from the {@code src} array to the {@code
	 * dest} array. The components at positions {@code srcPos} through {@code
	 * srcPos+length-1} in the source array are copied into positions {@code
	 * destPos} through {@code destPos+length-1}, respectively, of the
	 * destination array.
	 */
	void copyForward( T src, int srcPos, T dest, int destPos, int length );

	/**
	 * Copy {@code length} components from the {@code src} array to the {@code
	 * dest} array, in reverse order. The components at positions {@code srcPos}
	 * through {@code srcPos-length-1} in the source array are copied into
	 * positions {@code destPos} through {@code destPos+length-1}, respectively,
	 * of the destination array.
	 */
	void copyReverse( T src, int srcPos, T dest, int destPos, int length );

	/**
	 * Copy component at position {@code srcPos} in the {@code src} array
	 * ({@code length} times) into positions {@code destPos} through {@code
	 * destPos+length-1} of the destination array.
	 */
	void copyValue( T src, int srcPos, T dest, int destPos, int length );

	/**
	 * TODO javadoc
	 */
	void copyStrided( T src, int srcPos, T dest, int destPos, int destStride, int length );

	MemCopyBoolean BOOLEAN = new MemCopyBoolean();
	MemCopyByte BYTE = new MemCopyByte();
	MemCopyChar CHAR = new MemCopyChar();
	MemCopyShort SHORT = new MemCopyShort();
	MemCopyInt INT = new MemCopyInt();
	MemCopyLong LONG = new MemCopyLong();
	MemCopyFloat FLOAT = new MemCopyFloat();
	MemCopyDouble DOUBLE = new MemCopyDouble();

	static MemCopy< ? > forPrimitiveType( final PrimitiveType primitiveType )
	{
		switch ( primitiveType )
		{
		case BOOLEAN:
			return BOOLEAN;
		case BYTE:
			return BYTE;
		case CHAR:
			return CHAR;
		case SHORT:
			return SHORT;
		case INT:
			return INT;
		case LONG:
			return LONG;
		case FLOAT:
			return FLOAT;
		case DOUBLE:
			return DOUBLE;
		default:
		case UNDEFINED:
			throw new IllegalArgumentException();
		}
	}

	class MemCopyBoolean implements MemCopy< boolean[] >
	{
		@Override
		public void copyForward( final boolean[] src, final int srcPos, final boolean[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final boolean[] src, final int srcPos, final boolean[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final boolean[] src, final int srcPos, final boolean[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}

		@Override
		public void copyStrided( final boolean[] src, final int srcPos, final boolean[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}

	class MemCopyByte implements MemCopy< byte[] >
	{
		@Override
		public void copyForward( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}

		@Override
		public void copyStrided( final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}

	class MemCopyShort implements MemCopy< short[] >
	{
		@Override
		public void copyForward( final short[] src, final int srcPos, final short[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final short[] src, final int srcPos, final short[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final short[] src, final int srcPos, final short[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}


		@Override
		public void copyStrided( final short[] src, final int srcPos, final short[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}

	class MemCopyChar implements MemCopy< char[] >
	{
		@Override
		public void copyForward( final char[] src, final int srcPos, final char[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final char[] src, final int srcPos, final char[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final char[] src, final int srcPos, final char[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}

		@Override
		public void copyStrided( final char[] src, final int srcPos, final char[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}

	class MemCopyInt implements MemCopy< int[] >
	{
		@Override
		public void copyForward( final int[] src, final int srcPos, final int[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final int[] src, final int srcPos, final int[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final int[] src, final int srcPos, final int[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}

		@Override
		public void copyStrided( final int[] src, final int srcPos, final int[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}

	class MemCopyLong implements MemCopy< long[] >
	{
		@Override
		public void copyForward( final long[] src, final int srcPos, final long[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final long[] src, final int srcPos, final long[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final long[] src, final int srcPos, final long[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}

		@Override
		public void copyStrided( final long[] src, final int srcPos, final long[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}

	class MemCopyFloat implements MemCopy< float[] >
	{
		@Override
		public void copyForward( final float[] src, final int srcPos, final float[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final float[] src, final int srcPos, final float[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final float[] src, final int srcPos, final float[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}

		@Override
		public void copyStrided( final float[] src, final int srcPos, final float[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}

	class MemCopyDouble implements MemCopy< double[] >
	{
		@Override
		public void copyForward( final double[] src, final int srcPos, final double[] dest, final int destPos, final int length )
		{
			System.arraycopy( src, srcPos, dest, destPos, length );
		}

		@Override
		public void copyReverse( final double[] src, final int srcPos, final double[] dest, final int destPos, final int length )
		{
			for ( int i = 0; i < length; ++i )
				dest[ destPos + i ] = src[ srcPos - i ];
		}

		@Override
		public void copyValue( final double[] src, final int srcPos, final double[] dest, final int destPos, final int length )
		{
			Arrays.fill( dest, destPos, destPos + length, src[ srcPos ] );
		}

		@Override
		public void copyStrided( final double[] src, final int srcPos, final double[] dest, final int destPos, final int destStride, final int length )
		{
			if ( destStride == 1 )
				copyForward( src, srcPos, dest, destPos, length );
			else
				for ( int i = 0; i < length; ++i )
					dest[ destPos + i * destStride ] = src[ srcPos + i ];
		}
	}
}

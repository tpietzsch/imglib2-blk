package net.imglib2.blk.downsample;

import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.Interval;
import net.imglib2.blk.downsample.algo.BlockProcessor;
import net.imglib2.blocks.TempArray;
import net.imglib2.util.Intervals;

import static net.imglib2.blk.downsample.TypeConversionGenerated.from_u8;
import static net.imglib2.blk.downsample.TypeConversionGenerated.to_f32;
import static net.imglib2.type.PrimitiveType.BYTE;

public class TypeConversion
{
	static class ConvertU8ToF32 implements BlockProcessor< byte[], float[] >
	{
		private final TempArray< byte[] > tempArray;

		private long[] sourcePos;

		private int[] sourceSize;

		private int sourceLength;

		ConvertU8ToF32()
		{
			tempArray = TempArray.forPrimitiveType( BYTE );
		}

		@Override
		public Supplier< ? extends BlockProcessor< byte[], float[] > > threadSafeSupplier()
		{
			throw new UnsupportedOperationException();
			// TODO
		}

		@Override
		public void setTargetInterval( final Interval interval )
		{
			final int n = interval.numDimensions();
			if ( sourcePos == null || sourcePos.length != n )
			{
				sourcePos = new long[ n ];
				sourceSize = new int[ n ];
			}
			interval.min( sourcePos );
			Arrays.setAll( sourceSize, d -> safeInt( interval.dimension( d ) ) );
			sourceLength = safeInt( Intervals.numElements( sourceSize ) );
		}

		static int safeInt( final long value )
		{
			if ( value > Integer.MAX_VALUE )
				throw new IllegalArgumentException( "value too large" );
			return ( int ) value;
		}

		@Override
		public long[] getSourcePos()
		{
			return sourcePos;
		}

		@Override
		public int[] getSourceSize()
		{
			return sourceSize;
		}

		@Override
		public byte[] getSourceBuffer()
		{
			return tempArray.get( sourceLength );
		}

		@Override
		public void compute( final byte[] src, final float[] dest )
		{
			for( int i = 0; i < sourceLength; ++i )
			{
				dest[ i ] = to_f32( from_u8( src[ i ] ) );
			}
		}
	}
}

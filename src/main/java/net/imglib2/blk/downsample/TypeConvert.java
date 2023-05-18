package net.imglib2.blk.downsample;

import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.Interval;
import net.imglib2.blk.downsample.algo.BlockProcessor;
import net.imglib2.blk.downsample.algo.BlockProcessorSourceInterval;
import net.imglib2.blocks.TempArray;
import net.imglib2.type.NativeType;
import net.imglib2.util.Intervals;

/**
 * @param <I>
 * 		input primitive array type, e.g., float[]
 * @param <O>
 * 		output primitive array type, e.g., float[]
 */
class TypeConvert< S extends NativeType< S >, T extends NativeType< T >, I, O > implements BlockProcessor< I, O >
{
	private final S sourceType;

	private final T targetType;

	private final TempArray< I > tempArray;

	private final ConvertLoop< I, O > loop;

	private Supplier< TypeConvert< S, T, I, O > > threadSafeSupplier;

	private long[] sourcePos;

	private int[] sourceSize;

	private int sourceLength;

	private final BlockProcessorSourceInterval sourceInterval;

	public TypeConvert( final S sourceType, final T targetType )
	{
		this( sourceType, targetType, ClampType.NONE );
	}

	public TypeConvert( final S sourceType, final T targetType, final ClampType clamp )
	{
		this.sourceType = sourceType;
		this.targetType = targetType;
		tempArray = TempArray.forPrimitiveType( sourceType.getNativeTypeFactory().getPrimitiveType() );
		loop = ConvertLoops.get( UnaryOperatorType.of( sourceType, targetType ), clamp );
		sourceInterval = new BlockProcessorSourceInterval( this );
	}

	private TypeConvert( TypeConvert< S, T, I, O > convert )
	{
		sourceType = convert.sourceType;
		targetType = convert.targetType;
		tempArray = convert.tempArray.newInstance();
		loop = convert.loop;
		sourceInterval = new BlockProcessorSourceInterval( this );
		threadSafeSupplier = convert.threadSafeSupplier;
	}

	private TypeConvert< S, T, I, O > newInstance()
	{
		return new TypeConvert<>( this );
	}

	@Override
	public Supplier< ? extends BlockProcessor< I, O > > threadSafeSupplier()
	{
		if ( threadSafeSupplier == null )
			threadSafeSupplier = ThreadLocal.withInitial( this::newInstance )::get;
		return threadSafeSupplier;
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

	private static int safeInt( final long value )
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
	public Interval getSourceInterval()
	{
		return sourceInterval;
	}

	@Override
	public I getSourceBuffer()
	{
		return tempArray.get( sourceLength );
	}

	@Override
	public void compute( final I src, final O dest )
	{
		loop.apply( src, dest, sourceLength );
	}
}

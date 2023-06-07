package net.imglib2.algorithm;

import java.util.Arrays;
import java.util.function.Supplier;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.algorithm.blocks.BlockProcessor;
import net.imglib2.algorithm.blocks.util.BlockProcessorSourceInterval;
import net.imglib2.blocks.TempArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Cast;
import net.imglib2.util.CloseableThreadLocal;
import net.imglib2.util.Intervals;

public class UnifyPlayground
{
	public enum Interpolation
	{
		NEARESTNEIGHBOR,
		NLINEAR;
	}

	static abstract class AbstractTransformProcessor< T extends AbstractTransformProcessor< T, P >, P > implements BlockProcessor< P, P >
	{
		PrimitiveType primitiveType;
		Interpolation interpolation;

		final int n;
		final long[] destPos;
		final int[] destSize;
		final long[] sourcePos;

		final int[] sourceSize;

		private int sourceLength;

		private final BlockProcessorSourceInterval sourceInterval;
		private final TempArray< P > tempArray;
		Supplier< T > threadSafeSupplier;

		AbstractTransformProcessor(final int n, final Interpolation interpolation, final PrimitiveType primitiveType )
		{
			this.primitiveType = primitiveType;
			this.interpolation = interpolation;
			this.n = n;
			destPos = new long[ n ];
			destSize = new int[ n ];
			sourcePos = new long[ n ];
			sourceSize = new int[ n ];
			sourceInterval = new BlockProcessorSourceInterval( this );
			tempArray = TempArray.forPrimitiveType( primitiveType );
		}

		AbstractTransformProcessor( T transform )
		{
			// re-use
			primitiveType = transform.primitiveType;
			interpolation = transform.interpolation;
			n = transform.n;
			threadSafeSupplier = transform.threadSafeSupplier;

			// init empty
			destPos = new long[ n ];
			destSize = new int[ n ];
			sourcePos = new long[ n ];
			sourceSize = new int[ n ];

			// init new instance
			sourceInterval = new BlockProcessorSourceInterval( this );
			tempArray = TempArray.forPrimitiveType( primitiveType );
		}

		abstract T newInstance();

		@Override
		public Supplier< T > threadSafeSupplier()
		{
			if ( threadSafeSupplier == null )
				threadSafeSupplier = CloseableThreadLocal.withInitial( this::newInstance )::get;
			return threadSafeSupplier;
		}

		abstract RealInterval estimateBounds( Interval interval );

		@Override
		public void setTargetInterval( final Interval interval )
		{
			interval.min( destPos );
			Arrays.setAll( destSize, d -> ( int ) interval.dimension( d ) );

			final RealInterval bounds = estimateBounds( interval );
			switch ( interpolation )
			{
			case NEARESTNEIGHBOR:
				Arrays.setAll( sourcePos, d -> Math.round( bounds.realMin( d ) ) );
				Arrays.setAll( sourceSize, d -> ( int ) ( Math.round( bounds.realMax( d ) ) - sourcePos[ d ] ) + 1 );
				break;
			case NLINEAR:
				Arrays.setAll( sourcePos, d -> ( long ) Math.floor( bounds.realMin( d ) ) );
				Arrays.setAll( sourceSize, d -> ( int ) ( ( long ) Math.floor( bounds.realMax( d ) ) - sourcePos[ d ] ) + 2 );
				break;
			}
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
		public Interval getSourceInterval()
		{
			return sourceInterval;
		}

		@Override
		public P getSourceBuffer()
		{
			return tempArray.get( sourceLength );
		}
	}



	static class Affine3DProcessor< P > extends AbstractTransformProcessor< Affine3DProcessor< P >, P >
	{
		private final AffineTransform3D transformToSource;
		private final TransformLine3D< P > transformLine;
		private final double pdest[] = new double[ 3 ];
		private final double psrc[] = new double[ 3 ];

		Affine3DProcessor(
				final AffineTransform3D transformToSource,
				final Interpolation interpolation,
				final PrimitiveType primitiveType )
		{
			this( transformToSource, interpolation, primitiveType, TransformLine3D.of( interpolation, primitiveType ) );
		}

		private Affine3DProcessor(
				final AffineTransform3D transformToSource,
				final Interpolation interpolation,
				final PrimitiveType primitiveType,
				final TransformLine3D< P > transformLine )
		{
			super( 3, interpolation, primitiveType );
			this.transformToSource = transformToSource;
			this.transformLine = transformLine;
		}

		private Affine3DProcessor( Affine3DProcessor< P > processor )
		{
			super( processor );
			transformToSource = processor.transformToSource;
			transformLine = processor.transformLine;
		}

		@Override
		Affine3DProcessor newInstance()
		{
			return new Affine3DProcessor( this );
		}

		@Override
		RealInterval estimateBounds( final Interval interval )
		{
			return transformToSource.estimateBounds( interval );
		}

		// specific to 3D
		@Override
		public void compute( final P src, final P dest )
		{
			final float d0 = transformToSource.d( 0 ).getFloatPosition( 0 );
			final float d1 = transformToSource.d( 0 ).getFloatPosition( 1 );
			final float d2 = transformToSource.d( 0 ).getFloatPosition( 2 );
			final int ds0 = destSize[ 0 ];
			final int ss0 = sourceSize[ 0 ];
			final int ss1 = sourceSize[ 1 ] * ss0;
			pdest[ 0 ] = destPos[ 0 ];
			int i = 0;
			for ( int z = 0; z < destSize[ 2 ]; ++z )
			{
				pdest[ 2 ] = z + destPos[ 2 ];
				for ( int y = 0; y < destSize[ 1 ]; ++y )
				{
					pdest[ 1 ] = y + destPos[ 1 ];
					transformToSource.apply( pdest, psrc );
					float sf0 = ( float ) ( psrc[ 0 ] - sourcePos[ 0 ] );
					float sf1 = ( float ) ( psrc[ 1 ] - sourcePos[ 1 ] );
					float sf2 = ( float ) ( psrc[ 2 ] - sourcePos[ 2 ] );
					transformLine.apply( src, dest, i, ds0, d0, d1, d2, ss0, ss1, sf0, sf1, sf2 );
					i += ds0;
				}
			}
		}

		/**
		 * Compute a destination X line for 3D.
		 *
		 * @param <P>
		 * 		input/output primitive array type (i.e., float[] or double[])
		 */
		@FunctionalInterface
		interface TransformLine3D< P > {

			/**
			 *
			 * @param src flattened source data
			 * @param dest flattened dest data
			 * @param offset offset (into {@code dest}) of the line to compute
			 * @param length length of the line to compute (in {@code dest})
			 * @param d0 partial differential vector in X of the transform (X component)
			 * @param d1 partial differential vector in X of the transform (Y component)
			 * @param d2 partial differential vector in X of the transform (Z component)
			 * @param ss0 length of a source line (size_X)
			 * @param ss1 length of a source plane (size_X * size_Y)
			 * @param sf0 position of the first sample on the line (transformed into source)
			 * @param sf1 position of the first sample on the line (transformed into source)
			 * @param sf2 position of the first sample on the line (transformed into source)
			 */
			void apply( P src, P dest, int offset, int length,
					float d0, float d1, float d2,
					int ss0, int ss1,
					float sf0, float sf1, float sf2 );

			static < P > TransformLine3D<P> of(
					final Interpolation interpolation,
					final PrimitiveType primitiveType )
			{
				switch ( primitiveType )
				{
				case FLOAT:
					return Cast.unchecked( interpolation == Interpolation.NLINEAR
							? NLinearFloat.INSTANCE
							: NearestNeighborFloat.INSTANCE );
				case DOUBLE:
					return Cast.unchecked( interpolation == Interpolation.NLINEAR
							? NLinearDouble.INSTANCE
							: NearestNeighborDouble.INSTANCE );
				default:
					throw new IllegalArgumentException();
				}
			}
		}

		private static class NearestNeighborFloat implements TransformLine3D< float[] >
		{
			private NearestNeighborFloat() {}

			static final NearestNeighborFloat INSTANCE = new NearestNeighborFloat();

			@Override
			public void apply( final float[] src, final float[] dest, int offset, final int length,
					final float d0,final float d1, final float d2,
					final int ss0, final int ss1,
					float sf0, float sf1, float sf2)
			{
				sf0 += .5f;
				sf1 += .5f;
				sf2 += .5f;
				for ( int x = 0; x < length; ++x )
				{
					final int s0 = ( int ) sf0;
					final int s1 = ( int ) sf1;
					final int s2 = ( int ) sf2;
					dest[ offset++ ] = src[ s2 * ss1 + s1 * ss0 + s0 ];
					sf0 += d0;
					sf1 += d1;
					sf2 += d2;
				}
			}
		}

		private static class NearestNeighborDouble implements TransformLine3D< double[] >
		{
			private NearestNeighborDouble() {}

			static final NearestNeighborDouble INSTANCE = new NearestNeighborDouble();

			@Override
			public void apply( final double[] src, final double[] dest, int offset, final int length,
					final float d0,final float d1, final float d2,
					final int ss0, final int ss1,
					float sf0, float sf1, float sf2)
			{
				sf0 += .5f;
				sf1 += .5f;
				sf2 += .5f;
				for ( int x = 0; x < length; ++x )
				{
					final int s0 = ( int ) sf0;
					final int s1 = ( int ) sf1;
					final int s2 = ( int ) sf2;
					dest[ offset++ ] = src[ s2 * ss1 + s1 * ss0 + s0 ];
					sf0 += d0;
					sf1 += d1;
					sf2 += d2;
				}
			}
		}

		private static class NLinearFloat implements TransformLine3D< float[] >
		{
			private NLinearFloat() {}

			static final NLinearFloat INSTANCE = new NLinearFloat();

			@Override
			public void apply( final float[] src, final float[] dest, int offset, final int length,
					final float d0, final float d1, final float d2,
					final int ss0, final int ss1,
					float sf0, float sf1, float sf2 )
			{
				for ( int x = 0; x < length; ++x )
				{
					final int s0 = ( int ) sf0;
					final int s1 = ( int ) sf1;
					final int s2 = ( int ) sf2;
					final float r0 = sf0 - s0;
					final float r1 = sf1 - s1;
					final float r2 = sf2 - s2;
					final int o = s2 * ss1 + s1 * ss0 + s0;
					final float a000 = src[ o ];
					final float a001 = src[ o + 1 ];
					final float a010 = src[ o + ss0 ];
					final float a011 = src[ o + ss0 + 1 ];
					final float a100 = src[ o + ss1 ];
					final float a101 = src[ o + ss1 + 1 ];
					final float a110 = src[ o + ss1 + ss0 ];
					final float a111 = src[ o + ss1 + ss0 + 1 ];
					dest[ offset++ ] = a000 +
							r0 * ( -a000 + a001 ) +
							r1 * ( ( -a000 + a010 ) +
									r0 * ( a000 - a001 - a010 + a011 ) ) +
							r2 * ( ( -a000 + a100 ) +
									r0 * ( a000 - a001 - a100 + a101 ) +
									r1 * ( ( a000 - a010 - a100 + a110 ) +
											r0 * ( -a000 + a001 + a010 - a011 + a100 - a101 - a110 + a111 ) ) );
					sf0 += d0;
					sf1 += d1;
					sf2 += d2;
				}
			}
		}

		private static class NLinearDouble implements TransformLine3D< double[] >
		{
			private NLinearDouble() {}

			static final NLinearDouble INSTANCE = new NLinearDouble();

			@Override
			public void apply( final double[] src, final double[] dest, int offset, final int length,
					final float d0, final float d1, final float d2,
					final int ss0, final int ss1,
					float sf0, float sf1, float sf2 )
			{
				for ( int x = 0; x < length; ++x )
				{
					final int s0 = ( int ) sf0;
					final int s1 = ( int ) sf1;
					final int s2 = ( int ) sf2;
					final double r0 = sf0 - s0;
					final double r1 = sf1 - s1;
					final double r2 = sf2 - s2;
					final int o = s2 * ss1 + s1 * ss0 + s0;
					final double a000 = src[ o ];
					final double a001 = src[ o + 1 ];
					final double a010 = src[ o + ss0 ];
					final double a011 = src[ o + ss0 + 1 ];
					final double a100 = src[ o + ss1 ];
					final double a101 = src[ o + ss1 + 1 ];
					final double a110 = src[ o + ss1 + ss0 ];
					final double a111 = src[ o + ss1 + ss0 + 1 ];
					dest[ offset++ ] = a000 +
							r0 * ( -a000 + a001 ) +
							r1 * ( ( -a000 + a010 ) +
									r0 * ( a000 - a001 - a010 + a011 ) ) +
							r2 * ( ( -a000 + a100 ) +
									r0 * ( a000 - a001 - a100 + a101 ) +
									r1 * ( ( a000 - a010 - a100 + a110 ) +
											r0 * ( -a000 + a001 + a010 - a011 + a100 - a101 - a110 + a111 ) ) );
					sf0 += d0;
					sf1 += d1;
					sf2 += d2;
				}
			}
		}
	}






	static class Affine2DProcessor< P > extends AbstractTransformProcessor< Affine2DProcessor< P >, P >
	{
		private final AffineTransform2D transformToSource;

		private final TransformLine2D< P > transformLine;

		private final double pdest[] = new double[ 2 ];

		private final double psrc[] = new double[ 2 ];

		Affine2DProcessor(
				final AffineTransform2D transformToSource,
				final Interpolation interpolation,
				final PrimitiveType primitiveType )
		{
			this( transformToSource, interpolation, primitiveType, TransformLine2D.of( interpolation, primitiveType ) );
		}

		private Affine2DProcessor(
				final AffineTransform2D transformToSource,
				final Interpolation interpolation,
				final PrimitiveType primitiveType,
				final TransformLine2D< P > transformLine )
		{
			super( 2, interpolation, primitiveType );
			this.transformToSource = transformToSource;
			this.transformLine = transformLine;
		}

		private Affine2DProcessor( Affine2DProcessor< P > processor )
		{
			super( processor );
			transformToSource = processor.transformToSource;
			transformLine = processor.transformLine;
		}

		@Override
		Affine2DProcessor newInstance()
		{
			return new Affine2DProcessor( this );
		}

		@Override
		RealInterval estimateBounds( final Interval interval )
		{
			return transformToSource.estimateBounds( interval );
		}

		// specific to 3D
		@Override
		public void compute( final P src, final P dest )
		{
			final float d0 = transformToSource.d( 0 ).getFloatPosition( 0 );
			final float d1 = transformToSource.d( 0 ).getFloatPosition( 1 );
			final int ds0 = destSize[ 0 ];
			final int ss0 = sourceSize[ 0 ];
			pdest[ 0 ] = destPos[ 0 ];
			int i = 0;
			for ( int y = 0; y < destSize[ 1 ]; ++y )
			{
				pdest[ 1 ] = y + destPos[ 1 ];
				transformToSource.apply( pdest, psrc );
				float sf0 = ( float ) ( psrc[ 0 ] - sourcePos[ 0 ] );
				float sf1 = ( float ) ( psrc[ 1 ] - sourcePos[ 1 ] );
				transformLine.apply( src, dest, i, ds0, d0, d1, ss0, sf0, sf1 );
				i += ds0;
			}
		}

		/**
		 * Compute a destination X line for 3D.
		 *
		 * @param <P>
		 * 		input/output primitive array type (i.e., float[] or double[])
		 */
		@FunctionalInterface
		interface TransformLine2D< P >
		{

			/**
			 * @param src
			 * 		flattened source data
			 * @param dest
			 * 		flattened dest data
			 * @param offset
			 * 		offset (into {@code dest}) of the line to compute
			 * @param length
			 * 		length of the line to compute (in {@code dest})
			 * @param d0
			 * 		partial differential vector in X of the transform (X component)
			 * @param d1
			 * 		partial differential vector in X of the transform (Y component)
			 * @param ss0
			 * 		length of a source line (size_X)
			 * @param sf0
			 * 		position of the first sample on the line (transformed into source)
			 * @param sf1
			 * 		position of the first sample on the line (transformed into source)
			 */
			void apply( P src, P dest, int offset, int length,
					float d0, float d1,
					int ss0,
					float sf0, float sf1 );

			static < P > TransformLine2D< P > of(
					final Interpolation interpolation,
					final PrimitiveType primitiveType )
			{
				switch ( primitiveType )
				{
				case FLOAT:
					TODO make these implementations:
//					return Cast.unchecked( interpolation == Interpolation.NLINEAR
//							? NLinearFloat.INSTANCE
//							: NearestNeighborFloat.INSTANCE );
				case DOUBLE:
//					return Cast.unchecked( interpolation == Interpolation.NLINEAR
//							? NLinearDouble.INSTANCE
//							: NearestNeighborDouble.INSTANCE );
				default:
					throw new IllegalArgumentException();
				}
			}
		}
	}

}

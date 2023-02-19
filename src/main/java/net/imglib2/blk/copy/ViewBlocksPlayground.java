package net.imglib2.blk.copy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.blk.copy.ViewNodeImpl.DefaultViewNode;
import net.imglib2.blk.copy.ViewNodeImpl.ExtensionViewNode;
import net.imglib2.blk.copy.ViewNodeImpl.MixedTransformViewNode;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.NativeImg;
import net.imglib2.img.WrappedImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.transform.integer.BoundingBox;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class ViewBlocksPlayground
{
	private final List< ViewNode > nodes = new ArrayList<>();

	private void analyze( final RandomAccessible< ? > rai )
	{
		RandomAccessible< ? > source = rai;
		while ( source != null )
		{
			// NATIVE_IMG,
			if ( source instanceof NativeImg )
			{
				final NativeImg< ?, ? > view = ( NativeImg< ?, ? > ) source;
				nodes.add( new DefaultViewNode( ViewNode.ViewType.NATIVE_IMG, view ) );
				source = null;
			}
			// IDENTITY,
			else if ( source instanceof WrappedImg )
			{
				final WrappedImg< ? > view = ( WrappedImg< ? > ) source;
				nodes.add( new DefaultViewNode( ViewNode.ViewType.IDENTITY, source ) );
				source = view.getImg();
			}
			else if ( source instanceof ImgView )
			{
				final ImgView< ? > view = ( ImgView< ? > ) source;
				nodes.add( new DefaultViewNode( ViewNode.ViewType.IDENTITY, view ) );
				source = view.getSource();
			}
			// INTERVAL,
			else if ( source instanceof IntervalView )
			{
				final IntervalView< ? > view = ( IntervalView< ? > ) source;
				nodes.add( new DefaultViewNode( ViewNode.ViewType.INTERVAL, view ) );
				source = view.getSource();
			}
			// CONVERTER,
			else if ( source instanceof ConvertedRandomAccessible )
			{
				final ConvertedRandomAccessible< ?, ? > view = ( ConvertedRandomAccessible< ?, ? > ) source;
				nodes.add( new DefaultViewNode( ViewNode.ViewType.CONVERTER, view ) );
				source = view.getSource();
			}
			else if ( source instanceof ConvertedRandomAccessibleInterval )
			{
				final ConvertedRandomAccessibleInterval< ?, ? > view = ( ConvertedRandomAccessibleInterval< ?, ? > ) source;
				nodes.add( new DefaultViewNode( ViewNode.ViewType.CONVERTER, view ) );
				source = view.getSource();
			}
			// MIXED_TRANSFORM,
			else if ( source instanceof MixedTransformView )
			{
				final MixedTransformView< ? > view = ( MixedTransformView< ? > ) source;
				nodes.add( new MixedTransformViewNode( view ) );
				source = view.getSource();
			}
			// EXTENSION
			else if ( source instanceof ExtendedRandomAccessibleInterval )
			{
				ExtendedRandomAccessibleInterval< ?, ? > view = ( ExtendedRandomAccessibleInterval< ?, ? > ) source;
				nodes.add( new ExtensionViewNode( view ) );
				source = view.getSource();
			}
			// fallback
			else
			{
				// TODO
				throw new IllegalArgumentException( "Cannot handle " + source );
			}
		}
	}

	private boolean checkRootSupported()
	{
		final ViewNode root = nodes.get( nodes.size() - 1 );
		if ( root.viewType() != ViewNode.ViewType.NATIVE_IMG )
			return false;
		return ( root.view() instanceof PlanarImg )
				|| ( root.view() instanceof ArrayImg )
				|| ( root.view() instanceof AbstractCellImg );
	}

	private boolean checkConverters()
	{
		// Rule: There must be no converters

		// TODO: Add the ability to use Converters.
		//       OutOfBounds extension needs to happen before Converter is
		//       applied. (Or Converter needs to be invertible). Multiple
		//       Converters could be chained. This is independent of the
		//       coordinate transforms.

		return nodes.stream().noneMatch( node -> node.viewType() == ViewNode.ViewType.CONVERTER );
	}

	private int oobIndex = -1;

	private Extension oobExtension = null;

	private boolean checkExtensions1()
	{
		// Rule: There must be at most one extension

		// TODO: This could be softened to allow for Extend nodes that are
		//       "swallowed" by subsequent Extend nodes.

		oobIndex = -1;
		for ( int i = 0; i < nodes.size(); i++ )
		{
			if ( nodes.get( i ).viewType() == ViewNode.ViewType.EXTENSION )
			{
				if ( oobIndex < 0 )
					oobIndex = i;
				else // this is already the second EXTENSION
					return false;
			}
		}

		if (oobIndex >= 0)
		{
			final ExtensionViewNode node = ( ExtensionViewNode ) nodes.get( oobIndex );
			oobExtension = Extension.of( node.getOutOfBoundsFactory() );
		}
		return true;
	}

	private boolean checkExtensions2()
	{
		if ( oobIndex < 0 ) // there is no extension
			return true;

		return oobExtension.type() != Extension.Type.UNKNOWN;
	}

	private boolean checkExtensions3()
	{
		// Rule: At the EXTENSION node, the interval must be equal to the root
		//       interval carried through the transforms so far. This means that
		//       the EXTENSION can be applied to the root directly (assuming
		//       that extension method is the same for every axis.)

		// TODO: This could be softened to allow intervals that are fully
		//       contained in the bottom interval. This would require revising
		//       the Ranges.findRanges() implementations.

		if ( oobIndex < 0 ) // there is no extension
			return true;

		BoundingBox bbExtension = nodes.get( oobIndex + 1 ).bbox();
		BoundingBox bb = nodes.get( nodes.size() - 1 ).bbox();
		System.out.println( "bb.getInterval() = " + bb.getInterval() );
		for ( int i = nodes.size() - 1; i > oobIndex; --i )
		{
			final ViewNode node = nodes.get( i );

			// all other view types are ignored.
			if ( node.viewType() == ViewNode.ViewType.MIXED_TRANSFORM )
			{
				final MixedTransform t = ( ( MixedTransformViewNode ) node ).getTransformToSource();
				bb = transformInverse( t, bb );
			}
		}

		return Intervals.equals( bb.getInterval(), bbExtension.getInterval() );
	}

	/**
	 * Apply the inverse transform to a target vector to obtain a source vector.
	 *
	 * @param source
	 *            set this to the source coordinates.
	 * @param target
	 *            target coordinates.
	 */
	private static void applyInverse( MixedTransform transform, long[] source, long[] target )
	{
		assert source.length >= transform.numSourceDimensions();
		assert target.length >= transform.numSourceDimensions();

		for ( int d = 0; d < transform.numTargetDimensions(); ++d )
		{
			if ( !transform.getComponentZero( d ) )
			{
				long v = target[ d ] - transform.getTranslation( d );
				source[ transform.getComponentMapping( d ) ] = transform.getComponentInversion( d ) ? -v : v;
			}
		}
	}

	private static BoundingBox transformInverse( final MixedTransform transform, final BoundingBox boundingBox )
	{
		assert boundingBox.numDimensions() == transform.numSourceDimensions();

		if ( transform.numSourceDimensions() == transform.numTargetDimensions() )
		{ // apply in-place
			final long[] tmp = new long[ transform.numTargetDimensions() ];
			boundingBox.corner1( tmp );
			applyInverse( transform, boundingBox.corner1, tmp );
			boundingBox.corner2( tmp );
			applyInverse( transform, boundingBox.corner2, tmp );
			return boundingBox;
		}
		final BoundingBox b = new BoundingBox( transform.numSourceDimensions() );
		applyInverse( transform, b.corner1, boundingBox.corner1 );
		applyInverse( transform, b.corner2, boundingBox.corner2 );
		return b;
	}



	/**
	 * Array {@code component}, of length {@code numTargetDimensions}, contains
	 * for every target dimension {@code i} the source dimensions {@code
	 * component[i]} from which it originates.
	 *
	 * Returns an array {@code invComponent}, of length {@code
	 * numSourceDimensions}, that contains for every source dimension {@code i}
	 * the target dimension {@code invComponent[i]} which it maps to.
	 */
	private static int[] invComponent( final int[] component, final int numSourceDimensions )
	{
		final int[] invComponent = new int[ numSourceDimensions ];
		Arrays.fill( invComponent, -1 );
		for ( int i = 0; i < component.length; i++ )
		{
			final int s = component[ i ];
			if ( s >= 0 )
				invComponent[ s ] = i;
		}
		return invComponent;
	}

	/**
	 * Computes the inverse of (@code transform}. The {@code MixedTransform
	 * transform} is a pure axis permutation followed by inversion of some axes,
	 * that is
	 * <ul>
	 * <li>{@code numSourceDimensions == numTargetDimensions},</li>
	 * <li>the translation vector is zero, and</li>
	 * <li>no target component is zeroed out.</li>
	 * </ul>
	 * The computed inverse {@code MixedTransform} concatenates with {@code transform} to identity.
	 * @return the inverse {@code MixedTransform}
	 */
	private static MixedTransform invPermutationInversion( MixedTransform transform )
	{
		final int n = transform.numTargetDimensions();
		final int[] component = new int[ n ];
		final boolean[] invert = new boolean[ n ];
		final boolean[] zero = new boolean[ n ];
		transform.getComponentMapping( component );
		transform.getComponentInversion( invert );
		transform.getComponentZero( zero );

		final int m = transform.numSourceDimensions();
		final int[] invComponent = new int[ m ];
		final boolean[] invInvert = new boolean[ m ];
		final boolean[] invZero = new boolean[ m ];
		Arrays.fill( invZero, true );
		for ( int i = 0; i < n; i++ )
		{
			if ( transform.getComponentZero( i ) == false )
			{
				final int j = component[ i ];
				invComponent[ j ] = i;
				invInvert[ j ] = invert[ i ];
				invZero[ j ] = false;
			}
		}
		MixedTransform invTransform = new MixedTransform( n, m );
		invTransform.setComponentMapping( invComponent );
		invTransform.setComponentInversion( invInvert );
		invTransform.setComponentZero( invZero );
		return invTransform;
	}

	/**
	 * Returns {@code true} iff no element of {@code component} is {@code < 0}.
	 */
	private static boolean isFullMapping( final int[] component )
	{
		for ( int t : component )
			if ( t < 0 )
				return false;
		return true;
	}

	private static class IntPair
	{
		final int i0;
		final int i1;
		private IntPair( final int i0, final int i1 )
		{
			this.i0 = i0;
			this.i1 = i1;
		}
	}

	private static List<IntPair> toIndexAndTargetDimensionPairs( final int[] component )
	{
		final List< IntPair > pairs = new ArrayList<>( component.length );
		for ( int i = 0; i < component.length; i++ )
		{
			pairs.add( new IntPair( i, component[ i ] ) );
		}
		pairs.sort( Comparator.comparingInt( pair -> pair.i1 ) );
		return pairs;
	}

	private static int[] getComponents( final int componentIndex, final List< IntPair > pairs )
	{
		final int[] components = new int[ pairs.size() ];
		Arrays.setAll( components, componentIndex == 0
				? i -> pairs.get( i ).i0
				: i -> pairs.get( i ).i1 );
		return components;
	}



	static RandomAccessible< ? > example1()
	{
 		RandomAccessibleInterval< UnsignedByteType > img0 = ArrayImgs.unsignedBytes( 640, 480, 3 );
		RandomAccessibleInterval< UnsignedByteType > img1 = Views.rotate( img0, 1, 0 );
		RandomAccessibleInterval< UnsignedByteType > img2 = Views.zeroMin( img1 );
		RandomAccessible< UnsignedByteType > img3 = Views.extendBorder( img2 );
		RandomAccessible< UnsignedByteType > img4 = Views.hyperSlice( img3, 2, 1 );
//		RandomAccessibleInterval< UnsignedByteType > img3 = Views.hyperSlice( img2, 2, 1 );
//		RandomAccessible< UnsignedByteType > img4 = Views.extendBorder( img3 );
		return img4;
	}

	public static void main( String[] args )
	{
		System.out.println( "hello world!" );
		final RandomAccessible< ? > rai = example1();
		final ViewBlocksPlayground playground = new ViewBlocksPlayground();
		playground.analyze( rai );
		for ( int i = 0; i < playground.nodes.size(); i++ )
		{
			ViewNode node = playground.nodes.get( i );
			System.out.println( i + ": " + node );
		}

		System.out.println( "playground.checkRootSupported() = " + playground.checkRootSupported() );
		System.out.println( "playground.checkConverters() = " + playground.checkConverters() );
		System.out.println( "playground.checkExtensions1() = " + playground.checkExtensions1() );
		System.out.println( "playground.oobIndex = " + playground.oobIndex );
		System.out.println( "playground.oobExtension.type() = " + playground.oobExtension.type() );
		System.out.println( "playground.checkExtensions2() = " + playground.checkExtensions2() );
		System.out.println( "playground.checkExtensions3() = " + playground.checkExtensions3() );

	}

}

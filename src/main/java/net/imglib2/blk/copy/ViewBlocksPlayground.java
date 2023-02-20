package net.imglib2.blk.copy;

import java.util.ArrayList;
import java.util.List;
import net.imglib2.Interval;
import net.imglib2.Point;
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
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.TransformBuilder;
import net.imglib2.view.Views;

public class ViewBlocksPlayground
{
	private final RandomAccessible< ? > ra;

	private final List< ViewNode > nodes = new ArrayList<>();

	ViewBlocksPlayground( final RandomAccessible< ? > rai )
	{
		this.ra = rai;
	}

	private void analyze()
	{
		RandomAccessible< ? > source = ra;
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

	private boolean checkRootTypeSupported()
	{
		final ViewNode root = nodes.get( nodes.size() - 1 );
		final NativeType< ? > type = ( NativeType< ? > ) ( ( NativeImg< ?, ? > ) root.view() ).createLinkedType();
		return type.getEntitiesPerPixel().getRatio() == 1;
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

	private MixedTransform transform;

	private void concatenateTransforms()
	{
		final int n = ra.numDimensions();
		transform = new MixedTransform( n, n );
		for ( ViewNode node : nodes )
		{
			if ( node.viewType() == ViewNode.ViewType.MIXED_TRANSFORM )
			{
				final MixedTransformViewNode tnode = ( MixedTransformViewNode ) node;
				transform = transform.preConcatenate( tnode.getTransformToSource() );
			}
		}
	}

	private boolean checkNoDimensionsAdded()
	{
		// Rule: No source dimension is unused. That is Views.addDimension(...)
		//       is not allowed for now.

		return transform.hasFullSourceMapping();
	}

	private MixedTransform permuteInvertTransform;

	private MixedTransform remainderTransform;

	private void splitTransform()
	{
		final MixedTransform[] split = PrimitiveBlocksUtils.split( transform );
		permuteInvertTransform = split[ 0 ];
		remainderTransform = split[ 1 ];
	}

	private boolean checkNoPermutationInversion()
	{
		// Rule: No axis permutation or inversion is

		return TransformBuilder.isIdentity( permuteInvertTransform );
	}

	private < T extends NativeType< T >, R extends NativeType< R > > ViewProperties< T, R > getViewProperties()
	{
		final T viewType = getType( ( RandomAccessible< T > ) ra );
		final NativeImg< R, ? > root = ( NativeImg< R, ? > ) nodes.get( nodes.size() - 1 ).view();
		final R rootType = root.createLinkedType();
		return new ViewProperties<>( viewType, root, rootType, oobExtension, transform );
	}

	// TODO replace by ra.getType() when that is available in imglib2 core
	private static < T extends Type< T > > T getType( RandomAccessible< T > ra )
	{
		final Point p = new Point( ra.numDimensions() );
		if ( ra instanceof Interval )
			( ( Interval ) ra ).min( p );
		return ra.getAt( p ).createVariable();
	}

	/**
	 *
	 * @param <T> type of the view {@code RandomAccessible}
	 * @param <R> type of the root {@code NativeImg}
	 */
	static class ViewProperties< T extends NativeType< T >, R extends NativeType< R > >
	{
		private final T viewType;

		private final NativeImg< R, ? > root;

		private final R rootType;

		private final Extension extension;

		private final MixedTransform transform;

		ViewProperties(
				final T viewType,
				final NativeImg< R, ? > root,
				final R rootType,
				final Extension extension,
				final MixedTransform transform )
		{
			this.viewType = viewType;
			this.root = root;
			this.rootType = rootType;
			this.extension = extension;
			this.transform = transform;
		}

		@Override
		public String toString()
		{
			return "ViewProperties{" +
					"viewType=" + viewType.getClass().getSimpleName() +
					", root=" + root +
					", rootType=" + rootType.getClass().getSimpleName() +
					", extension=" + extension +
					", transform=" + transform +
					'}';
		}
	}






	static RandomAccessible< ? > example1()
	{
//		RandomAccessibleInterval< UnsignedByteType > img0 = ArrayImgs.unsignedBytes( 640, 480, 3 );
//		RandomAccessibleInterval< UnsignedByteType > img1 = Views.rotate( img0, 1, 0 );
//		RandomAccessibleInterval< UnsignedByteType > img2 = Views.zeroMin( img1 );
//		RandomAccessible< UnsignedByteType > img3 = Views.extendBorder( img2 );
//		RandomAccessible< UnsignedByteType > img4 = Views.hyperSlice( img3, 2, 1 );
//		return img4;

//		RandomAccessibleInterval< UnsignedByteType > img3 = Views.hyperSlice( img2, 2, 1 );
//		RandomAccessible< UnsignedByteType > img4 = Views.extendBorder( img3 );
//		return img4;

		RandomAccessibleInterval< UnsignedByteType > img0 = ArrayImgs.unsignedBytes( 640, 480, 3 );
		RandomAccessibleInterval< UnsignedByteType > img1 = Views.hyperSlice( img0, 2, 1 );
		RandomAccessible< UnsignedByteType > img2 = Views.extendBorder( img1 );
		RandomAccessible< UnsignedByteType > img3 = Views.translate( img2, 100, 50 );
		return img3;
	}

	public static void main( String[] args )
	{
		System.out.println( "hello world!" );
		final RandomAccessible< ? > rai = example1();
		final ViewBlocksPlayground playground = new ViewBlocksPlayground( rai );
		playground.analyze();
		for ( int i = 0; i < playground.nodes.size(); i++ )
		{
			ViewNode node = playground.nodes.get( i );
			System.out.println( i + ": " + node );
		}

		System.out.println( "playground.checkRootSupported() = " + playground.checkRootSupported() );
		System.out.println( "playground.checkRootTypeSupported() = " + playground.checkRootTypeSupported() );
		System.out.println( "playground.checkConverters() = " + playground.checkConverters() );
		System.out.println( "playground.checkExtensions1() = " + playground.checkExtensions1() );
		System.out.println( "playground.oobIndex = " + playground.oobIndex );
		System.out.println( "playground.oobExtension.type() = " + playground.oobExtension );
		System.out.println( "playground.checkExtensions2() = " + playground.checkExtensions2() );
		System.out.println( "playground.checkExtensions3() = " + playground.checkExtensions3() );
		playground.concatenateTransforms();
		System.out.println( "playground.transform = " + playground.transform );
		System.out.println( "playground.checkNoDimensionsAdded() = " + playground.checkNoDimensionsAdded() );
		playground.splitTransform();
		System.out.println( "playground.permuteInvertTransform = " + playground.permuteInvertTransform );
		System.out.println( "playground.remainderTransform = " + playground.remainderTransform );
		System.out.println( "playground.checkNoPermutationInversion() = " + playground.checkNoPermutationInversion() );

		final ViewProperties< ?, ? > viewProperties = playground.getViewProperties();
		System.out.println( "viewProperties = " + viewProperties );
	}

}

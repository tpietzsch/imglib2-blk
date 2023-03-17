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
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.TransformBuilder;
import net.imglib2.view.Views;

public class ViewBlocksPlayground
{
	/**
	 * The View.
	 */
	private final RandomAccessible< ? > ra;

	/**
	 * View sequence of the target {@code RandomAccessible}. The first element
	 * is the target {@code RandomAccessible} itself. The last element is the
	 * source {@code NativeImg} where the View sequence originates.
	 */
	private final List< ViewNode > nodes = new ArrayList<>();

	private final StringBuilder errorDescription = new StringBuilder();

	ViewBlocksPlayground( final RandomAccessible< ? > ra )
	{
		this.ra = ra;
	}

	/**
	 * Deconstruct the View sequence of the target {@link #ra RandomAccessible}
	 * into a list of {@link #nodes ViewNodes}.
	 *
	 * @return {@code false}, if during the analysis a View type is encountered that can not be handled.
	 *         {@code true}, if everything went ok.
	 */
	private boolean analyze()
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
				errorDescription.append( "Cannot analyze view " + source + " of class " + source.getClass().getSimpleName() );
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the root of the View sequence is supported. Supported roots
	 * are {@code PlanarImg}, {@code ArrayImg}, and {@code CellImg} variants.
	 *
	 * @return {@code true}, if the root is supported.
	 */
	private boolean checkRootSupported()
	{
		final ViewNode root = nodes.get( nodes.size() - 1 );
		if ( root.viewType() != ViewNode.ViewType.NATIVE_IMG )
		{
			errorDescription.append( "The root of the View sequence must be a NativeImg. (Found "
					+ root.view() + " of class " + root.view().getClass().getSimpleName() + ")" );
			return false;
		}
		if ( ( root.view() instanceof PlanarImg )
				|| ( root.view() instanceof ArrayImg )
				|| ( root.view() instanceof AbstractCellImg ) )
		{
			return true;
		}
		else
		{
			errorDescription.append(
					"The root of the View sequence must be PlanarImg, ArrayImg, or AbstractCellImg. (Found "
							+ root.view() + " of class " + root.view().getClass().getSimpleName() + ")" );
			return false;
		}
	}

	/**
	 * Check whether the pixel {@code Type} of the root of the View sequence is
	 * supported. All {@code NativeType}s with {@code entitiesPerPixel==1} are
	 * supported.
	 *
	 * @return {@code true}, if the root's pixel type is supported.
	 */
	private boolean checkRootTypeSupported()
	{
		final ViewNode root = nodes.get( nodes.size() - 1 );
		final NativeType< ? > type = ( NativeType< ? > ) ( ( NativeImg< ?, ? > ) root.view() ).createLinkedType();
		if ( type.getEntitiesPerPixel().getRatio() == 1 )
		{
			return true;
		}
		else
		{
			errorDescription.append(
					"The pixel Type of root of the View sequence must be a NativeType with entitiesPerPixel==1. (Found "
							+ type.getClass().getSimpleName() + ")" );
			return false;
		}
	}

	/**
	 * TODO javadoc when finalized.
	 *
	 * @return
	 */
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

	/**
	 * The index of the out-of-bounds extension in {@link #nodes}.
	 */
	private int oobIndex = -1;

	/**
	 * The description of the out-of-bounds extension.
	 */
	private Extension oobExtension = null;


	/**
	 * Check whether there is at most one out-of-bounds extension.
	 * If an extension is found, store its index into {@link #oobIndex},
	 * and its description into {@link #oobExtension}.
	 *
	 * @return {@code true}, if there is at most one out-of-bounds extension.
	 *         {@code false}, otherwise
	 */
	private boolean checkExtensions1()
	{
		// TODO: This could be weakened to allow for extensions that are
		//       "swallowed" by subsequent extensions on a fully contained
		//       sub-interval (i.e., the earlier extension doesn't really do
		//       anything).

		oobIndex = -1;
		for ( int i = 0; i < nodes.size(); i++ )
		{
			if ( nodes.get( i ).viewType() == ViewNode.ViewType.EXTENSION )
			{
				if ( oobIndex < 0 )
					oobIndex = i;
				else
				{
					errorDescription.append( "There must be at most one out-of-bounds extension." );
					return false;
				}
			}
		}

		if (oobIndex >= 0)
		{
			final ExtensionViewNode node = ( ExtensionViewNode ) nodes.get( oobIndex );
			oobExtension = Extension.of( node.getOutOfBoundsFactory() );
		}
		return true;
	}

	/**
	 * Check whether the out-of-bounds extension (if any) is of a supported type
	 * (constant-value, border, mirror-single, mirror-double).
	 *
	 * @return {@code true}, if the out-of-bounds extension is of a supported
	 *         type, or if there is no extension.
	 */
	private boolean checkExtensions2()
	{
		// TODO: This could be weakened to allow for unknown extensions, by using
		//       fast copying for in-bounds regions, and fall-back for the rest.

		if ( oobIndex < 0 ) // there is no extension
			return true;

		if ( oobExtension.type() != Extension.Type.UNKNOWN )
		{
			return true;
		}
		else
		{
			final ExtensionViewNode node = ( ExtensionViewNode ) nodes.get( oobIndex );
			errorDescription.append(
					"Only constant-value, border, mirror-single, mirror-double out-of-bounds extensions are supported. (Found "
							+ node.getOutOfBoundsFactory().getClass().getSimpleName() + ")" );
			return false;
		}
	}

	/**
	 * Check whether the interval at the out-of-bounds extension is compatible.
	 * The interval must be equal to the root interval carried through the
	 * transforms so far. This means that the extension can be applied to the
	 * root directly (assuming that extension method is the same for every
	 * axis.)
	 *
	 * @return {@code true}, if the out-of-bounds extension interval is
	 *         compatible, or if there is no extension.
	 */
	private boolean checkExtensions3()
	{
		// TODO: This could be weakened to allow intervals that are fully
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
				bb = transform( t, bb );
			}
		}

		if ( Intervals.equals( bb.getInterval(), bbExtension.getInterval() ) )
		{
			return true;
		}
		else
		{
			errorDescription.append(
					"The interval at the out-of-bounds extension must be equal to the root interval carried through the transforms so far." );
			return false;
		}
	}

	/**
	 * Apply the {@code transformToSource} to a target vector to obtain a
	 * source vector.
	 *
	 * @param transformToSource
	 * 		the transformToSource from target to source.
	 * @param source
	 * 		set this to the source coordinates.
	 * @param target
	 * 		target coordinates.
	 */
	private static void apply( MixedTransform transformToSource, long[] source, long[] target )
	{
		assert source.length >= transformToSource.numSourceDimensions();
		assert target.length >= transformToSource.numSourceDimensions();

		for ( int d = 0; d < transformToSource.numTargetDimensions(); ++d )
		{
			if ( !transformToSource.getComponentZero( d ) )
			{
				long v = target[ d ] - transformToSource.getTranslation( d );
				source[ transformToSource.getComponentMapping( d ) ] = transformToSource.getComponentInversion( d ) ? -v : v;
			}
		}
	}

	/**
	 * Apply the {@code transformToSource} to a target bounding box to obtain a
	 * source bounding box.
	 *
	 * @param transformToSource
	 * 		the transformToSource from target to source.
	 * @param boundingBox
	 * 		the target bounding box.
	 *
	 * @return the source bounding box.
	 */
	private static BoundingBox transform( final MixedTransform transformToSource, final BoundingBox boundingBox )
	{
		assert boundingBox.numDimensions() == transformToSource.numSourceDimensions();

		if ( transformToSource.numSourceDimensions() == transformToSource.numTargetDimensions() )
		{ // apply in-place
			final long[] tmp = new long[ transformToSource.numTargetDimensions() ];
			boundingBox.corner1( tmp );
			apply( transformToSource, boundingBox.corner1, tmp );
			boundingBox.corner2( tmp );
			apply( transformToSource, boundingBox.corner2, tmp );
			return boundingBox;
		}
		final BoundingBox b = new BoundingBox( transformToSource.numSourceDimensions() );
		apply( transformToSource, b.corner1, boundingBox.corner1 );
		apply( transformToSource, b.corner2, boundingBox.corner2 );
		return b;
	}

	/**
	 * The concatenated transform from the View {@link #ra RandomAccessible} to the root.
	 */
	private MixedTransform transform;

	/**
	 * Compute the concatenated {@link #transform transform} from the View
	 * {@link #ra RandomAccessible} to the root.
	 */
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

	/**
	 * Check that all View dimensions are used (mapped to some root dimension).
	 *
	 * @return {@code true}, if all View dimensions are used.
	 */
	private boolean checkNoDimensionsAdded()
	{
		// TODO: Views.addDimension(...) is not allowed for now. This could be
		//       supported by replicating hyperplanes in the target block.

		if(transform.hasFullSourceMapping())
		{
			return true;
		}
		else
		{
			errorDescription.append(
					"All View dimensions must map to a dimension of the underlying NativeImg. "
							+ "That is Views.addDimension(...) is not allowed." );
			return false;
		}
	}

	private MixedTransform permuteInvertTransform;

	private MixedTransform remainderTransform;

	/**
	 * Split {@link #transform} into
	 * <ol>
	 * <li>{@link #permuteInvertTransform}, a pure axis permutation followed by inversion of some axes, and</li>
	 * <li>{@link #remainderTransform}, a remainder transformation,</li>
	 * </ol>
	 * such that {@code remainder * permuteInvert == transform}.
	 * <p>
	 * Block copying will then first use {@code remainderTransform} to extract a
	 * intermediate block from the root {@code NativeImg}. Then compute the
	 * final block by applying {@code permuteInvertTransform}.
	 */
	private void splitTransform()
	{
		final MixedTransform[] split = PrimitiveBlocksUtils.split( transform );
		permuteInvertTransform = split[ 0 ];
		remainderTransform = split[ 1 ];
	}

	private < T extends NativeType< T >, R extends NativeType< R > > ViewProperties< T, R > getViewProperties()
	{
		final T viewType = getType( ( RandomAccessible< T > ) ra );
		final NativeImg< R, ? > root = ( NativeImg< R, ? > ) nodes.get( nodes.size() - 1 ).view();
		final R rootType = root.createLinkedType();
		return new ViewProperties<>( viewType, root, rootType, oobExtension, transform, permuteInvertTransform );
	}

	// TODO replace with ra.getType() when that is available in imglib2 core
	private static < T extends Type< T > > T getType( RandomAccessible< T > ra )
	{
		final Point p = new Point( ra.numDimensions() );
		if ( ra instanceof Interval )
			( ( Interval ) ra ).min( p );
		return ra.getAt( p ).createVariable();
	}

	// TEMPORARY. TODO REMOVE
	public static ViewProperties< ?, ? > properties( RandomAccessible< ? > view )
	{
		final ViewBlocksPlayground v = new ViewBlocksPlayground( view );
		v.analyze();
		v.checkRootSupported();
		v.checkRootTypeSupported();
		v.checkConverters();
		v.checkExtensions1();
		v.checkExtensions2();
		v.checkExtensions3();
		v.concatenateTransforms();
		v.checkNoDimensionsAdded();
		v.splitTransform();
		return v.getViewProperties();
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

		private final MixedTransform permuteInvertTransform;

		private final boolean hasPermuteInvertTransform;

		ViewProperties(
				final T viewType,
				final NativeImg< R, ? > root,
				final R rootType,
				final Extension extension,
				final MixedTransform transform,
				final MixedTransform permuteInvertTransform )
		{
			this.viewType = viewType;
			this.root = root;
			this.rootType = rootType;
			this.extension = extension;
			this.transform = transform;
			this.permuteInvertTransform = permuteInvertTransform;
			hasPermuteInvertTransform = !TransformBuilder.isIdentity( permuteInvertTransform );
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
					", permuteInvertTransform=" + permuteInvertTransform +
					'}';
		}

		public T getViewType()
		{
			return viewType;
		}

		public NativeImg< R, ? > getRoot()
		{
			return root;
		}

		public R getRootType()
		{
			return rootType;
		}

		public Extension getExtension()
		{
			return extension;
		}

		public MixedTransform getTransform()
		{
			return transform;
		}

		/**
		 * Returns {@code true} if there is a non-identity {@link
		 * #getPermuteInvertTransform() permute-invert} transform.
		 *
		 * @return {@code true} iff the {@link #getPermuteInvertTransform()
		 * permute-invert} transform is not identity.
		 */
		public boolean hasPermuteInvertTransform()
		{
			return hasPermuteInvertTransform;
		}

		public MixedTransform getPermuteInvertTransform()
		{
			return permuteInvertTransform;
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

		final ViewProperties< ?, ? > viewProperties = playground.getViewProperties();
		System.out.println( "viewProperties = " + viewProperties );
	}
}

package net.imglib2.blk.copy;

import java.util.ArrayList;
import java.util.List;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
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

	enum ViewType
	{
		NATIVE_IMG,
		IDENTITY, // for wrappers like ImgPlus, ImgView
		INTERVAL, //
		CONVERTER, //
		MIXED_TRANSFORM, // for Mixed transforms
		EXTENSION // oob extensions
	}

	interface ViewNode
	{
		ViewType viewType();

		RandomAccessible< ? > view();

		Interval interval();

		default BoundingBox bbox() {
			return interval() == null ? null : new BoundingBox( interval() );
		}
	}

	static abstract class AbstractViewNode< V extends RandomAccessible< ? > > implements ViewNode
	{
		final ViewType viewType;

		final V view;

		final Interval interval;

		AbstractViewNode( final ViewType viewType, final V view )
		{
			this.viewType = viewType;
			this.view = view;
			this.interval = view instanceof Interval ? ( Interval ) view : null;
		}

		@Override
		public ViewType viewType()
		{
			return viewType;
		}

		@Override
		public RandomAccessible< ? > view()
		{
			return view;
		}

		@Override
		public Interval interval()
		{
			return interval;
		}
	}

	static class DefaultViewNode extends AbstractViewNode< RandomAccessible< ? > >
	{
		DefaultViewNode( final ViewType viewType, final RandomAccessible< ? > view )
		{
			super( viewType, view );
		}

		@Override
		public String toString()
		{
			return "DefaultViewNode{viewType=" + viewType + ", view=" + view + ", interval=" + interval + '}';
		}
	}

	static class MixedTransformViewNode extends AbstractViewNode< MixedTransformView< ? > >
	{
		MixedTransformViewNode( final MixedTransformView< ? > view )
		{
			super( ViewType.MIXED_TRANSFORM, view );
		}

		public MixedTransform getTransformToSource()
		{
			return view.getTransformToSource();
		}

		@Override
		public String toString()
		{
			return "MixedTransformViewNode{viewType=" + viewType + ", view=" + view + ", interval=" + interval + '}';
		}
	}

	private final List< ViewNode > nodes = new ArrayList<>();

	private boolean checkRootSupported()
	{
		final ViewNode root = nodes.get( nodes.size() - 1 );
		if ( root.viewType() != ViewType.NATIVE_IMG )
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

		return nodes.stream().noneMatch( node -> node.viewType() == ViewType.CONVERTER );
	}

	private int oobIndex = -1;

	private boolean checkExtensions1()
	{
		// Rule: There must be at most one extension

		// TODO: This could be softened to allow for Extend nodes that are
		//       "swallowed" by subsequent Extend nodes.

		oobIndex = -1;
		for ( int i = 0; i < nodes.size(); i++ )
		{
			if ( nodes.get( i ).viewType() == ViewType.EXTENSION )
			{
				if ( oobIndex < 0 )
					oobIndex = i;
				else // this is already the second EXTENSION
					return false;
			}
		}
		return true;
	}

	private boolean checkExtensions2()
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

		final Interval rootInterval = nodes.get( nodes.size() - 1 ).interval();

		BoundingBox bb = nodes.get( oobIndex + 1 ).bbox();
		System.out.println( "bb.getInterval() = " + bb.getInterval() );

		for ( int i = oobIndex + 1; i < nodes.size(); ++i )
		{
			final ViewNode node = nodes.get( i );

			// all other view types are ignored.
			if ( node.viewType() == ViewType.MIXED_TRANSFORM )
			{
				final MixedTransform t = ( ( MixedTransformViewNode ) node ).getTransformToSource();
				bb = t.transform( bb );
				t.applyInverse( rootInterval );
			}
		}

		System.out.println( "rootInterval = " + rootInterval );
		System.out.println( "bb.getInterval() = " + bb.getInterval() );
		return Intervals.equals( bb.getInterval(), rootInterval );
	}


	private void analyze( final RandomAccessible< ? > rai )
	{
		RandomAccessible< ? > source = rai;
		while ( source != null )
		{
			// NATIVE_IMG,
			if ( source instanceof NativeImg )
			{
				final NativeImg< ?, ? > view = ( NativeImg< ?, ? > ) source;
				nodes.add( new DefaultViewNode( ViewType.NATIVE_IMG, view ) );
				source = null;
			}
			// IDENTITY,
			else if ( source instanceof WrappedImg )
			{
				final WrappedImg< ? > view = ( WrappedImg< ? > ) source;
				nodes.add( new DefaultViewNode( ViewType.IDENTITY, source ) );
				source = view.getImg();
			}
			else if ( source instanceof ImgView )
			{
				final ImgView< ? > view = ( ImgView< ? > ) source;
				nodes.add( new DefaultViewNode( ViewType.IDENTITY, view ) );
				source = view.getSource();
			}
			// INTERVAL,
			else if ( source instanceof IntervalView )
			{
				final IntervalView< ? > view = ( IntervalView< ? > ) source;
				nodes.add( new DefaultViewNode( ViewType.INTERVAL, view ) );
				source = view.getSource();
			}
			// CONVERTER,
			else if ( source instanceof ConvertedRandomAccessible )
			{
				final ConvertedRandomAccessible< ?, ? > view = ( ConvertedRandomAccessible< ?, ? > ) source;
				nodes.add( new DefaultViewNode( ViewType.CONVERTER, view ) );
				source = view.getSource();
			}
			else if ( source instanceof ConvertedRandomAccessibleInterval )
			{
				final ConvertedRandomAccessibleInterval< ?, ? > view = ( ConvertedRandomAccessibleInterval< ?, ? > ) source;
				nodes.add( new DefaultViewNode( ViewType.CONVERTER, view ) );
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
				nodes.add( new DefaultViewNode( ViewType.EXTENSION, view ) );
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






	static RandomAccessible< ? > example1()
	{
 		RandomAccessibleInterval< UnsignedByteType > img0 = ArrayImgs.unsignedBytes( 640, 480, 3 );
		RandomAccessibleInterval< UnsignedByteType > img1 = Views.rotate( img0, 1, 0 );
		RandomAccessibleInterval< UnsignedByteType > img2 = Views.zeroMin( img1 );
//		RandomAccessible< UnsignedByteType > img3 = Views.extendBorder( img2 );
//		RandomAccessible< UnsignedByteType > img4 = Views.hyperSlice( img3, 2, 1 );
		RandomAccessibleInterval< UnsignedByteType > img3 = Views.hyperSlice( img2, 2, 1 );
		RandomAccessible< UnsignedByteType > img4 = Views.extendBorder( img3 );
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

		System.out.println( "playground.checkRootSupported() == " + playground.checkRootSupported() );
		System.out.println( "playground.checkConverters() == " + playground.checkConverters() );
		System.out.println( "playground.checkExtensions1() == " + playground.checkExtensions1() );
		System.out.println( "playground.oobIndex = " + playground.oobIndex );
		System.out.println( "playground.checkExtensions2() == " + playground.checkExtensions2() );

	}

}

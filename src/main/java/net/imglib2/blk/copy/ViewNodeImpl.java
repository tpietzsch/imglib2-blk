package net.imglib2.blk.copy;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.MixedTransformView;

class ViewNodeImpl
{
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

	static class DefaultViewNode extends ViewNodeImpl.AbstractViewNode< RandomAccessible< ? > >
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

	static class MixedTransformViewNode extends ViewNodeImpl.AbstractViewNode< MixedTransformView< ? > >
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
			return "MixedTransformViewNode{viewType=" + viewType + ", view=" + view + ", interval=" + interval + ", transformToSource=" + getTransformToSource() + '}';
		}
	}

	static class ExtensionViewNode extends ViewNodeImpl.AbstractViewNode< ExtendedRandomAccessibleInterval< ?, ? > >
	{
		ExtensionViewNode( final ExtendedRandomAccessibleInterval< ?, ? > view )
		{
			super( ViewType.EXTENSION, view );
		}

		public OutOfBoundsFactory< ?, ? > getOutOfBoundsFactory()
		{
			return view.getOutOfBoundsFactory();
		}

		@Override
		public String toString()
		{
			return "ExtensionViewNode{viewType=" + viewType + ", view=" + view + ", interval=" + interval + ", oobFactory=" + getOutOfBoundsFactory() + '}';
		}
	}
}
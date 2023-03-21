package net.imglib2.blk.copy;

import java.util.function.Supplier;
import net.imglib2.converter.Converter;
import net.imglib2.img.NativeImg;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.NativeType;
import net.imglib2.view.TransformBuilder;

/**
 * TODO javadoc
 *
 * @param <T>
 * 		type of the view {@code RandomAccessible}
 * @param <R>
 * 		type of the root {@code NativeImg}
 */
public class ViewProperties< T extends NativeType< T >, R extends NativeType< R > >
{
	private final T viewType;

	private final NativeImg< R, ? > root;

	private final R rootType;

	private final Extension extension;

	private final MixedTransform transform;

	private final MixedTransform permuteInvertTransform;

	private final boolean hasPermuteInvertTransform;

	private final Supplier< Converter< R, T > > converterSupplier;

	ViewProperties(
			final T viewType,
			final NativeImg< R, ? > root,
			final R rootType,
			final Extension extension,
			final MixedTransform transform,
			final MixedTransform permuteInvertTransform,
			final Supplier< ? extends Converter< ?, ? > > converterSupplier )
	{
		this.viewType = viewType;
		this.root = root;
		this.rootType = rootType;
		this.extension = extension;
		this.transform = transform;
		this.permuteInvertTransform = permuteInvertTransform;
		hasPermuteInvertTransform = !TransformBuilder.isIdentity( permuteInvertTransform );
		this.converterSupplier = converterSupplier == null ? null : () -> ( Converter< R, T > ) converterSupplier.get();
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
				", hasPermuteInvertTransform=" + hasPermuteInvertTransform +
				", permuteInvertTransform=" + permuteInvertTransform +
				", converterSupplier=" + converterSupplier +
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

	public boolean hasConverterSupplier()
	{
		return converterSupplier != null;
	}

	public Supplier< Converter< R, T > > getConverterSupplier()
	{
		return converterSupplier;
	}
}

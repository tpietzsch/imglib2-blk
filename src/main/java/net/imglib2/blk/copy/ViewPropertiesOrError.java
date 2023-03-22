package net.imglib2.blk.copy;

import net.imglib2.type.NativeType;

class ViewPropertiesOrError< T extends NativeType< T >, R extends NativeType< R > >
{
	private final ViewProperties< T, R > viewProperties;

	private final ViewBlocksPlayground.FallbackProperties< T > fallbackProperties;

	private final String errorMessage;

	ViewPropertiesOrError(
			final ViewProperties< T, R > viewProperties,
			final ViewBlocksPlayground.FallbackProperties< T > fallbackProperties,
			final String errorMessage )
	{
		this.viewProperties = viewProperties;
		this.fallbackProperties = fallbackProperties;
		this.errorMessage = errorMessage;
	}

	/**
	 * Whether {@code PrimitiveBlocks} copying from the view is supported, at
	 * all, either {@link #isFullySupported() fully} or via the fall-back implementation.
	 *
	 * @return {@code true}, if {@code PrimitiveBlocks} copying from the view is supported, at all.
	 */
	public boolean isSupported()
	{
		return isFullySupported() || getFallbackProperties() != null;
	}

	/**
	 * Whether optimized {@code PrimitiveBlocks} copying from the view is supported.
	 *
	 * @return {@code true}, if optimized {@code PrimitiveBlocks} copying from the view is supported.
	 */
	public boolean isFullySupported()
	{
		return getViewProperties() != null;
	}

	public ViewProperties< T, R > getViewProperties()
	{
		return viewProperties;
	}

	public ViewBlocksPlayground.FallbackProperties< T > getFallbackProperties()
	{
		return fallbackProperties;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}
}

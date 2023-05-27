package net.imglib2.algorithm.blocks.convert;

public enum ClampType
{
	/**
	 * don't clamp
	 */
	NONE,

	/**
	 * clamp to lower and upper bound
	 */
	CLAMP,

	/**
	 * clamp only to upper bound
	 */
	CLAMP_MAX;
}

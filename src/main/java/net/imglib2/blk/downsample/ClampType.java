package net.imglib2.blk.downsample;

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

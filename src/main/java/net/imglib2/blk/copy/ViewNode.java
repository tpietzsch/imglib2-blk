package net.imglib2.blk.copy;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.transform.integer.BoundingBox;

interface ViewNode
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

	ViewType viewType();

	RandomAccessible< ? > view();

	Interval interval();

	default BoundingBox bbox()
	{
		return interval() == null ? null : new BoundingBox( interval() );
	}
}

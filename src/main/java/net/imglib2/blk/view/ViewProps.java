package net.imglib2.blk.view;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessible;
import net.imglib2.blk.copy.Extension;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccessible;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.NativeImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.transform.integer.Mixed;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;

import static net.imglib2.blk.copy.Extension.Type.UNKNOWN;

public class ViewProps
{
	NativeImg< ?, ? > img;
	Extension extension = Extension.border();
	Object oobValue;
	Converter< ?, ? > converter;
	MixedTransform transform;

	public ViewProps( final RandomAccessible< ? > original )
	{
		final int n = original.numDimensions();
		transform = new MixedTransform( n, n );
		analyze( original );
		if ( !isSupported( transform ) )
			throw new IllegalArgumentException( "aggregated transform is not supported: " + transform );
	}

	private void transform( final Mixed transform )
	{
		this.transform = this.transform.preConcatenate( transform );
	}

	private void analyze( final RandomAccessible< ? > view )
	{
		if ( view instanceof MixedTransformView )
		{
			final MixedTransformView< ? > mixed = ( MixedTransformView< ? > ) view;
			transform( mixed.getTransformToSource() );
			analyze( mixed.getSource() );
		}
		else if ( view instanceof ConvertedRandomAccessible )
		{
			final ConvertedRandomAccessible< ?, ? > converted = ( ConvertedRandomAccessible< ?, ? > ) view;
			if ( converter != null )
				throw new IllegalArgumentException( "Cannot handle more than one converter" );
			converter = converted.getConverter();
			analyze( converted.getSource() );
		}
		else if ( view instanceof ConvertedRandomAccessibleInterval )
		{
			final ConvertedRandomAccessibleInterval< ?, ? > converted = ( ConvertedRandomAccessibleInterval< ?, ? > ) view;
			if ( converter != null )
				throw new IllegalArgumentException( "Cannot handle more than one converter" );
			converter = converted.getConverter();
			analyze( converted.getSource() );
		}
		else if ( view instanceof ExtendedRandomAccessibleInterval )
		{
			ExtendedRandomAccessibleInterval< ?, ? > extended = ( ExtendedRandomAccessibleInterval< ?, ? > ) view;
			final OutOfBoundsFactory< ?, ? > oobFactory = extended.getOutOfBoundsFactory();
			extension = Extension.of( oobFactory );
			if ( extension.type() == UNKNOWN )
				throw new IllegalArgumentException( "Cannot handle OutOfBoundsFactory " + oobFactory.getClass() );
			analyze( extended.getSource() );
		}
		else if ( view instanceof ImgPlus )
		{
			analyze( ( ( ImgPlus< ? > ) view ).getImg() );
		}
		else if ( view instanceof ImgView )
		{
			analyze( ( ( ImgView< ? > ) view ).getSource() );
		}
		else if ( view instanceof IntervalView )
		{
			analyze( ( ( IntervalView< ? > ) view ).getSource() );
		}
		else if ( view instanceof AbstractCellImg || view instanceof ArrayImg || view instanceof PlanarImg )
		{
			img = ( NativeImg< ?, ? > ) view;
		}
		else
			throw new IllegalArgumentException( "Cannot handle " + view );
	}

	private static boolean isSupported( final Mixed t )
	{
		// We allow translation and slicing, but not axis permutation or inverting.
		final int n = t.numSourceDimensions();
		final int m = t.numTargetDimensions();
		if ( n > m )
			return false;
		for ( int d = 0; d < m; ++d )
			if ( t.getComponentInversion( d ) )
				return false;
		int sourceComponent = -1;
		for ( int d = 0; d < m; ++d )
		{
			if ( !t.getComponentZero( d ) )
			{
				final int s = t.getComponentMapping( d );
				if ( s <= sourceComponent )
					return false;
				sourceComponent = s;
			}
		}
		return true;
	}
}

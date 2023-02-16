package net.imglib2.blk.copy;

import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;

import static net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary.SINGLE;

public interface Extension
{
	enum Type
	{
		CONSTANT,
		BORDER,
		MIRROR_SINGLE,
		MIRROR_DOUBLE,
		UNKNOWN
	}

	Type type();

	static Extension border()
	{
		return ExtensionImpl.border;
	}

	static Extension mirrorSingle()
	{
		return ExtensionImpl.mirrorSingle;
	}

	static Extension mirrorDouble()
	{
		return ExtensionImpl.mirrorDouble;
	}

	static < T > Extension constant( T oobValue )
	{
		return new ExtensionImpl.ConstantExtension<>( oobValue );
	}

	static Extension of( OutOfBoundsFactory< ?, ? > oobFactory )
	{
		if ( oobFactory instanceof OutOfBoundsBorderFactory )
		{
			return border();
		}
		else if ( oobFactory instanceof OutOfBoundsMirrorFactory )
		{
			final OutOfBoundsMirrorFactory.Boundary boundary = ( ( OutOfBoundsMirrorFactory< ?, ? > ) oobFactory ).getBoundary();
			return boundary == SINGLE ? mirrorSingle() : mirrorDouble();
		}
		else if ( oobFactory instanceof OutOfBoundsConstantValueFactory )
		{
			return constant( ( ( OutOfBoundsConstantValueFactory ) oobFactory ).getValue() );
		}
		else
		{
			return new ExtensionImpl.UnknownExtension<>( oobFactory );
		}
	}
}

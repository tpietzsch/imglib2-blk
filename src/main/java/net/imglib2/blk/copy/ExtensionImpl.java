package net.imglib2.blk.copy;

import net.imglib2.blk.copy.Extension.Type;
import net.imglib2.outofbounds.OutOfBoundsFactory;

class ExtensionImpl
{
	static final Extension border = () -> Type.BORDER;

	static final Extension mirrorSingle = () -> Type.MIRROR_SINGLE;

	static final Extension mirrorDouble = () -> Type.MIRROR_DOUBLE;

	static class UnknownExtension< T, F > implements Extension
	{
		private final OutOfBoundsFactory< T, F > oobFactory;

		UnknownExtension( final OutOfBoundsFactory< T, F > oobFactory )
		{
			this.oobFactory = oobFactory;
		}

		public OutOfBoundsFactory< T, F > getOobFactory()
		{
			return oobFactory;
		}

		@Override
		public Type type()
		{
			return Type.UNKNOWN;
		}
	}

	static class ConstantExtension< T > implements Extension
	{
		private final T value;

		ConstantExtension( T value )
		{
			this.value = value;
		}

		public T getValue()
		{
			return value;
		}

		@Override
		public Type type()
		{
			return Type.CONSTANT;
		}
	}
}

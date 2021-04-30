package net.imglib2.blk;

import java.util.Arrays;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

public class VectorExample
{

	static void vectorMultiply( float[] a, float[] b, float[] c )
	{
		// It is assumed array arguments are of the same size
		final VectorSpecies< Float > SPECIES = FloatVector.SPECIES_PREFERRED;
		for ( int i = 0; i < a.length; i += SPECIES.length() )
		{
			VectorMask< Float > m = SPECIES.indexInRange( i, a.length );
			FloatVector va = FloatVector.fromArray( SPECIES, a, i, m );
			FloatVector vb = FloatVector.fromArray( SPECIES, b, i, m );
			FloatVector vc = va.mul( vb );
			vc.intoArray( c, i, m );
		}
	}

	public static void main( String[] args )
	{
		float[] a = { 1, 2, 3, 4, 5 };
		float[] b = { 1, 2, 3, 4, 5 };
		float[] c = { 1, 2, 3, 4, 5 };

		vectorMultiply( a, b, c );
		System.out.println( "c = " + Arrays.toString( c ) );

		System.out.println( "FloatVector.SPECIES_PREFERRED = " + FloatVector.SPECIES_PREFERRED );
		System.out.println( "DoubleVector.SPECIES_PREFERRED = " + DoubleVector.SPECIES_PREFERRED );
	}
}

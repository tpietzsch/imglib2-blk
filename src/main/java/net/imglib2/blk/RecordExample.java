package net.imglib2.blk;

public class RecordExample
{
	public record Point(double x, double y)
	{
	}

	public static void main( String[] args )
	{
		System.out.println( "Hello Java 16!" );
		final Point point = new Point( 100, 200 );
		System.out.println( "point = " + point );
	}
}

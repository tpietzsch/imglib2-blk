/*
 * TODO: imglib2 header
 */

/*
 * This is autogenerated source code -- DO NOT EDIT. Instead, edit the
 * corresponding template in templates/ and rerun bin/generate.groovy.
 */

package net.imglib2.blk.downsample;

public class ConversionGenerated
{
	static int u8_to_int( byte value )
	{
		return value & 0xff;
	}

	static byte to_u8( float value )
	{
		return ( byte ) Math.round( value );
	}

}
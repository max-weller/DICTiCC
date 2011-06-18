package de.wikilab.dicticc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public abstract class Dict {
	public static final int LCWORD = 0;
	public static final int WORD = 1;
	public static final int WORD_GENDER = 2;
	public static final int WORD_EXTRA = 3;
	public static final int DEF = 4;
	public static final int DEF_GENDER = 5;
	public static final int DEF_EXTRA = 6;
	public static final int TYPE = 7;
	

    public static String readFile(String path) throws IOException {
    	FileInputStream stream = new FileInputStream(new File(path));
    	try {
    		FileChannel fc = stream.getChannel();
    		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    		/* Instead of using default, pass in a decoder. */
    		return Charset.defaultCharset().decode(bb).toString();
    	}
    	finally {
    		stream.close();
    	}
    }
}

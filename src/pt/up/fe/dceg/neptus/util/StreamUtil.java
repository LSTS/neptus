/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2005/03/01
 */
package pt.up.fe.dceg.neptus.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 *
 */
public class StreamUtil {

    /**
     * 
     */
    private StreamUtil() {
        super();
    }

    /**
     * This won't close the streams!! You have to close them.
     * @param inStream
     * @param outStream
     * @return
     */
    public static boolean copyStreamToStream (InputStream inStream, OutputStream outStream) {
		try {
            byte[] extra = new byte[50000];

            int ret = 0;
            int pos = 0;

            for (;;) {
            	ret = inStream.read(extra);
                NeptusLog.waste().debug("copyStreamToStream> ret: " + ret);
            	if (ret != -1) {
            	    byte[] extra1 = new byte[ret];
					System.arraycopy (extra, 0 , extra1, 0 , ret);					
            	    outStream.write (extra1);
            		outStream.flush();
            		pos =+ret;
                    NeptusLog.waste().debug("copyStreamToStream> pos: " + pos);
            	}
            	else {
                    NeptusLog.waste().debug("copyStreamToStream> end <");
            		break;
            	}
            }
            // outStream.close(); //pdias - Tacking back again because some operations that use this don't want the stream closed 
            return true;
        }
        catch (IOException e) {
            NeptusLog.waste().error("copyStreamToStream", e);
            //e.printStackTrace();
//            try {
//				outStream.close(); //pdias - Tacking back again because some operations that use this don't want the stream closed
//			} catch (IOException e1) {
//			}
            return false;
        }
    }

    
    /**
     * @param inStream
     * @return
     */
    public static String copyStreamToString (InputStream inStream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStreamToStream(inStream, baos);
        String ret = baos.toString();
        return ret;
    }

    /**
     * This won't close the stream!! You have to close them.
     * @param inStream
     * @param outFile
     * @return
     */
    public static boolean copyStreamToFile (InputStream inStream, File outFile) {
        return copyStreamToFile(inStream, outFile, false);
    }

    
    /**
     * This won't close the stream!! You have to close them.
     * @param inStream
     * @param outFile
     * @param append 
     * @return
     */
    public static boolean copyStreamToFile (InputStream inStream, File outFile, boolean append) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile, append);
            boolean ret = copyStreamToStream(inStream, fos);
            return ret;
        }
        catch (Exception e) {
            NeptusLog.pub().error("copyStreamToFile", e);
            return false;
        }
        finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (Exception e) {
                NeptusLog.pub().error("copyStreamToFile", e);
            }
        }
    }
    
    
    /**
     * @param inStream
     * @return
     */
    public static File copyStreamToTempFile (InputStream inStream) {
        File fx;
        try {
        	File tmpDir = new File(ConfigFetch.getNeptusTmpDir());
            fx = File.createTempFile("neptus_", "tmp", tmpDir);
            fx.deleteOnExit();
            boolean ret = copyStreamToFile (inStream, fx);
            if (ret)
                return fx;
            else
                return null;
        }
        catch (IOException e) {
            NeptusLog.pub().error("copyStreamToTempFile", e);
            return null;
        }
    }

	/**
	 * Works similarly to {@link InputStream} but ensures that len is read,
	 * or returns -1 if EOS.
	 * @see {@link InputStream}
	 * @param      in    the InputStream to read from.
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
	 * @param b
	 * @param off
	 * @param len
	 * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
	 * @throws IOException
	 */
	public static int ensureRead (InputStream in, byte[] b, int off, int len) throws IOException {
		int actualRead = 0;
		int ret = in.read(b, off, len);
		if (ret == -1) {
			//throw new IOException("Return -1");
			return ret;
		}
		actualRead += ret;
		while (actualRead != len) {
			int left = len - actualRead;
			ret = in.read(b, off+actualRead, left);
			if (ret == -1) {
				//throw new IOException("Return -1");
				return ret;
			}
			actualRead += ret;
		}
		return actualRead;
	}
}
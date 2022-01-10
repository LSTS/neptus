/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 2005/03/01
 */
package pt.lsts.neptus.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 *
 */
public class StreamUtil {

    /** To avoid instantiation */
    private StreamUtil() {
        super();
    }

    /**
     * Copies an input stream to the output stream until end of stream. 
     * This won't close the streams!! You have to close them.
     * 
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
            	    outStream.write(extra1);
            		outStream.flush();
            		pos =+ret;
                    NeptusLog.waste().debug("copyStreamToStream> pos: " + pos);
            	}
            	else {
                    NeptusLog.waste().debug("copyStreamToStream> end <");
            		break;
            	}
            }
            return true;
        }
        catch (IOException e) {
            NeptusLog.waste().error("copyStreamToStream", e);
            return false;
        }
    }

    
    /**
     * Copies an input stream to a string until end of stream. 
     * This won't close the stream!! You have to close them.
     * 
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
     * Copies an input stream to a file until end of stream. 
     * This won't close the stream!! You have to close them.
     * This will overwrite the file.
     * 
     * @param inStream
     * @param outFile
     * @return
     */
    public static boolean copyStreamToFile (InputStream inStream, File outFile) {
        return copyStreamToFile(inStream, outFile, false);
    }

    
    /**
     * Copies an input stream to a file until end of stream. 
     * This won't close the stream!! You have to close them.
     * 
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
            }
            catch (Exception e) {
                NeptusLog.pub().error("copyStreamToFile", e);
            }
        }
    }
    
    /**
     * Copies an input stream to a temporary file until end of stream. 
     * This won't close the stream!! You have to close them.
     * 
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
     * Works similarly to {@link InputStream} but ensures that len is read, or returns -1 if EOS.
     * 
     * @see {@link InputStream}
     * 
     * @param in the InputStream to read from.
     * @param b the buffer into which the data is read.
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len The maximum number of bytes to read.
     * @return The total number of bytes read into the buffer, or <code>-1</code> if there is no more data because the
     *         end of the stream has been reached.
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
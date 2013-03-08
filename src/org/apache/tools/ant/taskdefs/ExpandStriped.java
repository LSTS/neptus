/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtilsStriped;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

/**
 * Unzip a file.
 * 
 * 
 * @since Ant 1.1
 * 
 * @ant.task category="packaging" name="unzip" name="unjar" name="unwar"
 */
public class ExpandStriped {
    private boolean overwrite = true;

    private static final String NATIVE_ENCODING = "native-encoding";

    private String encoding = "UTF8";

    public void expandFile(File srcF, File dir) {
        expandFile(FileUtilsStriped.newFileUtils(), srcF, dir);
    }

    /*
     * This method is to be overridden by extending unarchival tasks.
     */
    protected void expandFile(FileUtilsStriped fileUtils, File srcF, File dir) {
        // log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
        ZipFile zf = null;
        try {
            zf = new ZipFile(srcF, encoding);
            Enumeration<ZipEntry> e = zf.getEntries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                extractFile(fileUtils, srcF, dir, zf.getInputStream(ze), ze.getName(), new Date(ze.getTime()),
                        ze.isDirectory());
            }

            // log("expand complete", Project.MSG_VERBOSE);
        }
        catch (IOException ioe) {
            throw new BuildException("Error while expanding " + srcF.getPath(), ioe);
        }
        finally {
            if (zf != null) {
                try {
                    zf.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    protected void extractFile(FileUtilsStriped fileUtils, File srcF, File dir, InputStream compressedInputStream,
            String entryName, Date entryDate, boolean isDirectory) throws IOException {

        File f = fileUtils.resolveFile(dir, entryName);
        try {
            if (!overwrite && f.exists() && f.lastModified() >= entryDate.getTime()) {
                return;
            }

            File dirF = fileUtils.getParentFile(f);
            if (dirF != null) {
                dirF.mkdirs();
            }

            if (isDirectory) {
                f.mkdirs();
            }
            else {
                byte[] buffer = new byte[1024];
                int length = 0;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);

                    while ((length = compressedInputStream.read(buffer)) >= 0) {
                        fos.write(buffer, 0, length);
                    }

                    fos.close();
                    fos = null;
                }
                finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        }
                        catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }

            fileUtils.setFileLastModified(f, entryDate.getTime());
        }
        catch (FileNotFoundException ex) {
            // log("Unable to expand to file " + f.getPath(), Project.MSG_WARN);
        }

    }


    /**
     * Should we overwrite files in dest, even if they are newer than the corresponding entries in the archive?
     */
    public void setOverwrite(boolean b) {
        overwrite = b;
    }

    // /**
    // * Add a patternset
    // */
    // public void addPatternset(PatternSet set) {
    // patternsets.addElement(set);
    // }
    //
    // /**
    // * Add a fileset
    // */
    // public void addFileset(FileSet set) {
    // filesets.addElement(set);
    // }

    /**
     * Sets the encoding to assume for file names and comments.
     * 
     * <p>
     * Set to <code>native-encoding</code> if you want your platform's native encoding, defaults to UTF8.
     * </p>
     * 
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        if (NATIVE_ENCODING.equals(encoding)) {
            encoding = null;
        }
        this.encoding = encoding;
    }

}

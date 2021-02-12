/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 1/Mar/2005
 */
package pt.lsts.neptus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.log4j.Level;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 */
public class ZipUtils {
    
    /** To avoid instantiation */
    public ZipUtils() {
    }
    
    /**
     * Unzips a Zip file into destination path.
     * It assumes ibm437 encoding at first.
     * 
     * @param zipFile
     * @param destinationPath
     * @return
     */
    public static boolean unZip(String zipFile, String destinationPath) {
        ZipFile fxZipFile = null;
        try {
            if (Charset.isSupported("ibm437"))
                fxZipFile = new ZipFile(zipFile, "ibm437");
            else
                fxZipFile = new ZipFile(zipFile);
            
            NeptusLog.pub().debug(zipFile + "   " + fxZipFile.getEncoding());
            
            Enumeration<ZipArchiveEntry> entries = fxZipFile.getEntries();

            File destination = new File(destinationPath).getAbsoluteFile();
            destination.mkdirs();
            
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                InputStream content = fxZipFile.getInputStream(entry);
                
                try {
                    NeptusLog.pub().debug(entry.getName() + "   " + entry.getLastModifiedDate() + "\n"
                            + Arrays.toString(entry.getExtraFields()));
                    if (entry.isDirectory()) {
                        File dir = new File(destinationPath, entry.getName());
                        boolean bl = dir.mkdirs();
                        NeptusLog.pub().debug("Created dir (" + bl + "): " + dir.getAbsolutePath());
                    }
                    else {
                        File file = new File(destinationPath, entry.getName());
                        file.getParentFile().mkdirs();
                        FileOutputStream fxOutStream = new FileOutputStream(file);
                        boolean bl = StreamUtil.copyStreamToStream(content, fxOutStream);
                        try {
                            fxOutStream.close();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        file.setLastModified(entry.getTime() < 0 ? System.currentTimeMillis() : entry.getTime());
                        NeptusLog.pub().debug("Created file(" + bl + "): " + entry.getName());
                    }
                }
                finally {
                    try {
                        content.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            fxZipFile.close();
            
            return true;
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("unZip", e);
            return false;
        }
        catch (Exception e) {
            NeptusLog.pub().error("unZip", e);
            return false;
        }
        finally {
            if (fxZipFile != null) {
                try {
                    fxZipFile.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Compresses a source folder (of file) into a Zip.
     * 
     * @param zipFile
     * @param sourceDir
     * @param encoding
     * @return
     */
    public static boolean zipDir(String zipFile, String sourceDir, String encoding) {
        try {
            File fxZipFile = new File(zipFile);
            ZipArchiveOutputStream zOutStream = new ZipArchiveOutputStream(fxZipFile);
            zOutStream.setEncoding(encoding);
            
            zOutStream.setUseLanguageEncodingFlag(true);
            zOutStream.setFallbackToUTF8(false);
            zOutStream.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.NOT_ENCODEABLE);

            zipDirWorker(sourceDir, sourceDir, zOutStream);

            zOutStream.close();

            return true;
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("zipDir", e);
            return false;
        }
        catch (Exception e) {
            NeptusLog.pub().error("zipDir", e);
            return false;
        }
    }

    /**
     * Compresses a source folder (of file) into a Zip
     * (with "IBM437" encoding, the most usual for Zip files).
     * 
     * @param zipFile
     * @param sourceDir
     * @return
     */
    public static boolean zipDir(String zipFile, String sourceDir) {
        if (Charset.isSupported("ibm437"))
            return zipDir(zipFile, sourceDir, "ibm437");
        else
            return zipDir(zipFile, sourceDir, null);
    }

    /**
     * This is the Zip worker.
     * 
     * @param dir2zip
     * @param baseDir
     * @param zOutStream
     */
    private static void zipDirWorker(String dir2zip, String baseDir, ZipArchiveOutputStream zOutStream) {
        File baseDirFile = new File(baseDir);
        if (baseDirFile.isFile()) {
            baseDirFile = baseDirFile.getAbsoluteFile().getParentFile();
            baseDir = baseDirFile.getAbsolutePath();
        }
        File zipDir = new File(dir2zip).getAbsoluteFile();
        // get a listing of the directory content
        String[] dirList;
        if (zipDir.isDirectory()) {
            dirList = zipDir.list();
        }
        else {
            dirList = new String[] { zipDir.getName() };
            zipDir = zipDir.getParentFile();
        }
        // loop through dirList, and zip the files
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                // if the File object is a directory, call this
                // function again to add its content recursively
                String filePath = f.getPath();
                zipDirWorker(filePath, baseDir, zOutStream);
                // loop again
                continue;
            }
            // if we reached here, the File object was not a directory
            addZipEntry(FileUtil.relativizeFilePath(baseDir, f.getPath()), f.getPath(), zOutStream);
        }
    }

    /**
     * Called to add a Zip entry.
     * 
     * @param entryName
     * @param filePath
     * @param zOutStream
     * @return
     */
    private static boolean addZipEntry(String entryName, String filePath, ZipArchiveOutputStream zOutStream) {
        try {
            entryName = entryName.replace('\\', '/');
            File contentFx = new File(filePath);
            
            ZipArchiveEntry entry = (ZipArchiveEntry) zOutStream.createArchiveEntry(contentFx, entryName);

            zOutStream.putArchiveEntry(entry);

            FileInputStream fxInStream = new FileInputStream(contentFx);
            OutputStream os = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    zOutStream.write(b);
                }
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    zOutStream.write(b, off, len);
                }
            };
            boolean bl = StreamUtil.copyStreamToStream(fxInStream, os);
            zOutStream.flush();
            zOutStream.closeArchiveEntry();

            try {
                fxInStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                os.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
            return bl;
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("addZipEntry", e);
            return false;
        }
        catch (IOException e) {
            NeptusLog.pub().error("addZipEntry", e);
            return false;
        }
    }

    /**
     * Searches a Zip mission file for the mission file and returns it as an {@link InputStream}-
     * 
     * @param zipFile 
     * @return
     */
    public static InputStream getMissionZipedAsInputSteam(String zipFile) {
        boolean missionFileFound = false;
        try {
            FileInputStream fxInStream = new FileInputStream(zipFile);
            ZipArchiveInputStream zInStream; 
            if (Charset.isSupported("ibm437"))
                zInStream = new ZipArchiveInputStream(fxInStream, "ibm437");
            else
                zInStream = new ZipArchiveInputStream(fxInStream);

            while (true) {
                ZipArchiveEntry zipEntry = zInStream.getNextZipEntry();

                if (zipEntry == null)
                    break;
                if (zipEntry.isDirectory()) {
                    continue;
                }
                else {
                    String fileZname = zipEntry.getName();
                    if (fileZname.equalsIgnoreCase("mission.nmis")) {
                        missionFileFound = true;
                        break;
                    }
                }
            }

            if (missionFileFound)
                return zInStream;
            else
                return null;
            // zInStream.close();
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("unZip", e);
            return null;
        }
        catch (Exception e) {
            NeptusLog.pub().error("unZip", e);
            return null;
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        ConfigFetch.initialize();
        NeptusLog.pub().setLevel(Level.DEBUG);
        // ZipUtils.zipDir("teste.zip", "tmp");
        // ZipUtils.unZip("teste.zip", "tmp/teste");
        // ZipUtils.zipDir("teste-nep.zip", "D:\\Program Files\\BitComet");
        // System.err.println(Charset.isSupported("ibm437"));
        // NeptusLog.pub().info("<###> "+new String("Cópia".getBytes(), "IBM437"));

        ZipUtils.zipDir("teste0.zip", "CHANGES.md");
        ZipUtils.zipDir("teste1.zip", "c:\\Temp\\zipTest\\test1");
        ZipUtils.zipDir("teste2.zip", "c:\\Temp\\zipTest\\test2");

        ZipUtils.unZip("teste2.zip", "c:\\Temp\\zipTest\\test2-unzip");
        ZipUtils.unZip("teste2-utf8.zip", "c:\\Temp\\zipTest\\test2-utf8-unzip");
        ZipUtils.unZip("teste2-0.zip", "c:\\Temp\\zipTest\\test2-unzip-0");
    }
}

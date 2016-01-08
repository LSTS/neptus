/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 1/Mar/2005
 *
 */

package pt.lsts.neptus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.taskdefs.ExpandStriped;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import pt.lsts.neptus.NeptusLog;

/**
 * @author Paulo Dias
 * @version 1.1 Ago/2006
 */
public class ZipUtils {
    /**
     * @param zipFile
     * @param destinationPath
     * @return
     */
    public static boolean unZip(String zipFile, String destinationPath) {
        try {
            FileInputStream fxInStream = new FileInputStream(zipFile);
            ZipInputStream zInStream = new ZipInputStream(fxInStream);
            File destination = new File(destinationPath).getAbsoluteFile();
            destination.mkdirs();

            while (true) {
                java.util.zip.ZipEntry zipEntry = zInStream.getNextEntry();
                if (zipEntry == null)
                    break;
                if (zipEntry.isDirectory()) {
                    File dir = new File(destinationPath, zipEntry.getName());
                    boolean bl = dir.mkdirs();
                    NeptusLog.pub().debug("Created dir (" + bl + "): " + dir.getAbsolutePath());
                }
                else {
                    File file = new File(destinationPath, zipEntry.getName());
                    file.getParentFile().mkdirs();
                    FileOutputStream fxOutStream = new FileOutputStream(file);
                    boolean bl = StreamUtil.copyStreamToStream(zInStream, fxOutStream);
                    NeptusLog.pub().debug("Created file(" + bl + "): " + zipEntry.getName());
                }
            }
            zInStream.close();

            return true;
        }
        catch (IllegalArgumentException e) {
            NeptusLog.pub().debug("unZip: found Zip with encoding IBM437. " + "Running alternateUnzip!");
            return alternateUnzip(zipFile, destinationPath, "ibm437");
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("unZip", e);
            // e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            NeptusLog.pub().error("unZip", e);
            // e.printStackTrace();
            return false;
        }

    }

    /**
     * @param zipFile
     * @param destinationPath
     * @param encoding
     * @return
     */
    private static boolean alternateUnzip(String zipFile, String destinationPath, String encoding) {
        try {
            File sourceZip = new File(zipFile).getAbsoluteFile();
            File destination = new File(destinationPath).getAbsoluteFile();
            ExpandStriped exp = new ExpandStriped();
            exp.setEncoding(encoding);
            exp.expandFile(sourceZip, destination);
            return true;
        }
        catch (RuntimeException e) {
            NeptusLog.pub().error("alternateUnzip", e);
            return false;
        }
    }

    /**
     * @param zipFile
     * @param sourceDir
     * @param encoding
     * @return
     */
    public static boolean zipDir(String zipFile, String sourceDir, String encoding) {
        try {
            FileOutputStream fxOutStream = new FileOutputStream(zipFile);
            ZipOutputStream zOutStream = new ZipOutputStream(fxOutStream);
            zOutStream.setEncoding(encoding);

            zipDirWorker(sourceDir, sourceDir, zOutStream);

            zOutStream.close();
            fxOutStream.flush();
            fxOutStream.close();

            return true;
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("zipDir", e);
            // e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            NeptusLog.pub().error("zipDir", e);
            // e.printStackTrace();
            return false;
        }
    }

    /**
     * With "IBM437" encoding.
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
     * @param dir2zip
     * @param baseDir
     * @param zOutStream
     */
    private static void zipDirWorker(String dir2zip, String baseDir, ZipOutputStream zOutStream) {
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
            ZipUtils.addZipEntry(FileUtil.relativizeFilePath(baseDir, f.getPath()), f.getPath(), zOutStream);
        }
    }

    /**
     * @param entryName
     * @param filePath
     * @param zOutStream
     * @return
     */
    private static boolean addZipEntry(String entryName, String filePath, ZipOutputStream zOutStream) {
        try {

            entryName = entryName.replace('\\', '/');
            ZipEntry zEntry = new ZipEntry(entryName);

            FileInputStream fxInStream = new FileInputStream(filePath);

            zOutStream.putNextEntry(zEntry);
            boolean bl = StreamUtil.copyStreamToStream(fxInStream, zOutStream);
            zOutStream.flush();
            zOutStream.closeEntry();
            fxInStream.close();
            return bl;
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("addZipEntry", e);
            // e.printStackTrace();

            return false;
        }
        catch (IOException e) {
            NeptusLog.pub().error("addZipEntry", e);
            // e.printStackTrace();
            return false;
        }
    }

    public static InputStream getMissionZipedAsInputSteam(String zipFile) {
        boolean missionFileFound = false;
        try {
            FileInputStream fxInStream = new FileInputStream(zipFile);
            ZipInputStream zInStream = new ZipInputStream(fxInStream);

            while (true) {
                java.util.zip.ZipEntry zipEntry = zInStream.getNextEntry();

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
        catch (IllegalArgumentException e) {
            NeptusLog.pub().debug("unZip: found Zip with encoding IBM437. " + "Running alternateUnzip!");
            return null; // alternateUnzip(zipFile, destinationPath, "ibm437");
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("unZip", e);
            // e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            NeptusLog.pub().error("unZip", e);
            // e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        // ConfigFetch.initialize();
        // ZipUtils.zipDir("teste.zip", "tmp");
        // ZipUtils.unZip("teste.zip", "tmp/teste");
        // ZipUtils.zipDir("teste-nep.zip", "D:\\Program Files\\BitComet");
        // System.err.println(Charset.isSupported("ibm437"));
        // NeptusLog.pub().info("<###> "+new String("Cópia".getBytes(), "IBM437"));

        ZipUtils.zipDir("teste-nep.zip", "CHANGES.md");
    }
}

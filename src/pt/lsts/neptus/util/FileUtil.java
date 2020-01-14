/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 2005/01/14
 */
package pt.lsts.neptus.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Some file utilities.
 * 
 * @author Paulo Dias
 */
/**
 * @author pdias
 *
 */
public class FileUtil {
    /** Byte order mark for UTF-8 */
    public static final byte[] BOM_UTF8 = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    /** Byte order mark for UTF-16 little-endian */
    public static final byte[] BOM_UTF16LE = new byte[] { (byte) 0xFF, (byte) 0xFE };
    /** Byte order mark for UTF-16 big-endian */
    public static final byte[] BOM_UTF16BE = new byte[] { (byte) 0xFE, (byte) 0xFF };
    /** Byte order mark for UTF-32 little-endian */
    public static final byte[] BOM_UTF32LE = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFE };
    /** Byte order mark for UTF-32 big-endian */
    public static final byte[] BOM_UTF32BE = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF };

    /** Mission file extension */
    public static final String FILE_TYPE_MISSION = "nmis";
    /** Compressed mission file extension */
    public static final String FILE_TYPE_MISSION_COMPRESSED = "nmisz";
    /** Map file extension */
    public static final String FILE_TYPE_MAP = "nmap";
    /** Console file extension */
    public static final String FILE_TYPE_CONSOLE = "ncon";
    /** Config file extension (not used often) */
    public static final String FILE_TYPE_CONFIG = "ncfg";
    /** Vehicle file extension */
    public static final String FILE_TYPE_VEHICLE = "nvcl";
    /** Checklist file extension */
    public static final String FILE_TYPE_CHECKLIST = "nchk";
    /** WSN (Wireless Sensor Network) file extension (not used often now) */
    public static final String FILE_TYPE_WSN = "nwsn";
    /** INI file extension */
    public static final String FILE_TYPE_INI = "ini";
    /** RMF (REMUS Mission Format) file extension (not used often now) */
    public static final String FILE_TYPE_RMF = "rmf";
    /** XML file extension */
    public static final String FILE_TYPE_XML = "xml";
    /** LSF (LSTS Serialized Format) file extension */
    public static final String FILE_TYPE_LSF = "lsf";
    /** GZipped compressed LSF (LSTS Serialized Format) file extension */
    public static final String FILE_TYPE_LSF_COMPRESSED = "lsf.gz";
    /** BZipped2 compressed LSF (LSTS Serialized Format) file extension */
    public static final String FILE_TYPE_LSF_COMPRESSED_BZIP2 = "lsf.bz2";

    /** To avoid instantiation */
    private FileUtil() {
    }

    /**
     * Returns the file extension string.
     * 
     * @param fx
     * @return
     */
    public static String getFileExtension(File fx) {
        String path = null;
        try {
            path = fx.getCanonicalPath();
        }
        catch (IOException e1) {
            path = fx.getAbsolutePath();
        }
        return getFileExtension(path);
    }

    /**
     * Returns the file extension string.
     * 
     * @param path
     * @return
     */
    public static String getFileExtension(String path) {
        int lastDotPostion = path.lastIndexOf('.');
        return (lastDotPostion != -1) ? (path.substring(lastDotPostion + 1)) : "";
    }
    
    /**
     * Return the file name without extension.
     * 
     * @param fx
     * @return
     */
    public static String getFileNameWithoutExtension(File fx) {
        String path = null;
        try {
            path = fx.getCanonicalPath();
        }
        catch (IOException e1) {
            path = fx.getAbsolutePath();
        }
        return getFileNameWithoutExtension(path);
    }
    
    /**
     * Return the file name without extension.
     * 
     * @param path
     * @return
     */
    public static String getFileNameWithoutExtension(String path) {
        File f = new File(path);
        String fname = f.getName();

        int lastDotPostion = fname.lastIndexOf('.');
        String ret = (lastDotPostion != -1) ? (fname.substring(0, lastDotPostion)) : fname;
        return ret;
    }
    
    /**
     * Return the file name string with the extension replaced.
     * 
     * @param fx
     * @param newExtension
     * @return
     */
    public static String replaceFileExtension(File fx, String newExtension) {
        String path = null;
        try {
            path = fx.getCanonicalPath();
        }
        catch (IOException e1) {
            path = fx.getAbsolutePath();
        }
        return replaceFileExtension(path, newExtension);
    }

    /**
     * Return the file name string with the extension replaced.
     * 
     * @param path
     * @param newExtension
     * @return
     */
    public static String replaceFileExtension(String path, String newExtension) {
        int lastDotPostion = path.lastIndexOf('.');
        String st = (lastDotPostion != -1) ? (path.substring(0, lastDotPostion)) : path;
        return st + "." + newExtension;
    }

    /**
     * See {@link #checkFileForExtensions(String, String...)}.
     * 
     * @param file
     * @param extensions
     * @return
     */
    public static String checkFileForExtensions(File file, String... extensions) {
        return checkFileForExtensions(file.getName(), extensions);
    }

    /**
     * Checks a file name for matching a file extension (case insensitive).
     * 
     * @param filePath
     * @param extensions
     * @return The extension that matched or null if not.
     */
    public static String checkFileForExtensions(String filePath, String... extensions) {
        if (filePath == null || extensions == null || extensions.length == 0)
            return null;
        
        String fileExt = getFileExtension(filePath);
        if (fileExt == null || fileExt.isEmpty())
            return null;
        
        for (String ext : extensions) {
            if (fileExt.equalsIgnoreCase(ext))
                return ext;
        }
        
        return null;
    }
    
    /**
     * See {@link #getFileAsString(String)}
     * 
     * @param fx
     * @return
     */
    public static String getFileAsString(File fx) {
        return getFileAsString(fx.getAbsolutePath());
    }

    /**
     * Returns the content of the file as a UTF-8 (or in the encoding 
     * of the file if found, {@link #findOutFileEncoding(byte[])}) encoded string.
     * 
     * @param url The URL or relative file path.
     * @return
     */
    public static String getFileAsString(String url) {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        byte[] ba = null;
        String result = null;
        String actualEncoding = "UTF-8";
        try {
            fis = new FileInputStream(url);
            ba = new byte[1024];
            while ((len = fis.read(ba)) > 0) {
                bos.write(ba, 0, len);
            }
            ba = bos.toByteArray();

            // Find the file encoding, just a try
            String enc = findOutFileEncoding(ba);
            if (enc == null)
                actualEncoding = "UTF-8";
            else
                actualEncoding = enc;

            try {
                result = new String(ba, actualEncoding);
            }
            catch (UnsupportedEncodingException e1) {
                NeptusLog.pub().debug(FileUtil.class + "getFileAsString ", e1);
                result = new String(ba, "UTF-8");
            }
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error(FileUtil.class, e);
        }
        catch (IOException e) {
            NeptusLog.pub().error(FileUtil.class, e);
        }
        finally {
            if (fis != null)
                try {
                    fis.close();
                }
                catch (IOException e) {
                    NeptusLog.pub().error(FileUtil.class, e);
                }
        }

        return result;
    }

    /**
     * Get the list of sorted files in folder or null if parent folder doesn't exist or pattern is not valid.
     * This is the worker used by {@link #getFilesFromDisk(File, String)} and {@link #getFoldersFromDisk(File, String)}.
     * 
     * @param folderToLoad
     * @param searchPattern
     * @param justFolders
     * @return
     */
    private static File[] getFilesFromDiskWorker(File folderToLoad, final String searchPattern,
            final boolean justFolders) {
        try {
            if (folderToLoad != null && folderToLoad.exists()) {
                File folder = folderToLoad.isDirectory() ? folderToLoad : 
                    folderToLoad.getParentFile();
                
                FilenameFilter fileFilter = new FilenameFilter() {
                    Pattern pat = searchPattern == null || searchPattern.isEmpty() ? null : Pattern
                            .compile(searchPattern);

                    @Override
                    public boolean accept(File file, String name) {
                        if (pat == null) {
                            File fx = new File(file, name);
                            return justFolders ? fx.isDirectory() : fx.isFile();
                        }
                        else {
                            Matcher m = pat.matcher(name);
                            boolean ret = m.find();
                            File fx = new File(file, name);
                            return ret ? (justFolders ? fx.isDirectory() : fx.isFile()) : false;
                        }
                    }
                };
                
                File[] lst = folder.listFiles(fileFilter);
                Arrays.sort(lst);
                return lst;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the list of sorted files in folder or null if parent folder doesn't exist or pattern is not valid.
     * 
     * @param folderToLoad
     * @param searchPattern null for all
     * @return
     */
    public static File[] getFilesFromDisk(File folderToLoad, final String searchPattern) {
        return getFilesFromDiskWorker(folderToLoad, searchPattern, false);
    }

    /**
     * Get the list of sorted folders in folder or null if parent folder doesn't exist or pattern is not valid.
     * 
     * @param folderToLoad
     * @param searchPattern null for all
     * @return
     */
    public static File[] getFoldersFromDisk(File folderToLoad, final String searchPattern) {
        return getFilesFromDiskWorker(folderToLoad, searchPattern, true);
    }

    /**
     * Return the file as an byte array.
     * 
     * @param url The URL or file path for the file
     * @return
     */
    public static byte[] getFileAsByteArray(String url) {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        byte[] ba = null;
        try {
            fis = new FileInputStream(url);
            ba = new byte[1024];
            while ((len = fis.read(ba)) > 0) {
                bos.write(ba, 0, len);
            }
            ba = bos.toByteArray();
            fis.close();
        }
        catch (FileNotFoundException e) {
            // e.printStackTrace();
            NeptusLog.pub().error(FileUtil.class, e);
        }
        catch (IOException e) {
            // e.printStackTrace();
            NeptusLog.pub().error(FileUtil.class, e);
        }
        return ba;
    }

    /**
     * Find the encoding of a file.
     * 
     * @param ba
     * @return
     */
    private static String findOutFileEncoding(byte[] ba) {
        try {
            String str = new String(ba);
            String delim = "=\"";

            // Test if XML
            boolean isXML = str.startsWith("<?xml");
            if (isXML) {
                int tc = str.indexOf(">");
                if (tc == -1)
                    return null;
                // String s1 = str.substring (0, tc);
                tc = str.indexOf("encoding");
                if (tc == -1)
                    return null;
                String s2 = str.substring(tc + "encoding".length());
                StringTokenizer strt = new StringTokenizer(s2, delim);
                String encoding = strt.nextToken();
                NeptusLog.pub().debug(FileUtil.class + ".findOutFileEncoding - " + encoding);
                return encoding;
            }

            if (ba.length == 0) {
                return "UTF-8";
            }
            else if ((ba[0] == BOM_UTF8[0]) && (ba[1] == BOM_UTF8[1]) && (ba[2] == BOM_UTF8[2])) {
                NeptusLog.pub().debug(FileUtil.class + ".findOutFileEncoding - " + "UTF-8");
                return "UTF-8";
            }
            else if ((ba[0] == BOM_UTF16LE[0]) && (ba[1] == BOM_UTF16LE[1])) {
                NeptusLog.pub().debug(FileUtil.class + ".findOutFileEncoding - " + "UTF-16LE");
                return "UTF-16LE";
            }
            else if ((ba[0] == BOM_UTF16BE[0]) && (ba[1] == BOM_UTF16BE[1])) {
                NeptusLog.pub().debug(FileUtil.class + ".findOutFileEncoding - " + "UTF-16BE");
                return "UTF-16BE";
            }
        }
        catch (RuntimeException e) {
            NeptusLog.pub().error("FIleUtil:findOutFileEncoding", e);
        }
        return null;
    }

    /**
     * This will get a flat or partial formated XML string as a formated XML string.
     * (With XML declaration, {@link #getAsPrettyPrintFormatedXMLString(String, boolean)} )
     * 
     * @param xml
     * @return
     */
    public static String getAsPrettyPrintFormatedXMLString(String xml) {
        return getAsPrettyPrintFormatedXMLString(xml, false);
    }

    /**
     * This will get a flat or partial formated XML string as a formated XML string.
     * 
     * @param xml
     * @param omitDeclaration If the declaration of the XML file is to be included or omitted.
     * @return
     */
    public static String getAsPrettyPrintFormatedXMLString(String xml, boolean omitDeclaration) {
        Document doc = null;
        try {
            doc = DocumentHelper.parseText(xml);
        }
        catch (DocumentException e) {
            NeptusLog.pub().error("FIleUtil:getAsPrettyPrintFormatedXMLString", e);
            return null;
        }
        return getAsPrettyPrintFormatedXMLString(doc, omitDeclaration);
    }

    /**
     * This removes all extra spaces and new lines from the XML and return it.
     * (With XML declaration, {@link #getAsCompactFormatedXMLString(String, boolean)} )
     * 
     * @param xml
     * @return
     */
    public static String getAsCompactFormatedXMLString(String xml) {
        return getAsCompactFormatedXMLString(xml, false);
    }

    /**
     * This removes all extra spaces and new lines from the XML and return it.
     * 
     * @param xml
     * @param omitDeclaration If the declaration of the XML file is to be included or omitted.
     * @return
     */
    public static String getAsCompactFormatedXMLString(String xml, boolean omitDeclaration) {
        Document doc = null;
        try {
            doc = DocumentHelper.parseText(xml);
        }
        catch (DocumentException e) {
            NeptusLog.pub().error("FIleUtil:getAsCompactFormatedXMLString", e);
            return null;
        }
        return getAsCompactFormatedXMLString(doc, omitDeclaration);
    }

    /**
     * This will get a flat or partial formated XML string as a formated XML string.
     * (With XML declaration, {@link #getAsPrettyPrintFormatedXMLString(Document, boolean)} )
     * 
     * @param doc
     * @return
     */
    public static String getAsPrettyPrintFormatedXMLString(Document doc) {
        return getAsPrettyPrintFormatedXMLString(doc, false);
    }

    /**
     * This will get a flat or partial formated XML string as a formated XML string.
     * 
     * @param doc
     * @param omitDeclaration If the declaration of the XML file is to be included or omitted.
     * @return
     */
    public static String getAsPrettyPrintFormatedXMLString(Document doc, boolean omitDeclaration) {
        // PrettyPrint format
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setTrimText(false);
        // format.setPadText(true);
        format.setNewLineAfterDeclaration(false);
        format.setSuppressDeclaration(omitDeclaration);
        return getAsFormatedXMLString(doc, format);
    }

    /**
     * This removes all extra spaces and new lines from the XML and return it.
     * (With XML declaration, {@link #getAsCompactFormatedXMLString(String, boolean)} )
     * 
     * @param doc
     * @return
     */
    public static String getAsCompactFormatedXMLString(Document doc) {
        return getAsCompactFormatedXMLString(doc, false);
    }

    /**
     * This removes all extra spaces and new lines from the XML and return it.
     * 
     * @param doc
     * @param omitDeclaration If the declaration of the XML file is to be included or omitted.
     * @return
     */
    public static String getAsCompactFormatedXMLString(Document doc, boolean omitDeclaration) {
        // Compact format
        OutputFormat format = OutputFormat.createCompactFormat();
        format.setSuppressDeclaration(omitDeclaration);
        return getAsFormatedXMLString(doc, format);
    }

    /**
     * This is the worker for the others methods.
     * 
     * @param doc
     * @param format Either {@link OutputFormat#createPrettyPrint()} or {@link OutputFormat#createCompactFormat()}.
     * @return
     */
    private static String getAsFormatedXMLString(Document doc, OutputFormat format) {
        String enc = doc.getXMLEncoding();
        if (enc == null)
            enc = "UTF-8";
        
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        XMLWriter writer;
        try {
            writer = new XMLWriter(ba, format);
            writer.write(doc);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String result;
        try {
            result = ba.toString(enc);
        }
        catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            try {
                result = ba.toString("UTF-8");
            }
            catch (UnsupportedEncodingException e2) {
                e2.printStackTrace();
                result = ba.toString();
            }
        }
        return result;
    }

    /**
     * This will appends the source to the destination file and close it.
     * 
     * @param destination
     * @param source
     * @throws Exception
     */
    public static void appendToFile(File destination, InputStream source) throws Exception {
        FileOutputStream fos = new FileOutputStream(destination, true);

        byte[] buff = new byte[1024];
        int readBytes = source.read(buff);

        while (readBytes > 0) {
            fos.write(buff, 0, readBytes);
            readBytes = source.read(buff);
        }

        fos.close();
    }

    /**
     * This concatenates two files.
     * 
     * @param destination
     * @param fileToBeAppended
     * @throws Exception
     */
    public static void concatFiles(File destination, File fileToBeAppended) throws Exception {
        appendToFile(destination, fileToBeAppended);
    }

    /**
     * This will append one file to the destination.
     * @param destination
     * @param fileToBeAppended
     * @throws Exception
     */
    public static void appendToFile(File destination, File fileToBeAppended) throws Exception {
        FileInputStream fis = new FileInputStream(fileToBeAppended);
        appendToFile(destination, fis);
        fis.close();
    }

    /**
     * @see #saveToFile(String, String, String, boolean)
     * 
     * @param fileName
     * @param dataToSave
     * @param encoding
     * @return
     */
    public static boolean appendToFile(String fileName, String dataToSave, String encoding) {
        return saveToFile(fileName, dataToSave, encoding, true);
    }

    /**
     * @see #saveToFile(String, String, String, boolean)
     * 
     * @param fileName
     * @param dataToSave
     * @return
     */
    public static boolean appendToFile(String fileName, String dataToSave) {
        return appendToFile(fileName, dataToSave, "UTF-8");
    }

    /**
     * @see #saveToFile(String, String, String, boolean)
     * 
     * @param fileName
     * @param dataToSave
     * @param encoding
     * @return
     */
    public static boolean saveToFile(String fileName, String dataToSave, String encoding) {
        return saveToFile(fileName, dataToSave, encoding, false);
    }

    /**
     * @see #saveToFile(String, String, String, boolean)
     * 
     * @param fileName
     * @param dataToSave
     * @return
     */
    public static boolean saveToFile(String fileName, String dataToSave) {
        return saveToFile(fileName, dataToSave, "UTF-8", false);
    }

    /**
     * This will save the string (dataToSave) to the file with the encoding and appends or overwrite.
     * 
     * @param fileName File path to save to.
     * @param dataToSave String with the data.
     * @param encoding The encoding of the output file.
     * @param append Indicates if the data is to be appended to the file.
     * @return true if succeeds
     */
    public static boolean saveToFile(String fileName, String dataToSave, String encoding, boolean append) {
        String actualEncoding = encoding;
        try {
            Writer out;
            if (!fileName.equals("")) {
                File fx = (new File(fileName)).getAbsoluteFile();
                if (!fx.exists()) {
                    File fx1 = new File(fx.getParent());
                    if (!fx1.isDirectory() && !fx1.isFile())
                        fx1.mkdirs();
                    fx.createNewFile();
                }
                if (!fileName.equals(fx.toString()))
                    fileName = fx.toString();
                try {
                    out = new OutputStreamWriter(new FileOutputStream(fileName, append), encoding);
                }
                catch (UnsupportedEncodingException UEe) {
                    System.err.println("\n-- UnsupportedEncodingException\n");
                    System.err.flush();
                    out = new OutputStreamWriter(new FileOutputStream(fileName, append), "iso8859-1");
                    actualEncoding = "iso8859-1";
                }
            }
            else {
                out = new OutputStreamWriter(System.out, "iso8859-1");
                actualEncoding = "iso8859-1";
            }

            NeptusLog.waste().debug(FileUtil.class + " Output to file as (" + actualEncoding + ") \n" + dataToSave);
            out.write(dataToSave.toString());
            out.flush();
            out.close();
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("saveToFile", e);
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), "File not found",
                    "<html>An error occured while saving the file: <br><strong>" + e.getMessage() + "</strong></html>");
            return false;
        }
        catch (UnsupportedEncodingException e) {
            NeptusLog.pub().error("saveToFile", e);
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), "Encoding not supported: " + actualEncoding,
                    "<html>An error occured while saving the file: <br><strong>" + e.getMessage() + "</strong></html>");
            return false;
        }
        catch (IOException e) {
            NeptusLog.pub().error("saveToFile", e);
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), "Input/Output error",
                    "<html>An error occured while saving the file: <br><strong>" + e.getMessage() + "</strong></html>");
            return false;
        }
        return true;
    }

    /**
     * This will create a copy of the given file with the bak extension appended into the name.
     * 
     * @param source
     * @return
     */
    public static boolean backupFile(String source) {
        File fx = new File(source);
        NeptusLog.pub().debug("FileUtil::copyFileAndBackup: " + fx.exists());
        if (fx.exists()) {
            boolean bl = FileUtil.copyFile(fx.getAbsolutePath(), fx.getAbsolutePath().concat(".bak"));
            NeptusLog.pub().debug("FileUtil::copyFileAndBackupRes: " + bl);
            return true;
        }
        return false;
    }

    /**
     * This will copy the file to some other and also
     * creates a copy of the given file with the bak extension appended into the name.
     * 
     * @param source
     * @param out
     * @return
     */
    public static boolean copyFileAndBackup(String source, String out) {
        File fx = new File(out);
        NeptusLog.pub().debug("FileUtil::copyFileAndBackup: " + fx.exists());
        if (fx.exists()) {
            boolean bl = FileUtil.copyFile(fx.getAbsolutePath(), fx.getAbsolutePath().concat(".bak"));
            NeptusLog.pub().debug("FileUtil::copyFileAndBackupRes: " + bl);
        }
        return copyFile(source, out);
    }

    /**
     * This will copy the file to some other.
     * 
     * @param source
     * @param out
     * @return
     */
    public static boolean copyFile(String source, String dest) {
        boolean ret = false;
        try {
            File in = new File(source);
            File out = new File(dest);
            FileInputStream fis = new FileInputStream(in);
            ret = StreamUtil.copyStreamToFile(fis, out);
            try {
                fis.close();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
        return ret;
    }

    /**
     * This will copy a file to a given folder.
     * 
     * @param source
     * @param destDir
     * @return
     */
    public static boolean copyFileToDir(String source, String destDir) {
        try {
            File in = new File(source);
            File out = new File(destDir, in.getName());
            FileInputStream fis = new FileInputStream(in);
            boolean ret = StreamUtil.copyStreamToFile(fis, out);
            try {
                fis.close();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            return ret;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
    }

    /**
     * This will try to relativize the file path in relation to the other as an URI.
     * 
     * @param parentPath The path to relativize to
     * @param filePath The path to be relativized
     * @return
     */
    public static String relativizeFilePathAsURI(String parentPath, String filePath) {
        String st = relativizeFilePath(parentPath, filePath);
        st = st.replace('\\', '/').toString();
        NeptusLog.pub().debug("relativizeFilePathAsURI   > " + st);
        return st;
    }

    /**
     * This will try to relativize the file path in relation to the other.
     * 
     * @param parentPath The path to relativize to
     * @param filePath The path to be relativized
     * @return
     */
    public static String relativizeFilePath(String parentPath, String filePath) {

        NeptusLog.pub().debug("relativizeFilePath:parentPath > " + parentPath);
        NeptusLog.pub().debug("relativizeFilePath:filePath > " + filePath);

        File fxP = new File(parentPath);
        File fxF = new File(filePath);

        // FIXME Check if is a non-existent folder
        if (!fxP.isDirectory())
            fxP = fxP.getParentFile().getAbsoluteFile();
        
        if (fxF.exists())
            fxF = fxF.getAbsoluteFile();

        String pS, fS;
        try {
            pS = fxP.getCanonicalPath();
        }
        catch (IOException e) {
            pS = fxP.getAbsolutePath();
        }
        try {
            fS = fxF.getCanonicalPath();
        }
        catch (IOException e) {
            fS = fxF.getAbsolutePath();
        }

        String res = fS;
        int lev = 0;
        if (fS.regionMatches(0, pS, 0, pS.length())) {
            int len = pS.length() + 1;
            if (pS.endsWith("\\") || pS.endsWith("/"))
                len--;
            res = fS.substring(len);
        }
        else {
            int i = 0;
            String tmp = pS;
            String fxSep = System.getProperty("file.separator", "/");
            while (i != -1) {
                i = tmp.lastIndexOf(fxSep);
                if (i == -1) {
                    lev = 0;
                    break;
                }
                int j = tmp.indexOf(fxSep);
                if (j == i) {
                    lev = 0;
                    break;
                }
                lev++;
                tmp = tmp.substring(0, i);
                if (fS.regionMatches(0, tmp, 0, tmp.length())) {
                    res = fS.substring(tmp.length() + 1, fS.length());
                    break;
                }
            }
            if (lev > 0) {
                for (int k = 0; k < lev; k++)
                    res = ".." + fxSep + res;
            }
        }

        NeptusLog.pub().debug("relativizeFilePath:parent > " + pS);
        NeptusLog.pub().debug("relativizeFilePath:file   > " + fS);
        NeptusLog.pub().debug("relativizeFilePath:result > " + res);

        return res;
    }

    /**
     * Deletes the directory and its content. If dir is a file only deletes the file. If the directory does not exist,
     * nothing will be done.
     * 
     * @param dir
     */
    public static void deltree(String dir) {
        String sep = System.getProperty("file.separator");
        File directory = new File(dir);
        if (!directory.exists())
            return;
        if (!directory.isDirectory()) {
            directory.delete();
        }
        String[] files = directory.list();
        if (files == null)
            return;

        for (String f : files) {
            File tmp = new File(directory.getAbsolutePath() + sep + f);
            if (tmp.isDirectory()) {
                deltree(tmp.getAbsolutePath());
            }
            else {
                try {
                    tmp.delete();
                }
                catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
        directory.delete();
    }

    /**
     * This will transform a path into a URL.
     * 
     * @param path
     * @return
     * @throws MalformedURLException
     */
    public static URL pathToURL(String path) throws MalformedURLException {
        URL retval = null;
        if (path == null) {
            return null;
        }

        // hack: for win32 we check drive specifier
        // for solaris we check startsWith /

        // this really should all take a clean look at URL(context,spec)

        if (!path.startsWith(java.io.File.separator) && (path.indexOf(':') != 1)) {
            path = System.getProperties().getProperty("user.dir") + '/' + path;
        }

        // switch from file separator to URL separator
        path = path.replace(java.io.File.separatorChar, '/');

        retval = new URL("file:" + path);
        return retval;
    }

    /**
     * This will return the caller class. Is used in {@link #getResourceAsFile(String)}
     * and {@link #getResourceAsFileKeepName(String)}.
     * 
     * @return
     */
    private static Class<?> getCallerClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String className = stack[3].getClassName();
        if (className.startsWith(FileUtil.class.getName())) {
            className = stack[4].getClassName();
        }
        try {
            Class<?> clazz = Class.forName(className);
            return clazz;
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    /**
     * Return a resource path as a path. 
     * It can be a absolute path of a relative path to the caller.
     * The resource is extracted as a temporary file.
     * 
     * @param name
     * @return
     */
    public static String getResourceAsFile(String name) {
        InputStream inStream = getResourceAsStream(name);
        if (inStream == null)
                return null;

        try {
            return StreamUtil.copyStreamToTempFile(inStream).getPath();
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Return a resource path as a input stream. 
     * It can be a absolute path of a relative path to the caller.
     * 
     * @param name
     * @return
     */
    public static InputStream getResourceAsStream(String name) {
        InputStream inStream = FileUtil.class.getResourceAsStream(name.replace('\\', '/'));
        if (inStream == null) {
            Class<?> clazz = getCallerClass();
            if (clazz == null)
                return null;
            inStream = clazz.getResourceAsStream(name.replace('\\', '/'));
            if (inStream == null)
                return null;
        }
        
        return inStream;
    }

    /**
     * Return a resource path as a path. The name of the file match the resource name.
     * The resource is extracted as a temporary file.
     * 
     * @param name
     * @return
     */
    public static String getResourceAsFileKeepName(String name) {
        String nameSlashed = name.replace('\\', '/');
        InputStream inStream = null;
        
        Class<?> clazz = getCallerClass();
        if (clazz != null) {
            inStream = clazz.getResourceAsStream(nameSlashed);
            if (inStream == null)
                inStream = clazz.getResourceAsStream("/" + nameSlashed);
        }
        if (inStream == null)
            inStream = FileUtil.class.getResourceAsStream(nameSlashed);
        if (inStream == null)
            inStream = FileUtil.class.getResourceAsStream("/" + nameSlashed);
        if (inStream == null)
            return null;

        try {
            File fx;
            File tmpDir = new File(ConfigFetch.getNeptusTmpDir());
            File nfx = new File(name);
            fx = new File(tmpDir, nfx.getName());
            fx.getParentFile().mkdirs();
            try {
                fx.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            fx.deleteOnExit();
            boolean ret = StreamUtil.copyStreamToFile(inStream, fx);
            try {
                inStream.close();
            }
            catch (IOException e) {
                NeptusLog.waste().error("copyStreamToFile", e);
            }
            return ret ? fx.getPath() : null;
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Loads a library from resource using {@link System} load.
     * 
     * @param libResourcePath
     * @return
     */
    public static boolean loadLibraryFromResource(String libResourcePath) {
        try {
            String path = FileUtil.getResourceAsFileKeepName(libResourcePath);
            System.load(path);
            return true;
        }
        catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Return the Object package as a path.
     * 
     * @param obj
     * @return
     */
    public static String getPackageAsPath(Object obj) {
        if (obj == null)
            return "";
        return getPackageAsPath(obj.getClass());
    }

    /**
     * Return the Class package as a path.
     * 
     * @param clazz
     * @return
     */
    public static String getPackageAsPath(Class<?> clazz) {
        if (clazz == null)
            return "";
        String path = clazz.getPackage().getName().replace('.', '/');
        return path;
    }

    public static void main(String[] args) throws IOException {
        ConfigFetch.initialize();
        NeptusLog.pub().info("<###> "+relativizeFilePath("D:\\FEUP\\NeptusProj\\neptus_ini\\testemission.xml",
                "D:\\FEUP\\NeptusProj\\neptus_ini\\teste\\cl.xml"));

        NeptusLog.pub().info("<###> "+relativizeFilePath("D:/cl.xml", "D:/FEUP/NeptusProj/neptus_ini/teste/mission.xml"));

        File f = new File("C:/AUTOEXEC.BAT");
        NeptusLog.pub().info("<###> "+getFileExtension(f));
        f = new File("C:/AUTOEXEC");
        NeptusLog.pub().info("<###> "+getFileExtension(f));

        f = new File("D:\\FEUP\\NeptusProj\\neptus_ini\\missions\\alfena\\MISSIO~1.NMI");
        NeptusLog.pub().info("<###> "+f.getAbsolutePath());
        NeptusLog.pub().info("<###> "+f.getCanonicalPath());
        NeptusLog.pub().info("<###> "+f.canRead());

        NeptusLog.pub().info("<###> "+relativizeFilePathAsURI("D:\\FEUP\\NeptusProj\\neptus_ini\\testemission.xml",
                "D:\\FEUP\\NeptusProj\\neptus_ini\\teste\\cl.xml"));
    }
}

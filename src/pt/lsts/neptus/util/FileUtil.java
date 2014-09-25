/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * @author Paulo Dias
 */
public class FileUtil {
    public static final byte[] BOM_UTF8 = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    public static final byte[] BOM_UTF16LE = new byte[] { (byte) 0xFF, (byte) 0xFE };
    public static final byte[] BOM_UTF16BE = new byte[] { (byte) 0xFE, (byte) 0xFF };
    public static final byte[] BOM_UTF32LE = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFE };
    public static final byte[] BOM_UTF32BE = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF };

    public static final String FILE_TYPE_MISSION = "nmis";
    public static final String FILE_TYPE_MISSION_COMPRESSED = "nmisz";
    public static final String FILE_TYPE_MAP = "nmap";
    public static final String FILE_TYPE_CONSOLE = "ncon";
    public static final String FILE_TYPE_CONFIG = "ncfg";
    public static final String FILE_TYPE_VEHICLE = "nvcl";
    public static final String FILE_TYPE_CHECKLIST = "nchk";
    public static final String FILE_TYPE_WSN = "nwsn";
    public static final String FILE_TYPE_INI = "ini";
    public static final String FILE_TYPE_RMF = "rmf";
    public static final String FILE_TYPE_XML = "xml";
    public static final String FILE_TYPE_LSF = "lsf";
    public static final String FILE_TYPE_LSF_COMPRESSED = "lsf.gz";
    public static final String FILE_TYPE_LSF_COMPRESSED_BZIP2 = "lsf.bz2";

    /**
     * Empty constructor
     */
    private FileUtil() {
    }

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

    public static String getFileExtension(String path) {
        int lastDotPostion = path.lastIndexOf('.');
        return (lastDotPostion != -1) ? (path.substring(lastDotPostion + 1)) : "";
    }
    
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
    
    public static String getFileNameWithoutExtension(String path) {
        File f = new File(path);
        String fname = f.getName();

        int lastDotPostion = fname.lastIndexOf('.');
        String ret = (lastDotPostion != -1) ? (fname.substring(0, lastDotPostion)) : fname;
        return ret;
    }
    

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

    public static String replaceFileExtension(String path, String newExtension) {
        int lastDotPostion = path.lastIndexOf('.');
        String st = (lastDotPostion != -1) ? (path.substring(0, lastDotPostion)) : path;
        return st + "." + newExtension;
    }


    /**
     * See {@link #checkFileForExtensions(String, String...)}.
     * @param file
     * @param extensions
     * @return
     */
    public static String checkFileForExtensions(File file, String... extensions) {
        return checkFileForExtensions(file.getName(), extensions);
    }

    /**
     * Checks a file name for matching a file extension (case insensitive).
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
     * @param fx
     * @return
     */
    public static String getFileAsString(File fx) {
        return getFileAsString(fx.getAbsolutePath());
    }

    /**
     * @param url
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

            // fis.close();

            // NeptusLog.pub().debug(FileUtil.class + " Input file as (" +
            // actualEncoding + ") \n" + result);

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

    private static File[] getFilesFromDiskWorker(File folderToLoad, final String searchPattern, final boolean justFolders) {
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
                            return justFolders ? file.isDirectory() : file.isFile();
                        }
                        else {
                            Matcher m = pat.matcher(name);
                            boolean ret = m.find();
                            return ret ? (justFolders ? file.isDirectory() : file.isFile()) : false;
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
     * @param folderToLoad
     * @param filePattern
     * @return
     */
    public static File[] getFilesToLoadFromDisk(File folderToLoad, final String searchPattern) {
        return getFilesFromDiskWorker(folderToLoad, searchPattern, false);
    }

    /**
     * @param url
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

            if ((ba[0] == BOM_UTF8[0]) && (ba[1] == BOM_UTF8[1]) && (ba[2] == BOM_UTF8[2])) {
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
     * @param xml
     * @return
     */
    public static String getAsPrettyPrintFormatedXMLString(String xml) {
        return getAsPrettyPrintFormatedXMLString(xml, false);
    }

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
     * @param xml
     * @return
     */
    public static String getAsCompactFormatedXMLString(String xml) {
        return getAsCompactFormatedXMLString(xml, false);
    }

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
     * @param doc
     * @return
     */
    public static String getAsPrettyPrintFormatedXMLString(Document doc) {
        return getAsPrettyPrintFormatedXMLString(doc, false);
    }

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
     * @param doc
     * @return
     */
    public static String getAsCompactFormatedXMLString(Document doc) {
        return getAsCompactFormatedXMLString(doc, false);
    }

    public static String getAsCompactFormatedXMLString(Document doc, boolean omitDeclaration) {
        // Compact format
        OutputFormat format = OutputFormat.createCompactFormat();
        format.setSuppressDeclaration(omitDeclaration);
        return getAsFormatedXMLString(doc, format);
    }

    /**
     * @param doc
     * @param format
     * @return
     */
    private static String getAsFormatedXMLString(Document doc, OutputFormat format) {
        String enc = doc.getXMLEncoding();
        if (enc == null)
            enc = "UTF-8";
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        // PrettyPrint format
        // OutputFormat format = OutputFormat.createPrettyPrint();
        // Compact format
        // OutputFormat format = OutputFormat.createCompactFormat();
        XMLWriter writer;
        try {
            writer = new XMLWriter(ba, format);
            writer.write(doc);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // NeptusLog.pub().info("<###> "+ba.toString());
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

    public static void concatFiles(File destination, File fileToBeAppended) throws Exception {
        appendToFile(destination, fileToBeAppended);
    }

    public static void appendToFile(File destination, File fileToBeAppended) throws Exception {
        FileInputStream fis = new FileInputStream(fileToBeAppended);
        appendToFile(destination, fis);
        fis.close();
    }

    /**
     * @see #saveToFile(String, String, String, boolean)
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
     * @param fileName
     * @param dataToSave
     * @return
     */
    public static boolean appendToFile(String fileName, String dataToSave) {
        return appendToFile(fileName, dataToSave, "UTF-8");
    }

    /**
     * @see #saveToFile(String, String, String, boolean)
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
     * @param fileName
     * @param dataToSave
     * @return
     */
    public static boolean saveToFile(String fileName, String dataToSave) {
        return saveToFile(fileName, dataToSave, "UTF-8", false);
    }

    /**
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

    public static String relativizeFilePathAsURI(String parentPath, String filePath) {
        String st = relativizeFilePath(parentPath, filePath);
        // URI uri = new File(st).toURI();
        // NeptusLog.pub().debug("relativizeFilePathAsURI > " + uri.toString());
        // return uri.toString();

        st = st.replace('\\', '/').toString();
        NeptusLog.pub().debug("relativizeFilePathAsURI   > " + st);
        return st;
    }

    /**
     * @param parentPath
     * @param filePath
     * @return
     */
    public static String relativizeFilePath(String parentPath, String filePath) {

        NeptusLog.pub().debug("relativizeFilePath:parentPath > " + parentPath);
        NeptusLog.pub().debug("relativizeFilePath:filePath > " + filePath);

        File fxP = new File(parentPath);
        File fxF = new File(filePath);
        // URI uriP = fxP.toURI();
        // URI uriF = fxF.toURI();

        // if (fxP.exists())
        // {
        // if (fxP.isFile())

        // FIXME Verificar se é um directório não existente
        if (!fxP.isDirectory())
            fxP = fxP.getParentFile().getAbsoluteFile();
        // }
        if (fxF.exists())
            fxF = fxF.getAbsoluteFile();

        String pS, fS;
        try {
            pS = fxP.getCanonicalPath();
            // NeptusLog.pub().info("<###> "+pS);
        }
        catch (IOException e) {
            // e.printStackTrace();
            pS = fxP.getAbsolutePath();
        }
        try {
            fS = fxF.getCanonicalPath();
        }
        catch (IOException e) {
            // e.printStackTrace();
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
                // NeptusLog.pub().info("<###> "+tmp);
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
                // NeptusLog.pub().info("<###>deleting "+tmp.getAbsolutePath());
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
    
    public static String getResourceAsFile(String name) {
        InputStream inStream = FileUtil.class.getResourceAsStream(name.replace('\\', '/'));
        if (inStream == null) {
            Class<?> clazz = getCallerClass();
            if (clazz == null)
                return null;
            inStream = clazz.getResourceAsStream(name.replace('\\', '/'));
            if (inStream == null)
                return null;
        }
        try {
            return StreamUtil.copyStreamToTempFile(inStream).getPath();
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    public static String getResourceAsFileKeepName(String name) {
        InputStream inStream = FileUtil.class.getResourceAsStream(name.replace('\\', '/'));
        if (inStream == null) {
            Class<?> clazz = getCallerClass();
            if (clazz == null)
                return null;
            inStream = clazz.getResourceAsStream(name.replace('\\', '/'));
            if (inStream == null)
                return null;
        }
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
                // TODO Auto-generated catch block
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

/*
 * Copyright  2001-2005 The Apache Software Foundation
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

package org.apache.tools.ant.util;

import java.io.File;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.launch.Locator;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * This class also encapsulates methods which allow Files to be
 * referred to using abstract path names which are translated to native
 * system file paths at runtime as well as copying files or setting
 * their last modification time.
 *
 */
public class FileUtilsStriped {

    private static final FileUtilsStriped PRIMARY_INSTANCE = new FileUtilsStriped();

    private static boolean onNetWare = Os.isFamily("netware");
    private static boolean onDos = Os.isFamily("dos");

    // for toURI
    private static boolean[] isSpecial = new boolean[256];
    private static char[] escapedChar1 = new char[256];
    private static char[] escapedChar2 = new char[256];

    /**
     * The granularity of timestamps under FAT.
     */
    public static final long FAT_FILE_TIMESTAMP_GRANULARITY = 2000;

    /**
     * The granularity of timestamps under Unix.
     */
    public static final long UNIX_FILE_TIMESTAMP_GRANULARITY = 1000;


    // stolen from FilePathToURI of the Xerces-J team
    static {
        for (int i = 0; i <= 0x20; i++) {
            isSpecial[i] = true;
            escapedChar1[i] = Character.forDigit(i >> 4, 16);
            escapedChar2[i] = Character.forDigit(i & 0xf, 16);
        }
        isSpecial[0x7f] = true;
        escapedChar1[0x7f] = '7';
        escapedChar2[0x7f] = 'F';
        char[] escChs = {'<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            isSpecial[ch] = true;
            escapedChar1[ch] = Character.forDigit(ch >> 4, 16);
            escapedChar2[ch] = Character.forDigit(ch & 0xf, 16);
        }
    }

    /**
     * Factory method.
     *
     * @return a new instance of FileUtils.
     */
    public static FileUtilsStriped newFileUtils() {
        return new FileUtilsStriped();
    }

    /**
     * Method to retrieve The FileUtils, which is shared by all users of this
     * method.
     * @return an instance of FileUtils.
     * @since Ant 1.6.3
     */
    public static FileUtilsStriped getFileUtils() {
        return PRIMARY_INSTANCE;
    }

    /**
     * Empty constructor.
     */
    protected FileUtilsStriped() {
    }


    /**
     * Calls File.setLastModified(long time). Originally written to
     * to dynamically bind to that call on Java1.2+.
     *
     * @param file the file whose modified time is to be set
     * @param time the time to which the last modified time is to be set.
     *             if this is -1, the current time is used.
     */
    public void setFileLastModified(File file, long time) {
        file.setLastModified((time < 0) ? System.currentTimeMillis() : time);
    }

    /**
     * Interpret the filename as a file relative to the given file
     * unless the filename already represents an absolute filename.
     *
     * @param file the "reference" file for relative paths. This
     * instance must be an absolute file and must not contain
     * &quot;./&quot; or &quot;../&quot; sequences (same for \ instead
     * of /).  If it is null, this call is equivalent to
     * <code>new java.io.File(filename)</code>.
     *
     * @param filename a file name.
     *
     * @return an absolute file that doesn't contain &quot;./&quot; or
     * &quot;../&quot; sequences and uses the correct separator for
     * the current platform.
     */
    public File resolveFile(File file, String filename) {
        filename = filename.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);

        // deal with absolute files
        if (isAbsolutePath(filename)) {
            return normalize(filename);
        }
        if (file == null) {
            return new File(filename);
        }
        File helpFile = new File(file.getAbsolutePath());
        StringTokenizer tok = new StringTokenizer(filename, File.separator);
        while (tok.hasMoreTokens()) {
            String part = tok.nextToken();
            if (part.equals("..")) {
                helpFile = helpFile.getParentFile();
                if (helpFile == null) {
                    String msg = "The file or path you specified ("
                        + filename + ") is invalid relative to "
                        + file.getPath();
                    throw new BuildException(msg);
                }
            } else if (part.equals(".")) {
                // Do nothing here
            } else {
                helpFile = new File(helpFile, part);
            }
        }
        return new File(helpFile.getAbsolutePath());
    }

    /**
     * Verifies that the specified filename represents an absolute path.
     * @param filename the filename to be checked.
     * @return true if the filename represents an absolute path.
     */
    public static boolean isAbsolutePath(String filename) {
        if (filename.startsWith(File.separator)) {
            // common for all os
            return true;
        }
        if (onDos && filename.length() >= 2
            && Character.isLetter(filename.charAt(0))
            && filename.charAt(1) == ':') {
            // Actually on windows the : must be followed by a \ for
            // the path to be absolute, else the path is relative
            // to the current working directory on that drive.
            // (Every drive may have another current working directory)
            return true;
        }
        return (onNetWare && filename.indexOf(":") > -1);
    }

    /**
     * &quot;Normalize&quot; the given absolute path.
     *
     * <p>This includes:
     * <ul>
     *   <li>Uppercase the drive letter if there is one.</li>
     *   <li>Remove redundant slashes after the drive spec.</li>
     *   <li>Resolve all ./, .\, ../ and ..\ sequences.</li>
     *   <li>DOS style paths that start with a drive letter will have
     *     \ as the separator.</li>
     * </ul>
     * Unlike <code>File#getCanonicalPath()</code> this method
     * specifically does not resolve symbolic links.
     *
     * @param path the path to be normalized.
     * @return the normalized version of the path.
     *
     * @throws java.lang.NullPointerException if the file path is
     * equal to null.
     */
    public File normalize(String path) {
        String orig = path;

        path = path.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);

        // make sure we are dealing with an absolute path
        int colon = path.indexOf(":");

        if (!isAbsolutePath(path)) {
            String msg = path + " is not an absolute path";
            throw new BuildException(msg);
        }
        boolean dosWithDrive = false;
        String root = null;
        // Eliminate consecutive slashes after the drive spec
        if ((onDos && path.length() >= 2
                && Character.isLetter(path.charAt(0))
                && path.charAt(1) == ':')
            || (onNetWare && colon > -1)) {

            dosWithDrive = true;

            char[] ca = path.replace('/', '\\').toCharArray();
            StringBuffer sbRoot = new StringBuffer();
            for (int i = 0; i < colon; i++) {
                sbRoot.append(Character.toUpperCase(ca[i]));
            }
            sbRoot.append(':');
            if (colon + 1 < path.length()) {
                sbRoot.append(File.separatorChar);
            }
            root = sbRoot.toString();

            // Eliminate consecutive slashes after the drive spec
            StringBuffer sbPath = new StringBuffer();
            for (int i = colon + 1; i < ca.length; i++) {
                if ((ca[i] != '\\')
                    || (ca[i] == '\\' && ca[i - 1] != '\\')) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString().replace('\\', File.separatorChar);
        } else {
            if (path.length() == 1) {
                root = File.separator;
                path = "";
            } else if (path.charAt(1) == File.separatorChar) {
                // UNC drive
                root = File.separator + File.separator;
                path = path.substring(2);
            } else {
                root = File.separator;
                path = path.substring(1);
            }
        }
        Stack<String> s = new Stack<String>();
        s.push(root);
        StringTokenizer tok = new StringTokenizer(path, File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            } else if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    throw new BuildException("Cannot resolve path " + orig);
                } else {
                    s.pop();
                }
            } else { // plain component
                s.push(thisToken);
            }
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }
        path = sb.toString();
        if (dosWithDrive) {
            path = path.replace('/', '\\');
        }
        return new File(path);
    }

    /**
     * Constructs a file path from a <code>file:</code> URI.
     *
     * <p>Will be an absolute path if the given URI is absolute.</p>
     *
     * <p>Swallows '%' that are not followed by two characters,
     * doesn't deal with non-ASCII characters.</p>
     *
     * @param uri the URI designating a file in the local filesystem.
     * @return the local file system path for the file.
     * @since Ant 1.6
     */
    public String fromURI(String uri) {
        String path = Locator.fromURI(uri);

        // catch exception if normalize thinks this is not an absolute path
        try {
            path = normalize(path).getAbsolutePath();
        } catch (BuildException e) {
            // relative path
        }
        return path;
    }

    /**
     * This was originally an emulation of {@link File#getParentFile} for JDK 1.1,
     * but it is now implemented using that method (Ant 1.6.3 onwards).
     * @param f the file whose parent is required.
     * @return the given file's parent, or null if the file does not have a
     *         parent.
     * @since 1.10
     */
    public File getParentFile(File f) {
        return (f == null) ? null : f.getParentFile();
    }

 }


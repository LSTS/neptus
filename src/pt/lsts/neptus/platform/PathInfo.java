/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology, Lda.
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
 */
package pt.lsts.neptus.platform;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Neptus Filesystem Paths.
 * <p>
 * This class provides information about the filesystem paths used by Neptus. On initialization this class will try to
 * find the root folder of the Neptus distribution based on some heuristics. If this procedure fails a RuntimeException
 * will be thrown and this class will be rendered useless.
 *
 * @author Ricardo Martins
 */
public final class PathInfo {
    /** Name of the native libraries root folder. */
    private final static String JNI_FOLDER = "libJNI";
    /** Name of the configuration root folder. */
    private final static String CFG_FOLDER = "conf";
    /** Name of the byte-compiled binaries root folder. */
    private final static String BIN_FOLDER = "bin";
    /** Name of the bundled Java libraries root folder. */
    private final static String LIB_FOLDER = "lib";
    /** List of canonical folder names that must always exist in a Neptus distribution. */
    private final static String[] CANONICAL_FOLDERS = {JNI_FOLDER, CFG_FOLDER, BIN_FOLDER, LIB_FOLDER};
    /** Neptus distribution root folder. */
    private static final File neptusRoot = findRootFolder();
    /** Neptus folder containing native libraries. */
    private static final File neptusJniFolder = new File(neptusRoot, JNI_FOLDER);
    /** Neptus folder containing configuration files. */
    private static final File neptusCfgFolder = new File(neptusRoot, CFG_FOLDER);
    /** Neptus folder containing applications. */
    private static final File neptusBinFolder = new File(neptusRoot, BIN_FOLDER);
    /** Neptus folder containing Java libraries. */
    private static final File neptusLibFolder = new File(neptusRoot, LIB_FOLDER);
    /** List of folders containing platform specific libraries. */
    private static final List<File> platformJniFolders = findPlatformJniFolders();

    /**
     * Private constructor to prevent instantiation.
     */
    private PathInfo() {
    }

    /**
     * Retrieves the root folder of the Neptus distribution.
     *
     * @return Neptus root folder.
     */
    public static File getNeptusRoot() {
        return neptusRoot;
    }

    /**
     * Retrieves the folder containing the native libraries hierarchy.
     *
     * @return Neptus native libraries folder.
     */
    public static File getNeptusJniFolder() {
        return neptusJniFolder;
    }

    /**
     * Retrieves the list of folders containing native libraries specific to the current platform.
     *
     * @return list of folders containing native libraries.
     */
    public static List<File> getNeptusJniFolders() {
        return platformJniFolders;
    }

    /**
     * Retrieves the Neptus folder that contains configuration files.
     *
     * @return folder containing configuration files.
     */
    public static File getNeptusConfigFolder() {
        return neptusCfgFolder;
    }

    /**
     * Retrieves the Neptus folder that contains ready to run applications.
     *
     * @return folder containing applications.
     */
    public static File getNeptusBinFolder() {
        return neptusBinFolder;
    }

    /**
     * Retrieves the Neptus folder that contains Java libraries.
     *
     * @return folder containing Java libraries.
     */
    public static File getNeptusLibFolder() {
        return neptusLibFolder;
    }

    /**
     * Retrieves a Neptus configuration file.
     *
     * @param first the path string or initial part of the path string.
     * @param more  additional strings to be joined to form the path string.
     * @return folder containing Java libraries.
     */
    public static File getNeptusConfigFile(String first, String... more) {
        String base = new File(neptusCfgFolder, first).getAbsolutePath();
        return Paths.get(base, more).toFile();
    }

    /**
     * Finds folders containing native libraries suitable for the current platform.
     *
     * @return list of folders containing native libraries.
     */
    private static List<File> findPlatformJniFolders() {
        final String[][] candidateFolders = {
                {OsInfo.getNameString(), OsInfo.getArchString(), JvmInfo.getDataModelString()},
                {OsInfo.getNameString(), OsInfo.getArchString()}, {OsInfo.getNameString()}};

        List<File> paths = new ArrayList<>();

        for (String[] folder : candidateFolders) {
            File file = new File(PathInfo.getNeptusJniFolder(),
                    String.join(File.separator, folder));
            if (!file.isDirectory())
                continue;

            File[] files = file.listFiles();
            if (files == null)
                continue;

            for (final File child : files) {
                if (child.isFile()) {
                    paths.add(file.getAbsoluteFile());
                    break;
                }
            }
        }

        return paths;
    }

    /**
     * Finds the root folder of the Neptus distribution.
     *
     * @return Neptus root folder.
     * @throws RuntimeException if the root folder cannot be found.
     */
    private static File findRootFolder() {
        String[] paths = System.getProperties().getProperty("java.class.path")
                .split(File.pathSeparator);

        for (String path : paths) {
            File folder = new File(path).getAbsoluteFile().getParentFile().getParentFile();
            if (path.endsWith("neptus.jar") || isRootFolder(folder))
                return folder;
        }

        throw new RuntimeException("failed to find Neptus root folder");
    }

    /**
     * Tests if a given path is a Neptus root folder.
     *
     * @param folder folder to test.
     * @return true if the folder is a Neptus neptusRoot folder, false otherwise.
     */
    private static boolean isRootFolder(File folder) {
        for (String subfolder : CANONICAL_FOLDERS) {
            if (!new File(folder, subfolder).isDirectory())
                return false;
        }

        return true;
    }
}

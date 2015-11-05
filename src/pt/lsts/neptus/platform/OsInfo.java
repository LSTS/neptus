/*
 * Copyright (c) 2004-2015 OceanScan - Marine Systems & Technology, Lda.
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage Licencees holding valid commercial Neptus licences
 * may use this file in accordance with the commercial licence agreement
 * provided with the Software or, alternatively, in accordance with the terms
 * contained in a written agreement between you and Universidade do Porto. For
 * licensing terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage Alternatively, this file may
 * be used under the terms of the EUPL, Version 1.1 only (the "Licence"),
 * appearing in the file LICENCE.md included in the packaging of this file. You
 * may not use this work except in compliance with the Licence. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 */

package pt.lsts.neptus.platform;

/**
 * Unified Operating System Information.
 * <p>
 * This class provides information about the current Operating System in a unified format. The information about the
 * Operating System is collected and stored upon static initialization, therefore if the Operating System is not
 * recognized a RuntimeError exception will be thrown and this class will be rendered useless.
 *
 * @author Ricardo Martins
 */
public final class OsInfo {
    /** Enumeration of operating system families. */
    public enum Family {
        /** Microsoft Windows. */
        WINDOWS,
        /** Unix variants. */
        UNIX
    }

    /** Enumeration of operating system names. */
    public enum Name {
        /** Microsoft Windows. */
        WINDOWS,
        /** Linux. */
        LINUX,
        /** Apple Mac OS X. */
        MACOSX,
        /** Oracle Solaris. */
        SOLARIS
    }

    /** Enumeration of computer architectures. */
    public enum Architecture {
        /** Intel x86 and derivatives, including AMD64. */
        X86,
        /** ARM. */
        ARM
    }

    /** Machine data model (i.e., number of bits). */
    public enum DataModel {
        /** Data model is 32-bit. */
        B32,
        /** Data model is 64-bit. */
        B64
    }

    /** Operating system name. */
    private final static Name name;
    /** Operating system name. */
    private final static String nameString;
    /** Operating system version. */
    private final static String versionString;
    /** Operating system family. */
    private final static Family family;
    /** Operating system family. */
    private final static String familyString;
    /** Operating system architecture. */
    private final static Architecture arch;
    /** Operating system architecture. */
    private final static String archString;
    /** Operating system data model. */
    private final static DataModel dataModel;
    /** Operating system data model. */
    private final static String dataModelString;

    private OsInfo() {
    }

    /**
     * Retrieves the name of the operating system.
     *
     * @return name of the operating system.
     */
    public static Name getName() {
        return name;
    }

    /**
     * Retrieves the name of the operating system as a string.
     *
     * @return name of the operating system as a string.
     */
    public static String getNameString() {
        return nameString;
    }

    /**
     * Retrieves the version of the operating system as a string.
     *
     * @return version of the operating system as a string.
     */
    public static String getVersionString() {
        return versionString;
    }

    /**
     * Retrieves the family of the operating system.
     *
     * @return family of the operating system.
     */
    public static Family getFamily() {
        return family;
    }

    /**
     * Retrieves the family of the operating system as a string.
     *
     * @return family of the operating system as a string.
     */
    public static String getFamilyString() {
        return familyString;
    }

    /**
     * Retrieves the architecture of the operating system.
     *
     * @return architecture of the operating system.
     */
    public static Architecture getArch() {
        return arch;
    }

    /**
     * Retrieves the architecture of the operating system as a string.
     *
     * @return architecture of the operating system as a string.
     */
    public static String getArchString() {
        return archString;
    }

    /**
     * Retrieves the data model of the operating system.
     *
     * @return data model of the operating system.
     */
    public static DataModel getDataModel() {
        return dataModel;
    }

    /**
     * Retrieves the data model of the operating system as a string.
     *
     * @return data model of the operating system as a string.
     */
    public static String getDataModelString() {
        return dataModelString;
    }

    static {
        String osName = System.getProperty("os.name").toLowerCase().replaceAll(" ", "");
        if (osName.startsWith("win")) {
            name = Name.WINDOWS;
            family = Family.WINDOWS;
        } else if (osName.equals("linux")) {
            name = Name.LINUX;
            family = Family.UNIX;
        } else if (osName.startsWith("mac") && osName.endsWith("x")) {
            name = Name.MACOSX;
            family = Family.UNIX;
        } else if (osName.equals("solaris") || osName.equals("sunos")) {
            name = Name.SOLARIS;
            family = Family.UNIX;
        } else {
            throw new RuntimeException("unknown operating system");
        }

        versionString = System.getProperty("os.version").toLowerCase();
        nameString = name.toString().toLowerCase();
        familyString = family.toString().toLowerCase();
    }

    static {
        String osArch = System.getProperty("os.arch").toLowerCase();

        if (osArch.contains("x86-64") || osArch.contains("amd64") || osArch.contains("em64t")
                || osArch.contains("x86_64")) {
            arch = Architecture.X86;
            dataModel = DataModel.B64;
        } else if (osArch.equals("x86") || osArch.contains("pentium") || osArch.contains("i386")
                || osArch.contains("i486") || osArch.contains("i586") || osArch.contains("i686")) {
            arch = Architecture.X86;
            dataModel = DataModel.B32;
        } else if (osArch.startsWith("arm")) {
            arch = Architecture.ARM;
            dataModel = DataModel.B32;
        } else {
            throw new RuntimeException("unknown architecture");
        }

        archString = arch.toString().toLowerCase();
        dataModelString = dataModel.toString().toLowerCase();
    }

    public static void main(String[] args) {
        System.out.format("%-20s: %s\n", "OS - Name", getNameString());
        System.out.format("%-20s: %s\n", "OS - Version", getVersionString());
        System.out.format("%-20s: %s\n", "OS - Family", getFamilyString());
        System.out.format("%-20s: %s\n", "OS - Architecture", getArchString());
        System.out.format("%-20s: %s\n", "OS - Data Model", getDataModelString());
    }
}

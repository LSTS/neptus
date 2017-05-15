/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology, Lda.
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage Licencees holding valid commercial Neptus licences
 * may use this file in accordance with the commercial licence agreement
 * provided with the Software or, alternatively, in accordance with the terms
 * contained in a written agreement between you and Universidade do Porto. For
 * licensing terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage Alternatively, this file may
 * be used under the terms of the EUPL, Version 1.1 only (the "Licence"),
 * appearing in the file LICENCE.md included in the packaging of this file. You
 * may not use this work except in compliance with the Licence. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 */
package pt.lsts.neptus.platform;

import java.util.Locale;

/**
 * Operating System Information.
 * <p>
 * This class provides information about the current Operating System in a unified format. The information about the
 * Operating System is retrieved during static initialization, therefore if the Operating System is not recognized a
 * RuntimeException exception will be thrown and this class will be rendered useless.
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
    /** Operating system name as a string. */
    private final static String nameString;
    /** Operating system version as a string. */
    private final static String versionString;
    /** Operating system family. */
    private final static Family family;
    /** Operating system family as a string. */
    private final static String familyString;
    /** Operating system architecture. */
    private final static Architecture arch;
    /** Operating system architecture as a string. */
    private final static String archString;
    /** Operating system data model. */
    private final static DataModel dataModel;
    /** Operating system data model as a string. */
    private final static String dataModelString;

    /**
     * Private constructor to prevent instantiation.
     */
    private OsInfo() {
    }

    /**
     * Retrieves the name of the operating system. Use this method to perform comparisons.
     *
     * @return name of the operating system.
     */
    public static Name getName() {
        return name;
    }

    /**
     * Retrieves the name of the operating system as a string. Use this method exclusively to present information to the
     * user and never to perform comparisons.
     *
     * @return name of the operating system as a string.
     */
    public static String getNameString() {
        return nameString;
    }

    /**
     * Retrieves the version of the operating system as a string. The format of this information is implementation
     * dependent.
     *
     * @return version of the operating system as a string.
     */
    public static String getVersionString() {
        return versionString;
    }

    /**
     * Retrieves the family of the operating system. Use this method to perform comparisons.
     *
     * @return family of the operating system.
     */
    public static Family getFamily() {
        return family;
    }

    /**
     * Retrieves the family of the operating system as a string. Use this method exclusively to present information to
     * the user and never to perform comparisons.
     *
     * @return family of the operating system as a string.
     */
    public static String getFamilyString() {
        return familyString;
    }

    /**
     * Retrieves the architecture of the operating system. Use this method to perform comparisons.
     *
     * @return architecture of the operating system.
     */
    public static Architecture getArch() {
        return arch;
    }

    /**
     * Retrieves the architecture of the operating system as a string. Use this method exclusively to present
     * information to the user and never to perform comparisons.
     *
     * @return architecture of the operating system as a string.
     */
    public static String getArchString() {
        return archString;
    }

    /**
     * Retrieves the data model of the operating system. Use this method to perform comparisons.
     *
     * @return data model of the operating system.
     */
    public static DataModel getDataModel() {
        return dataModel;
    }

    /**
     * Retrieves the data model of the operating system as a string. Use this method exclusively to present information
     * to the user and never to perform comparisons.
     *
     * @return data model of the operating system as a string.
     */
    public static String getDataModelString() {
        return dataModelString;
    }

    /**
     * Retrieves a summary of the operating system information as a string. Use this method exclusively to present
     * information to the user and never to perform comparisons.
     *
     * @return summary of operating system information.
     */
    public static String getSummaryString() {
        return String.format("%s-%s-%s-%s-%s", getFamilyString(),
                getNameString(),
                getArchString(),
                getDataModelString(),
                getVersionString());
    }

    static {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US).replaceAll(" ", "");
        if (osName.startsWith("win")) {
            name = Name.WINDOWS;
            family = Family.WINDOWS;
        }
        else if (osName.equals("linux")) {
            name = Name.LINUX;
            family = Family.UNIX;
        }
        else if (osName.startsWith("mac") && osName.endsWith("x")) {
            name = Name.MACOSX;
            family = Family.UNIX;
        }
        else if (osName.equals("solaris") || osName.equals("sunos")) {
            name = Name.SOLARIS;
            family = Family.UNIX;
        }
        else {
            throw new RuntimeException("Unknown operating system");
        }

        versionString = System.getProperty("os.version").toLowerCase(Locale.US);
        nameString = name.toString().toLowerCase(Locale.US);
        familyString = family.toString().toLowerCase(Locale.US);
    }

    static {
        String osArch = System.getProperty("os.arch").toLowerCase(Locale.US);

        if (osArch.contains("x86-64") || osArch.contains("amd64") || osArch.contains("em64t")
                || osArch.contains("x86_64")) {
            arch = Architecture.X86;
            dataModel = DataModel.B64;
        }
        else if (osArch.equals("x86") || osArch.contains("pentium") || osArch.contains("i386")
                || osArch.contains("i486") || osArch.contains("i586") || osArch.contains("i686")) {
            arch = Architecture.X86;
            dataModel = DataModel.B32;
        }
        else if (osArch.startsWith("arm")) {
            arch = Architecture.ARM;
            dataModel = DataModel.B32;
        }
        else {
            throw new RuntimeException("Unknown architecture");
        }

        archString = arch.toString().toLowerCase(Locale.US);
        dataModelString = dataModel.toString().toLowerCase(Locale.US);
    }
}

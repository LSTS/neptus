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

import java.util.Locale;

/**
 * Java Virtual Machine Information.
 * <p>
 * This class provides information about the current Java Virtual Machine in a unified format. The information about the
 * Java Virtual Machine is retrieved during static initialization, therefore if the Java Virtual Machine is not
 * recognized a RuntimeException exception will be thrown and this class will be rendered useless.
 *
 * @author Ricardo Martins
 */
public final class JvmInfo {
    /** Enumeration of Java Virtual Machine vendors. */
    public enum Vendor {
        /** Oracle Corporation. */
        ORACLE,
        /** IBM Corporation. */
        IBM
    }

    /** Enumeration of Java Virtual Machine names. */
    public enum Name {
        /** Oracle HotSpot. */
        HOTSPOT,
        /** OpenJDK. */
        OPENJDK,
        /** IBM J9. */
        J9
    }

    /** Java Virtual Machine data model (i.e., number of bits). */
    public enum DataModel {
        /** Data model is 32-bit. */
        B32,
        /** Data model is 64-bit. */
        B64
    }

    /** Java Virtual Machine name. */
    private final static Name name = identifyJvmName();
    /** Java Virtual Machine name as a string. */
    private final static String nameString = name.toString().toLowerCase(Locale.US);
    /** Java Virtual Machine vendor. */
    private final static Vendor vendor = identifyJvmVendor();
    /** Java Virtual Machine vendor as a string. */
    private final static String vendorString = vendor.toString().toLowerCase(Locale.US);
    /** Java Virtual Machine data model. */
    private final static DataModel dataModel = identifyJvmDataModel();
    /** Java Virtual Machine data model as a string. */
    private final static String dataModelString = dataModel.toString().toLowerCase(Locale.US);
    /** Java Virtual Machine version. */
    private final static String versionString = System.getProperty("java.version").toLowerCase(Locale.US);

    /**
     * Private constructor to prevent instantiation.
     */
    private JvmInfo() {
    }

    /**
     * Retrieves the name of the Java Virtual Machine. Use this method to perform comparisons.
     *
     * @return name of the Java Virtual Machine.
     */
    public static Name getName() {
        return name;
    }

    /**
     * Retrieves the name of the Java Virtual Machine as a string. Use this method exclusively to present information to
     * the user and never to perform comparisons.
     *
     * @return name of the Java Virtual Machine as a string.
     */
    public static String getNameString() {
        return nameString;
    }

    /**
     * Retrieves the vendor of the Java Virtual Machine. Use this method to perform comparisons.
     *
     * @return vendor of the Java Virtual Machine.
     */
    public static Vendor getVendor() {
        return vendor;
    }

    /**
     * Retrieves the vendor of the Java Virtual Machine as a string. Use this method exclusively to present information
     * to the user and never to perform comparisons.
     *
     * @return vendor of the Java Virtual Machine as a string.
     */
    public static String getVendorString() {
        return vendorString;
    }

    /**
     * Retrieves the data model of the Java Virtual Machine. Use this method to perform comparisons.
     *
     * @return data model of the Java Virtual Machine.
     */
    public static DataModel getDataModel() {
        return dataModel;
    }

    /**
     * Retrieves the data model of the Java Virtual Machine as a string. Use this method exclusively to present
     * information to the user and never to perform comparisons.
     *
     * @return data model of the Java Virtual Machine as a string.
     */
    public static String getDataModelString() {
        return dataModelString;
    }

    /**
     * Retrieves the version of the Java Virtual Machine. The format of this information is implementation dependent.
     *
     * @return version of the Java Virtual Machine.
     */
    public static String getVersionString() {
        return versionString;
    }

    /**
     * Retrieves a summary of the Java Virtual Machine information as a string. Use this method exclusively to present
     * information to the user and never to perform comparisons.
     *
     * @return summary of the Java Virtual Machine information.
     */
    public static String getSummaryString() {
        return String.format("%s-%s-%s-%s", getVendorString(),
                getNameString(),
                getDataModelString(),
                getVersionString());
    }

    /**
     * Identifies the name of the Java Virtual Machine.
     *
     * @return name of the Java Virtual Machine.
     * @throws RuntimeException if the name of the Java Virtual Machine is not known.
     */
    private static Name identifyJvmName() {
        final String name = System.getProperty("java.vm.name").toLowerCase();

        if (name.startsWith("java hotspot"))
            return Name.HOTSPOT;
        else if (name.startsWith("openjdk"))
            return Name.OPENJDK;
        else if (name.startsWith("ibm j9"))
            return Name.J9;

        throw new RuntimeException("Unknown Java Virtual Machine: '" + name + "'");
    }

    /**
     * Identifies the vendor of the Java Virtual Machine.
     *
     * @return vendor of the Java Virtual Machine.
     * @throws RuntimeException if the vendor of the Java Virtual Machine is not known.
     */
    private static Vendor identifyJvmVendor() {
        String vendor = System.getProperty("java.vendor").toLowerCase();
        if (vendor.startsWith("oracle"))
            return Vendor.ORACLE;
        else if (vendor.startsWith("ibm"))
            return Vendor.IBM;

        throw new RuntimeException("Unknown Java Virtual Machine vendor: '" + vendor + "'");
    }

    /**
     * Identifies the data model of the Java Virtual Machine.
     *
     * @return data model of the Java Virtual Machine.
     * @throws RuntimeException if the data model of the Java Virtual Machine is not known.
     */
    private static DataModel identifyJvmDataModel() {
        final String model = System.getProperty("sun.arch.data.model").toLowerCase();

        if (model.startsWith("32"))
            return DataModel.B32;
        else if (model.startsWith("64"))
            return DataModel.B64;

        throw new RuntimeException("Unknown Java Virtual Machine data model: '" + model + "'");
    }
}

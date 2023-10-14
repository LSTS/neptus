/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * May 18, 2018
 */
package pt.lsts.neptus.util.netcdf.exporter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pt.lsts.neptus.NeptusLog;
import ucar.ma2.Array;
import ucar.ma2.ArrayBoolean;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayLong;
import ucar.ma2.ArrayShort;
import ucar.ma2.ArrayString;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
public class NetCDFVarElement {
    
    public enum ISO19115_1CodeSourceOfData {
        // ISO 19115-1 code to indicate the source of the data (image, thematicClassification, physicalMeasurement,
        // auxiliaryInformation, qualityInformation, referenceInformation, modelResult, or coordinate)
        image,
        thematicClassification,
        physicalMeasurement,
        auxiliaryInformation,
        qualityInformation,
        referenceInformation,
        modelResult,
        coordinate;
    }
    
    private String name;
    private String longName = null;
    private String standardName = null;
    private String units = null;
    private ISO19115_1CodeSourceOfData coverageContentType = null;
    
    private DataType dataType = null;
    private Group group = null;
    private List<Dimension> dimensions = null;

    private Map<String, Object> additionalAttrib = new LinkedHashMap<>(); 
    
    private Variable var = null;
    private Array dataArray = null;
    
    public NetCDFVarElement(String name) {
        this.name = name;
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @param name the name to set
     * @return 
     */
    public NetCDFVarElement setName(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * @return the longName
     */
    public String getLongName() {
        return longName;
    }
    
    /**
     * @param longName the longName to set
     * @return 
     */
    public NetCDFVarElement setLongName(String longName) {
        this.longName = longName;
        return this;
    }
    
    /**
     * @return the standardName
     */
    public String getStandardName() {
        return standardName;
    }
    
    /**
     * @param standardName the standardName to set
     * @return 
     */
    public NetCDFVarElement setStandardName(String standardName) {
        this.standardName = standardName;
        return this;
    }
    
    /**
     * @return the units
     */
    public String getUnits() {
        return units;
    }
    
    /**
     * @param units the units to set
     * @return 
     */
    public NetCDFVarElement setUnits(String units) {
        this.units = units;
        return this;
    }
    
    /**
     * @return the coverageContentType
     */
    public ISO19115_1CodeSourceOfData getCoverageContentType() {
        return coverageContentType;
    }
    
    /**
     * @param coverageContentType the coverageContentType to set
     * @return 
     */
    public NetCDFVarElement setCoverageContentType(ISO19115_1CodeSourceOfData coverageContentType) {
        this.coverageContentType = coverageContentType;
        return this;
    }
    
    public NetCDFVarElement setAtribute(String name, Object val) {
        if (name != null && !name.isEmpty())
            additionalAttrib.put(name, val);
        return this;
    }

    public NetCDFVarElement setAtribute(String name, Object[] val) {
        if (name != null && !name.isEmpty())
            additionalAttrib.put(name, val);
        return this;
    }

    public NetCDFVarElement removeAtribute(String name) {
        if (name != null && !name.isEmpty())
            additionalAttrib.remove(name);
        return this;
    }

    /**
     * @return the dataType
     */
    public DataType getDataType() {
        return dataType;
    }
    
    /**
     * @param dataType the dataType to set
     * @return 
     */
    public NetCDFVarElement setDataType(DataType dataType) {
        this.dataType = dataType;
        return this;
    }

    /**
     * @return
     */
    public Array createDataArray() {
        if (dataArray != null)
            return dataArray;
        
        int[] dim = dimensions.stream().mapToInt(d -> d.getLength()).toArray();
        switch (dataType) {
            case BOOLEAN:
                dataArray = new ArrayBoolean(dim);
                break;
            case BYTE:
                dataArray = new ArrayByte(dim, false);
                break;
            case UBYTE: // TODO Add support
                dataArray = new ArrayByte(dim, true);
                break;
            case CHAR:
                dataArray = new ArrayChar(dim);
                break;
            case DOUBLE:
                dataArray = new ArrayDouble(dim);
                break;
            case FLOAT:
                dataArray = new ArrayFloat(dim);
                break;
            case INT:
                dataArray = new ArrayInt(dim, false);
                break;
            case UINT: // TODO Add support
                dataArray = new ArrayInt(dim, true);
                break;
            case LONG:
                dataArray = new ArrayLong(dim, false);
                break;
            case ULONG: // TODO Add support
                dataArray = new ArrayLong(dim, true);
                break;
            case SHORT:
                dataArray = new ArrayShort(dim, false);
                break;
            case USHORT: // TODO Add support
                dataArray = new ArrayShort(dim, true);
                break;
            case STRING:
                dataArray = new ArrayString(dim);
            case ENUM1:
            case ENUM2:
            case ENUM4:
            case OBJECT:
            case OPAQUE:
            case SEQUENCE:
            case STRUCTURE:
            default:
                dataArray = null;
                break;
        }
        return dataArray;
    }
    
    public boolean insertData(boolean value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setBoolean(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(byte value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setByte(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(char value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setChar(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(double value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setDouble(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(float value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setFloat(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(int value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setInt(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(long value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setLong(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(short value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            dataArray.setShort(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(String value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            Index idx = setindexAt(index);
            ((ArrayString) dataArray).set(idx, value);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertData(char[] value, int... index) {
        try {
            if (dataArray == null)
                createDataArray();
            if (index.length > 0) {
                int[] idxT = Arrays.copyOf(index, index.length + 1);
                setindexAt(idxT);
            }
            else {
                setindexAt(0);
            }
            for (int i = 0; i < value.length; i++)
                dataArray.setChar(i, value[i]);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Index setindexAt(int... index) {
        Index idx = dataArray.getIndex();
        idx.set(index);
        return idx;
    }
    
    /**
     * @return the group
     */
    public Group getGroup() {
        return group;
    }
    
    /**
     * @param group the group to set
     * @return 
     */
    public NetCDFVarElement setGroup(Group group) {
        this.group = group;
        return this;
    }
    
    /**
     * @return the dimensions
     */
    public List<Dimension> getDimensions() {
        return dimensions;
    }
    
    /**
     * @param dimensions the dimensions to set
     * @return 
     */
    public NetCDFVarElement setDimensions(List<Dimension> dimensions) {
        this.dimensions = dimensions;
        return this;
    }
    
    public boolean writeVariable(NetcdfFileWriter writer) {
        try {
            var = writer.addVariable(group, name, dataType, dimensions);
            if (longName != null)
                var.addAttribute(new Attribute("long_name", longName));
            if (standardName != null)
                var.addAttribute(new Attribute("standard_name", standardName));
            if (units != null)
                var.addAttribute(new Attribute("units", units));
            if (coverageContentType != null)
                var.addAttribute(new Attribute("coverage_content_type", coverageContentType.toString()));
            
            additionalAttrib.keySet().stream().forEach(name -> {
                Object val = additionalAttrib.get(name);
                if (val == null)
                    return;
                
                try {
                    if (val.getClass().isArray())
                        var.addAttribute(new Attribute(name, extractedArrayForAttribute(val)));
                    else if (val.getClass().isAssignableFrom(String.class))
                        var.addAttribute(new Attribute(name, (String) val));
                    else if (val.getClass().isAssignableFrom(Attribute.class))
                        var.addAttribute(new Attribute(name, (Attribute) val));
                    else if (val.getClass().isAssignableFrom(Array.class))
                        var.addAttribute(new Attribute(name, (Array) val));
                    else if (val.getClass().isAssignableFrom(List.class))
                        var.addAttribute(new Attribute(name, (List<?>) val));
                    else {
                        try {
                            Number number = (Number) val;
                            var.addAttribute(new Attribute(name, number));   
                        }
                        catch (Exception e) {
                            throw new Exception("Not valid attribute type!");
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(String.format("Error while writting attribute '$s'!", name), e);
                }
            });
            
            if (dimensions == null || dimensions.size() == 0) {
                // Create a scalar Variable named scalar of type double. Note that the empty ArrayList means that it is
                // a scalar, ie has no Dimensions.
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
            var = null;
            return false;
        }
        return true;
    }

    /**
     * @param val
     * @throws Exception 
     */
    static Array extractedArrayForAttribute(Object val) throws Exception {
        // Array.factory(val);
        try {
            Class<? extends Object> clazz = val.getClass();
            NeptusLog.pub().warn(String.format("Processing type class %s", clazz));
            if (clazz == boolean[].class) {
                boolean[] aArray = (boolean[]) val;
                return Array.factory(DataType.BOOLEAN, new int[] {aArray.length}, val);
            }
            else if (clazz == byte[].class) {
                byte[] aArray = (byte[]) val;
                return Array.factory(DataType.BYTE, new int[] {aArray.length}, val);
            }
            // UBYTE: // TODO Add support
            else if (clazz == char[].class) {
                char[] aArray = (char[]) val;
                return Array.factory(DataType.CHAR, new int[] {aArray.length}, val);
            }
            else if (clazz == double[].class) {
                double[] aArray = (double[]) val;
                return Array.factory(DataType.DOUBLE, new int[] {aArray.length}, val);
            }
            else if (clazz == float[].class) {
                float[] aArray = (float[]) val;
                return Array.factory(DataType.FLOAT, new int[] {aArray.length}, val);
            }
            else if (clazz == int[].class) {
                int[] aArray = (int[]) val;
                return Array.factory(DataType.INT, new int[] {aArray.length}, val);
            }
            // UINT: // TODO Add support
            else if (clazz == long[].class) {
                long[] aArray = (long[]) val;
                return Array.factory(DataType.LONG, new int[] {aArray.length}, val);
            }
            // ULONG: // TODO Add support
            else if (clazz == short[].class) {
                short[] aArray = (short[]) val;
                return Array.factory(DataType.SHORT, new int[] {aArray.length}, val);
            }
            // USHORT: // TODO Add support
            else if (clazz == String[].class) {
                String[] aArray = (String[]) val;
                return Array.factory(DataType.STRING, new int[] {aArray.length}, val);
            }
            else {
                throw new Exception(String.format("Not hable to process type! [%s]", clazz.toString()));
            }
        }
        catch (Exception e) {
            throw new Exception(String.format("Not valid attribute type! [%s]", e.getMessage()));
        }
    }
    
    public boolean writeData(NetcdfFileWriter writer) {
        try {
            if (dimensions != null && dimensions.size() >= 0)
                writer.write(var, dataArray);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
            return false;
        }
        return true;
    }
    
    public static void main(String[] args) {
        double[] arrayD = new double[] {};
        System.out.println(arrayD.getClass().getComponentType());
        
    }
}

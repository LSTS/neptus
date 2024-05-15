/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 15/5/2024
 */
package pt.lsts.neptus.plugins.alertintrusion.data;

import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

public class SystemSizeAndCourseData {
    private final String name;

    private double width = 0;
    private double length = 0;
    private double height = 0;
    private double draught = 0;
    private double widthOffsetFromCenter = 0;
    private double lengthOffsetFromCenter = 0;

    private double speedMps = 0;
    private double headingDegrees = 0;
    private double courseDegrees = 0;

    private long timestampMillis = 0;

    public SystemSizeAndCourseData(String name, long timestampMillis) {
        this.name = name;
        this.timestampMillis = timestampMillis;
    }

    public SystemSizeAndCourseData(String name) {
        this(name, 0);
    }

    public static SystemSizeAndCourseData from(ImcSystem sys) {
        SystemSizeAndCourseData ret = new SystemSizeAndCourseData(sys.getName());
        ret.updateWith(sys);
        return ret;
    }

    public static SystemSizeAndCourseData from(ExternalSystem sys) {
        SystemSizeAndCourseData ret = new SystemSizeAndCourseData(sys.getName());
        ret.updateWith(sys);
        return ret;
    }

    public String getName() {
        return name;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidthOffsetFromCenter() {
        return widthOffsetFromCenter;
    }

    public void setWidthOffsetFromCenter(double widthOffsetFromCenter) {
        this.widthOffsetFromCenter = widthOffsetFromCenter;
    }

    public double getLengthOffsetFromCenter() {
        return lengthOffsetFromCenter;
    }

    public void setLengthOffsetFromCenter(double lengthOffsetFromCenter) {
        this.lengthOffsetFromCenter = lengthOffsetFromCenter;
    }

    public double getMaxDiameter() {
        return Math.max(width, length);
    }

    public double getMaxDiameterFromPosition() {
        return Math.max(getMaxDiameterWidthFromPosition(), getMaxDiameterLengthFromPosition());
    }

    public double getMaxDiameterWidthFromPosition() {
        return getMaxDiameter() / 2 + Math.abs(widthOffsetFromCenter) / 2;
    }

    public double getMaxDiameterLengthFromPosition() {
        return getMaxDiameter() / 2 + Math.abs(lengthOffsetFromCenter) / 2;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getDraught() {
        return draught;
    }

    public void setDraught(double draught) {
        this.draught = draught;
    }

    public double getSpeedMps() {
        return speedMps;
    }

    public void setSpeedMps(double speedMps) {
        this.speedMps = speedMps;
    }

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    public void setHeadingDegrees(double headingDegrees) {
        this.headingDegrees = headingDegrees;
    }

    public double getCourseDegrees() {
        return courseDegrees;
    }

    public void setCourseDegrees(double courseDegrees) {
        this.courseDegrees = courseDegrees;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    public void setTimestampMillis() {
        setTimestampMillis(System.currentTimeMillis());
    }

    public boolean updateWith(ImcSystem sys) {
        if (sys == null || !name.equalsIgnoreCase(sys.getName()))
            return false;

        VehicleType veh = VehiclesHolder.getVehicleById(getName());

        headingDegrees = sys.getYawDegrees();
        Object obj = sys.retrieveData(SystemUtils.COURSE_DEGS_KEY);
        courseDegrees = obj != null ? ((Number) obj).doubleValue() : headingDegrees;

        obj = sys.retrieveData(SystemUtils.GROUND_SPEED_KEY);
        speedMps = obj != null ? ((Number) obj).doubleValue() : 0;

        widthOffsetFromCenter = 0;
        lengthOffsetFromCenter = 0;

        obj = sys.retrieveData(SystemUtils.WIDTH_KEY);
        if (obj != null) {
            width = ((Number) obj).doubleValue();
            obj = sys.retrieveData(SystemUtils.LENGHT_KEY);
            if (obj != null) {
                length = ((Number) obj).doubleValue();

                obj = sys.retrieveData(SystemUtils.WIDTH_CENTER_OFFSET_KEY);
                if (obj != null) {
                    widthOffsetFromCenter = ((Number) obj).doubleValue();
                }
                obj = sys.retrieveData(SystemUtils.LENGHT_CENTER_OFFSET_KEY);
                if (obj != null)
                    lengthOffsetFromCenter = ((Number) obj).doubleValue();
            }
        } else {
            width = 0;
            length = 0;
            if (veh != null) {
                width = veh.getXSize();
                length = veh.getYSize();
            }
        }

        height = 0;
        if (veh != null) {
            height = veh.getZSize();
        }

        obj = sys.retrieveData(SystemUtils.DRAUGHT_KEY);
        draught = obj != null ? ((Number) obj).doubleValue() : 0;

        setTimestampMillis(sys.getLocationTimeMillis());

        return true;
    }

    public boolean updateWith(ExternalSystem sys) {
        if (sys == null || !name.equalsIgnoreCase(sys.getName()))
            return false;

        VehicleType veh = VehiclesHolder.getVehicleById(getName());

        headingDegrees = sys.getYawDegrees();
        Object obj = sys.retrieveData(SystemUtils.COURSE_DEGS_KEY);
        courseDegrees = obj != null ? ((Number) obj).doubleValue() : headingDegrees;

        obj = sys.retrieveData(SystemUtils.GROUND_SPEED_KEY);
        speedMps = obj != null ? ((Number) obj).doubleValue() : 0;

        widthOffsetFromCenter = 0;
        lengthOffsetFromCenter = 0;

        obj = sys.retrieveData(SystemUtils.WIDTH_KEY);
        if (obj != null) {
            width = ((Number) obj).doubleValue();
            obj = sys.retrieveData(SystemUtils.LENGHT_KEY);
            if (obj != null) {
                length = ((Number) obj).doubleValue();

                obj = sys.retrieveData(SystemUtils.WIDTH_CENTER_OFFSET_KEY);
                if (obj != null) {
                    widthOffsetFromCenter = ((Number) obj).doubleValue();
                }
                obj = sys.retrieveData(SystemUtils.LENGHT_CENTER_OFFSET_KEY);
                if (obj != null)
                    lengthOffsetFromCenter = ((Number) obj).doubleValue();
            }
        } else {
            width = 0;
            length = 0;
            if (veh != null) {
                width = veh.getXSize();
                length = veh.getYSize();
            }
        }

        height = 0;
        if (veh != null) {
            height = veh.getZSize();
        }

        obj = sys.retrieveData(SystemUtils.DRAUGHT_KEY);
        draught = obj != null ? ((Number) obj).doubleValue() : 0;

        setTimestampMillis(sys.getLocationTimeMillis());

        return true;
    }
}

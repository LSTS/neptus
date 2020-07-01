/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel Ribeiro
 * 12/11/2018
 */
package pt.lsts.neptus.plugins.djiimporter;

import java.util.Date;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Manuel Ribeiro
 *
 */
public class ImageMetadata {
    private String name = null;
    private String path = null;
    private int imgHeight = -1;
    private int imgWidth = -1;
    private String make = null;
    private String model = null;
    private Date date = null;
    private LocationType location = null;
    private double AGL = -1;
    private double MSL = -1;
      
    public ImageMetadata() {
        
    }
    
    /**
     * @param name
     * @param path
     * @param imgHeight
     * @param imgWidth
     * @param make
     * @param model
     * @param date
     * @param location
     * @param aGL
     * @param mSL
     */
    public ImageMetadata(String name, String path, int imgHeight, int imgWidth, String make, String model, Date date,
            LocationType location, double aGL, double mSL) {
        super();
        this.name = name;
        this.path = path;
        this.imgHeight = imgHeight;
        this.imgWidth = imgWidth;
        this.make = make;
        this.model = model;
        this.date = date;
        this.location = location;
        AGL = aGL;
        MSL = mSL;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getImgHeight() {
        return imgHeight;
    }
    public void setImgHeight(int imgHeight) {
        this.imgHeight = imgHeight;
    }
    public int getImgWidth() {
        return imgWidth;
    }
    public void setImgWidth(int imgWidth) {
        this.imgWidth = imgWidth;
    }
    public String getMake() {
        return make;
    }
    public void setMake(String make) {
        this.make = make;
    }
    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public LocationType getLocation() {
        return location;
    }
    public void setLocation(LocationType location) {
        this.location = location;
    }
    public double getAGL() {
        return AGL;
    }
    public void setAGL(double aGL) {
        AGL = aGL;
    }
    public double getMSL() {
        return MSL;
    }
    public void setMSL(double mSL) {
        MSL = mSL;
    }

    @Override
    public String toString() {
        return "ImageMetadata [name=" + name + ", imgHeight=" + imgHeight + ", imgWidth=" + imgWidth + ", make=" + make
                + ", model=" + model + ", date=" + date + ", location=" + location + ", AGL=" + AGL + ", MSL=" + MSL
                + "]";
    }
    
    
}

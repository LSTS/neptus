/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Manuel R.
 * Feb 12, 2015
 */
package pt.lsts.neptus.mra.markermanagement;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Manuel
 *
 */
public class LogMarkerItem extends LogMarker {

    private static final long serialVersionUID = 1L;
    private int index;
    private String sidescanImgPath;
    private ArrayList<String> photoList = new ArrayList<>();
    private String drawImgPath;
    private String annotation;
    private double altitude;
    private double depth;
    private double range;
    private Classification classification;
    private HashSet<String> tagList = new HashSet<>();

    public enum Classification {
        UNDEFINED(0), 
        NONE(1), 
        CABLE(2), 
        PIPE(3), 
        ROCK(4), 
        WRECK(5),
        UNKNOWN(6);

        public int getValue() {
            return this.value;
        }

        private int value;

        private Classification(int value) {
            this.value = value;
        }

    }

    /**
     * @param label
     * @param timestamp
     * @param lat
     * @param lon
     */
    public LogMarkerItem(int index, String label, double timestamp, double lat, double lon, String sidescanImgPath, String drawImgPath, 
            String annot, double altitude, double depth, double range, Classification classif, ArrayList<String> photos, HashSet<String> tags) {
        super(label, annot, timestamp, lat, lon);
        this.index = index;
        this.sidescanImgPath = sidescanImgPath;
        this.drawImgPath = drawImgPath;
        this.annotation = annot;
        this.altitude = altitude;
        this.depth = depth;
        this.range = range;
        this.classification = classif;
        this.photoList = photos;
        this.tagList = tags;
    }

    /**
     * @return the classification
     */
    public Classification getClassification() {
        return classification;
    }

    /**
     * @param classification the classification to set
     */
    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    /**
     * @return the annotation
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * @param annotation the annotation to set
     */
    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    /**
     * @return the altitude
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * @param depth the depth to set
     */
    public void setAltitude(double depth) {
        this.altitude = depth;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    public LocationType getLocation() {
        return new LocationType(getLatRads(), getLonRads());
    }

    public String toString(){
        StringBuilder string = new StringBuilder();
        string.append(index + " ");
        string.append(getLabel() + " ");
        string.append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(getDate()) + " ");
        string.append(getLatRads() + " ");
        string.append(getLonRads() + " ");
        string.append(getAltitude()+ " ");
        string.append(getAnnotation()+ " ");
        string.append(getClassification()+ " ");

        return string.toString();
    }

    public void copy(LogMarkerItem from) {
        this.annotation = from.annotation;
        this.classification = from.classification;
        this.photoList = new ArrayList<>(from.photoList);
        this.tagList = new HashSet<>(from.tagList);
    }

    /**
     * @return the sidescanImgPath
     */
    public String getSidescanImgPath() {
        return sidescanImgPath;
    }

    /**
     * @param sidescanImgPath the sidescanImgPath to set
     */
    public void setSidescanImgPath(String sidescanImgPath) {
        this.sidescanImgPath = sidescanImgPath;
    }

    /**
     * @return the depth
     */
    public double getDepth() {
        return depth;
    }

    /**
     * @param depth the depth to set
     */
    public void setDepth(double depth) {
        this.depth = depth;
    }

    /**
     * @return the range
     */
    public double getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(double range) {
        this.range = range;
    }

    /**
     * @return the drawImgPath
     */
    public String getDrawImgPath() {
        return drawImgPath;
    }

    /**
     * @param drawImgPath the drawImgPath to set
     */
    public void setDrawImgPath(String drawImgPath) {
        this.drawImgPath = drawImgPath;
    }
    
    /**
     * @return the photosPath
     */
    public ArrayList<String> getPhotosPath() {
        return photoList;
    }

    /**
     * @param photosPath the photosPath to set
     */
    public void setPhotosPath(ArrayList<String> photosPath) {
        this.photoList = photosPath;
    }
    
    /**
     * @return the taglist
     */
    public HashSet<String> getTags() {
        return tagList;
    }

    /**
     * @param Set list of tags
     */
    public void setTags(HashSet<String> tags) {
        this.tagList = tags;
    }
    
    /**
     * @param Add Tag to list
     */
    public void addTag(String tag) {
        tagList.add(tag);
    }
    
    /**
     * @param remove tag from the list
     */
    public void removeTag(String tag) {
        Iterator<String> it = tagList.iterator();
        while (it.hasNext()) {
            String currElement = it.next();
            if (tag.equals(currElement)) {
                it.remove();
            }
        }
    }
}

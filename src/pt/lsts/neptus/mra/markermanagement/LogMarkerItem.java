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

import java.awt.image.BufferedImage;
import java.text.DateFormat;

import pt.lsts.neptus.mra.LogMarker;

/**
 * @author Manuel
 *
 */
public class LogMarkerItem extends LogMarker {

    private static final long serialVersionUID = 1L;
    private int index;
    private BufferedImage image;
    private float range;
    private String annotation;
    private int depth;
    private Classification classification;
    
    public enum Classification {
        UNDEFINED(-1), 
        UNKNOWN(1), 
        SHIP(2), 
        OTHER1(3), 
        OTHER2(4), 
        OTHER3(5);


        public int getValue() {
            return this.value;
        }

        private int value;

        private Classification(int value) {
            this.value = value;
        }

    }

    //TODO : add free draw attribute, independently from image (to be able to export image without draw or just draw, or both!)

    /**
     * @param label
     * @param timestamp
     * @param lat
     * @param lon
     */
    public LogMarkerItem(int index, String label, double timestamp, double lat, double lon, BufferedImage img, float range, String annot, int depth, Classification classif) {
        super(label, timestamp, lat, lon);
        this.image = img;
        this.range = range;
        this.annotation = annot;
        this.depth = depth;
        this.classification = classif;
    }

    /**
     * @return the image
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * @param image the image to set
     */
    public void setImage(BufferedImage image) {
        this.image = image;
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
     * @return the range
     */
    public float getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(float range) {
        this.range = range;
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
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param depth the depth to set
     */
    public void setDepth(int depth) {
        this.depth = depth;
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

    public String toString(){
        StringBuilder string = new StringBuilder();
        string.append(index + " ");
        string.append(getLabel() + " ");
        string.append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(getDate()) + " ");
        string.append(getLat() + " ");
        string.append(getLon() + " ");
        string.append(getRange()+ " ");
        string.append(getDepth()+ " ");
        string.append(getAnnotation()+ " ");
        string.append(getClassification()+ " ");

        return string.toString();
    }



}

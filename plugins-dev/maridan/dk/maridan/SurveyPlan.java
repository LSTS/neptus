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
 * Author: zp
 * Nov 25, 2015
 */
package dk.maridan;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author zp
 *
 */
@XmlRootElement(name="SurveyPlan")
public class SurveyPlan {

    @XmlElement(name="SafetyArea")
    private SafetyArea safetyArea = new SafetyArea();
    
    
    static class SafetyArea {
    }
    
    @XmlElements({@XmlElement(type=GotoMan.class), @XmlElement(type=SiteMan.class)})
    private List<Manoeuvre> Manoeuvre = new ArrayList<>();
    
    public void addManoeuvre(Manoeuvre man) {
        Manoeuvre.add(man);
    }
    
    static class Manoeuvre {
        @XmlElement(name="Payload")
        public Setting payload = new Setting();
        
        @XmlElement(name="SLBL")
        public Setting slbl = new Setting();
        
        @XmlElement(name="TP")
        public Setting tp = new Setting();
        
        @XmlElement(name="IntChan")
        public Setting intChan = new Setting();        
    }
    
    static class Setting {
        @XmlAttribute(name="Setting")
        private String setting = "0";
        
        public void set(String value) {
            this.setting = value;
        }
    }
    
    @XmlRootElement(name="Manoeuvre")
    static class GotoMan extends Manoeuvre {
        @XmlElement(name="Point")
        private Point point = new Point();
        
        public void setLatDegs(double lat) {
            point.latitude = lat;
        }
        
        public void setLonDegs(double lon) {
            point.longitude = lon;
        }
        
        public void setDepth(float depth) {
            point.height = -depth;
        }
        
        public void setAltitude(float altitude) {
            point.height = altitude;
        }
        
        public void setSpeedMps(float speed) {
            point.speed = speed;
        }
        
        public void setTimeoutSecs(float timeout) {
            point.timeout = timeout;
        }
        
    }
    
    @XmlRootElement(name="Manoeuvre")
    static class SiteMan extends Manoeuvre {
        @XmlElement(name="Site")
        private Site site = new Site(); 
        
        public void setDirectionDegs(float direction) {
            site.direction = direction;
        }
        
        public void setLegCount(int count) {
            site.legCount = count;
        }
        
        public void setSpacingMeters(float spacing) {
            site.spacing = spacing;
        }
        
        public void setLatDegs(double lat) {
            site.latitude = lat;
        }
        
        public void setLonDegs(double lon) {
            site.longitude = lon;
        }
        
        public void setDepth(float depth) {
            site.height = -depth;
        }
        
        public void setAltitude(float altitude) {
            site.height = altitude;
        }
        
        public void setSpeedMps(float speed) {
            site.speed = speed;
        }
        
        public void setTimeoutSecs(float timeout) {
            site.timeout = timeout;
        }
    }
    
    
    static class Point {
        @XmlAttribute(name="Lat")
        double latitude = 0;
        @XmlAttribute(name="Lon")
        double longitude = 0;
        @XmlAttribute(name="Height")
        float height = 0;
        @XmlAttribute(name="Speed")
        float speed = 1.3f;
        @XmlAttribute(name="Timeout")
        float timeout = 0;
    }
    
    static class Site extends Point{
        @XmlAttribute(name="LegCnt")
        int legCount = 8;
        @XmlAttribute(name="Spacing")
        float spacing = 20;
        @XmlAttribute(name="Direction")
        float direction = 45;
    }
    
    public static void main(String[] args) {
        SurveyPlan plan = new SurveyPlan();
        GotoMan g = new GotoMan();
        g.setLatDegs(41);
        g.setLonDegs(-8);
        g.setSpeedMps(1.4f);
        g.setTimeoutSecs(1000);
        g.payload.set("1");
        plan.addManoeuvre(g);
        plan.addManoeuvre(new SiteMan());
        JAXB.marshal(plan, System.out);
    }
}

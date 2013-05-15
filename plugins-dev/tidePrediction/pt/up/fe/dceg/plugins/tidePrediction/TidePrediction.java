/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: mfaria
 * ??/??/???
 */
package pt.up.fe.dceg.plugins.tidePrediction;
import java.util.Date;

/**
 * All the important info retrieved from the website and the way to retrieve it 
 *
 */
public class TidePrediction {
    /**
     * The two different tide types 
     */
    public enum TIDE_TYPE{
        LOW_TIDE("Baixa-mar"),
        HIGH_TIDE("Preia-mar");
        
        private String pt;
        private TIDE_TYPE(String inPT) { 
            pt = inPT; 
        }
        
        /**
         * Get the name of the tide type in portuguese that matches what is 
         * written in the website
         * @return the name in portuguese
         */
        public String getPt() {
            return pt;
        }
    }

    private float height;
    private Date timeAndDate;
    private TIDE_TYPE tideType;

    /**
     * @param height
     * @param timeAndDate
     * @param tideType
     */
    public TidePrediction(float height, Date timeAndDate, TIDE_TYPE tideType) {
        super();
        this.height = height;
        this.timeAndDate = timeAndDate;
        this.tideType = tideType;
    }


    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Date getTimeAndDate() {
        return timeAndDate;
    }

    public void setTimeAndDate(Date timeAndDate) {
        this.timeAndDate = timeAndDate;
    }

    public TIDE_TYPE getTideType() {
        return tideType;
    }

    public void setTideType(TIDE_TYPE tideType) {
        this.tideType = tideType;
    }

    @Override
    public String toString() {
        return "[height=" + height + ", timeAndDate=" + timeAndDate + ", tideType=" + tideType + "]";
    }

}

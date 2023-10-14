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
 * Jan 7, 2018
 */
package pt.lsts.neptus.renderer2d;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * This is used in the {@link StateRenderer2D} panel to be able report a change.
 * 
 * @author pdias
 *
 */
public interface IMapRendererChangeEvent {
    
    /**
     * This is the event to be sent to the bus indicating a change in the renderer map. 
     * 
     * @author pdias
     *
     */
    public static class RendererChangeEvent {
        private final StateRenderer2D source;
        private final LocationType centerLoc;
        private final double rotationRads;
        private final int levelOfDetail;
        
        public RendererChangeEvent(StateRenderer2D source, LocationType centerLoc, double rotationRads,
                int levelOfDetail) {
            this.source = source;
            this.centerLoc = centerLoc;
            this.rotationRads = rotationRads;
            this.levelOfDetail = levelOfDetail;
        }

        /**
         * @return the source
         */
        public StateRenderer2D getSource() {
            return source;
        }

        /**
         * @return the centerLoc
         */
        public LocationType getCenterLoc() {
            return centerLoc;
        }

        /**
         * @return the rotationRads
         */
        public double getRotationRads() {
            return rotationRads;
        }

        /**
         * @return the levelOfDetail
         */
        public int getLevelOfDetail() {
            return levelOfDetail;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getSimpleName());
            sb.append("::(source: ");
            sb.append(source.getClass().getSimpleName());
            sb.append("@0x");
            sb.append(Integer.toString(source.hashCode(), 16));
            sb.append(" | center: ");
            sb.append(centerLoc);
            sb.append(" | rot: ");
            sb.append(Math.toDegrees(rotationRads));
            sb.append("\u00B0 | lod: ");
            sb.append(levelOfDetail);
            return sb.toString();
        }
    }
    
    /**
     * @param event
     */
    public void mapRendererChangeEvent(RendererChangeEvent event);
}

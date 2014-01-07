/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 7, 2014
 */
package pt.lsts.neptus.plugins.fbt;

import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription
public class FbtExporter implements MRAExporter {

    private IMraLogGroup source;
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        this.source = source;
        return BathymetryParserFactory.build(source) != null || source.getLog("Distance") != null;
    }

    @Override
    public String process() {
        BathymetryParser bparser = BathymetryParserFactory.build(source);
        BathymetrySwath swath = bparser.nextSwath();
        return "Done";
    }
    
    // TODO
    public SurveyRecord translate(BathymetrySwath swath) {
        SurveyRecord rec = new SurveyRecord();
        LocationType loc = new LocationType(swath.getPose().getPosition());
        loc.convertToAbsoluteLatLonDepth();
        
        rec.time_d = swath.getTimestamp() / 1000.0;
        rec.longitude = loc.getLongitudeAsDoubleValue();
        rec.latitude = loc.getLatitudeAsDoubleValue();
        rec.altitude = swath.getPose().getAltitude();
        rec.sonardepth = swath.getPose().getPosition().getDepth();
        rec.speed = (float)swath.getPose().getU();
        rec.depth_scale = 0.1f;
        rec.roll = (float)Math.toDegrees(swath.getPose().getRoll());
        rec.pitch = (float)Math.toDegrees(swath.getPose().getPitch());
        rec.heading = (float)Math.toDegrees(swath.getPose().getYaw());
        rec.beams_bath = (short) swath.getNumBeams();
        
        //TODO not complete
        
        return rec;
    }
    

    @Override
    public String getName() {
        return "FBT Exporter";
    }

}

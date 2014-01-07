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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.lowagie.text.pdf.codec.Base64.OutputStream;

/**
 * @author zp
 *
 */
public class SurveyRecord {

    static final int RECORD_TYPE = 25443;
    
    // header
    public int     recordtype;     // 2, 00->02
    public double  time_d;         // 8, 02->10
    public double  longitude;      // 8, 10->18
    public double  latitude;       // 8, 18->26
    public double  sonardepth;     // 8, 26->34
    public double  altitude;       // 8, 34->42
    public float   heading;        // 4, 42->46
    public float   speed;          // 4, 46->50
    public float   roll;           // 4, 50->54
    public float   pitch;          // 4, 54->58
    public float   heave;          // 4, 58->62
    public float   beam_xwidth;    // 4, 62->66
    public float   beam_lwidth;    // 4, 66->70
    public short   beams_bath;     // 2, 70->72
    public short   beams_amp;      // 2, 72->74
    public short   pixels_ss;      // 2, 74->76
    public short   spare1;         // 2, 76->78
    public float   depth_scale;    // 4, 78->82
    public float   distance_scale; // 4, 82->86
    public short   ss_type;        // 2, 86->88
    public short   spare2;         // 2, 88->90
 
    // bathymetry data
    public byte[] beamflag;
    public short[] bath;
    public short[] bath_acrosstrack;
    public short[] bath_alongtrack;
    public short[] amp;
    
    // sidescan data
    public short[] ss;
    public short[] ss_acrosstrack;
    public short[] ss_alongtrack;
    
    public void read(boolean skipType, InputStream is) throws IOException {
        DataInputStream input = new DataInputStream(is);
        if (!skipType) {
            recordtype = input.readUnsignedShort();
            if (recordtype != RECORD_TYPE) {
                throw new IOException("Record type does not match exptected value: "+recordtype+" != "+RECORD_TYPE);
            }
        }
        
        //header
        time_d = input.readDouble();
        longitude = input.readDouble();
        latitude = input.readDouble();
        sonardepth = input.readDouble();
        altitude = input.readDouble();
        heading = input.readFloat();
        speed = input.readFloat();
        roll = input.readFloat();
        pitch = input.readFloat();
        heave = input.readFloat();
        beam_xwidth = input.readFloat();
        beam_lwidth = input.readFloat();
        beams_bath = input.readShort();
        beams_amp = input.readShort();
        pixels_ss = input.readShort();
        spare1 = input.readShort();
        depth_scale = input.readFloat();
        distance_scale = input.readFloat();
        ss_type = input.readShort();
        spare2 = input.readShort();
        
        // bathymetry data
        beamflag = new byte[beams_bath];
        int read = 0;
        while (read < beams_bath)
            read += input.read(beamflag, read, beams_bath - read);
        bath = new short[beams_bath];
        bath_acrosstrack = new short[beams_bath];
        bath_alongtrack = new short[beams_bath];
        amp = new short[beams_amp];
        
        for (int i = 0; i < beams_bath; i++)
            bath[i] = input.readShort();
        for (int i = 0; i < beams_bath; i++)
            bath_acrosstrack[i] = input.readShort();
        for (int i = 0; i < beams_bath; i++)
            bath_alongtrack[i] = input.readShort();
        for (int i = 0; i < beams_amp; i++)
            amp[i] = input.readShort();
        
        ss = new short[pixels_ss];
        ss_acrosstrack = new short[pixels_ss];
        ss_alongtrack = new short[pixels_ss];
        
        for (int i = 0; i < pixels_ss; i++)
            ss[i] = input.readShort();
        for (int i = 0; i < pixels_ss; i++)
            ss_acrosstrack[i] = input.readShort();
        for (int i = 0; i < pixels_ss; i++)
            ss_alongtrack[i] = input.readShort();        
    }
    
    public void write(OutputStream os) throws IOException {
        DataOutputStream out = new DataOutputStream(os);
        
        out.writeShort(RECORD_TYPE);
        out.writeDouble(time_d);
        out.writeDouble(longitude);
        out.writeDouble(latitude);
        out.writeDouble(sonardepth);
        out.writeDouble(altitude);
        out.writeFloat(heading);
        out.writeFloat(speed);
        out.writeFloat(roll);
        out.writeFloat(pitch);
        out.writeFloat(heave);
        out.writeFloat(beam_xwidth);
        out.writeFloat(beam_lwidth);
        out.writeShort(beams_bath);
        out.writeShort(beams_amp);
        out.writeShort(pixels_ss);
        out.writeShort(spare1);
        out.writeFloat(depth_scale);
        out.writeFloat(distance_scale);
        out.writeShort(ss_type);
        out.writeShort(spare2);
        
        out.write(beamflag);
        for (int i = 0; i < beams_bath; i++)
            out.writeShort(bath[i]);
        for (int i = 0; i < beams_bath; i++)
            out.writeShort(bath_acrosstrack[i]);
        for (int i = 0; i < beams_bath; i++)
            out.writeShort(bath_alongtrack[i]);
        for (int i = 0; i < beams_amp; i++)
            out.writeShort(amp[i]);
        
        for (int i = 0; i < pixels_ss; i++)
            out.writeShort(ss[i]);
        for (int i = 0; i < pixels_ss; i++)
            out.writeShort(ss_acrosstrack[i]);
        for (int i = 0; i < pixels_ss; i++)
            out.writeShort(ss_alongtrack[i]);        
    }
}

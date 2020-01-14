/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Dec 22, 2015
 */
package pt.lsts.neptus.console.plugins.airos;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Station {

    @SerializedName("mac")
    @Expose
    public String mac;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("lastip")
    @Expose
    public String lastip;
    @SerializedName("associd")
    @Expose
    public float associd;
    @SerializedName("aprepeater")
    @Expose
    public float aprepeater;
    @SerializedName("tx")
    @Expose
    public float tx;
    @SerializedName("rx")
    @Expose
    public float rx;
    @SerializedName("signal")
    @Expose
    public float signal;
    @SerializedName("ccq")
    @Expose
    public float ccq;
    @SerializedName("idle")
    @Expose
    public float idle;
    @SerializedName("uptime")
    @Expose
    public float uptime;
    @SerializedName("ack")
    @Expose
    public float ack;
    @SerializedName("distance")
    @Expose
    public float distance;
    @SerializedName("txpower")
    @Expose
    public float txpower;
    @SerializedName("noisefloor")
    @Expose
    public float noisefloor;
    @SerializedName("airmax")
    @Expose
    public Airmax airmax;
    @SerializedName("stats")
    @Expose
    public Stats stats;
    @SerializedName("rates")
    @Expose
    public List<String> rates = new ArrayList<String>();
    @SerializedName("signals")
    @Expose
    public List<Integer> signals = new ArrayList<Integer>();
}

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
 * Author: pdias
 * May 18, 2018
 */
package pt.lsts.neptus.util.netcdf.exporter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;

/**
 * @author pdias
 *
 */
public class NetCDFRootAttributes {
    // Highly Recommended
    // ==================
    public String title = null;
    public String summary = null;
    public String keywords = null; // comma separated values, see http://gcmd.gsfc.nasa.gov/learn/keywords.html
    public String conventions = "CF-1.7, ACDD-1.3";

    // Recommended
    // ===========
    public String id = null;
    public String namingAuthority = null;
    
    public String history = null;

    public String source = null;
    public String processingLevel = null;

    public String license = null;
    public String standardNameVocabulary = "CF Standard Name Table v37";

    // date_created
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
    private LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.of("UTC"));
    private ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
    public String dateCreated = zdt.format(timeFormatter);
    public String dateModified = dateCreated;
    public String dateIssued = dateCreated;
    public String dateMetadataModified = dateCreated;

    public String creatorName = "Neptus " + ConfigFetch.getNeptusVersion();
    public String creatorType = "group";
    
    private Map<String, Object> additionalAttrib = new LinkedHashMap<>(); 
    
    /**
     * @param title the title to set
     */
    public NetCDFRootAttributes setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * @param summary the summary to set
     * @return
     */
    public NetCDFRootAttributes setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    /**
     * @param keywords the keywords to set
     * @return
     */
    public NetCDFRootAttributes setKeywords(String... keywords) {
        if (keywords != null)
            this.keywords = Stream.of(keywords).collect(Collectors.joining(", "));
        else
            this.keywords = null;

        return this;
    }

    /**
     * It is already set to "CF-1.7, ACDD-1.3"
     * 
     * @param conventions the conventions to set
     * @return 
     */
    public NetCDFRootAttributes setConventions(String... conventions) {
        if (conventions != null)
            this.conventions = Stream.of(conventions).collect(Collectors.joining(", "));
        else
            this.conventions = null;

        return this;
    }
    
    /**
     * An identifier for the data set, provided by and unique within its naming authority
     * IDs can be URLs, URNs, DOIs, meaningful text strings, a local key, or any other unique string of characters. The
     * id should not include white space characters.
     * 
     * @param id the id to set
     * @return 
     */
    public NetCDFRootAttributes setId(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * @see #id
     * 
     * @param namingAuthority the namingAuthority to set
     * @return 
     */
    public NetCDFRootAttributes setNamingAuthority(String namingAuthority) {
        this.namingAuthority = namingAuthority;
        return this; 
    }
    
    /**
     * This is a character array with a line for each invocation of a program that has modified the dataset.
     * Well-behaved generic netCDF applications should append a line containing: date, time of day, user name, program
     * name and command arguments.'
     * 
     * @param history the history to set
     * @return 
     */
    public NetCDFRootAttributes setHistory(String history) {
        this.history = history;
        return this;
    }
    
    /**
     * @param source the source to set
     * @return 
     */
    public NetCDFRootAttributes setSource(String source) {
        this.source = source;
        return this;
    }
    
    /**
     * A textual description of the processing (or quality control) level of the data.
     * 
     * @param processingLevel the processingLevel to set
     * @return 
     */
    public NetCDFRootAttributes setProcessingLevel(String processingLevel) {
        this.processingLevel = processingLevel;
        return this;
    }
    
    /**
     * Provide the URL to a standard or specific license, enter "Freely Distributed" or "None", or describe any
     * restrictions to data access and distribution in free text.
     * 
     * @param license the license to set
     * @return 
     */
    public NetCDFRootAttributes setLicense(String license) {
        this.license = license;
        return this;
    }
    
    /**
     * Is set to "CF Standard Name Table v37" see 
     * http://cfconventions.org/Data/cf-standard-names/37/build/cf-standard-name-table.html
     * 
     * @param standardNameVocabulary the standardNameVocabulary to set
     * @return 
     */
    public NetCDFRootAttributes setStandardNameVocabulary(String standardNameVocabulary) {
        this.standardNameVocabulary = standardNameVocabulary;
        return this;
    }

    /**
     * Already set to current time.
     * 
     * @param dateCreated the dateCreated to set
     * @return 
     */
    public NetCDFRootAttributes setDateCreated(Date date) {
        ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
        zdt = ldt.atZone(ZoneId.of("UTC"));
        String dateFormated = zdt.format(timeFormatter);
        this.dateCreated = dateFormated;
        return this;
    }

    /**
     * Already set to current time.
     * The date on which the data was last modified. Note that this applies just to the data, not the metadata.
     * 
     * @param dateModified the dateModified to set
     * @return 
     */
    public NetCDFRootAttributes setDateModified(Date date) {
        ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
        zdt = ldt.atZone(ZoneId.of("UTC"));
        String dateFormated = zdt.format(timeFormatter);
        this.dateModified = dateFormated;
        return this;
    }

    /**
     * Already set to current time.
     * 
     * @param dateIssued the dateIssued to set
     * @return 
     */
    public NetCDFRootAttributes setDateIssued(Date date) {
        ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
        zdt = ldt.atZone(ZoneId.of("UTC"));
        String dateFormated = zdt.format(timeFormatter);
        this.dateIssued = dateFormated;
        return this;
    }

    /**
     * Already set to current time.
     * 
     * @param dateMetadataModified the dateMetadataModified to set
     * @return 
     */
    public NetCDFRootAttributes setDateMetadataModified(String dateMetadataModified) {
        this.dateMetadataModified = dateMetadataModified;
        return this;
    }
    
    /**
     * Already Set to {@link ConfigFetch#getNeptusVersion()}
     * 
     * @param creatorName the creatorName to set
     * @return 
     */
    public NetCDFRootAttributes setCreatorName(String creatorName) {
        this.creatorName = creatorName;
        return this;
    }

    /**
     * @param creatorType the creatorType to set
     * @return 
     */
    public NetCDFRootAttributes setCreatorType(String creatorType) {
        this.creatorType = creatorType;
        return this;
    }

    public NetCDFRootAttributes setAtribute(String name, Object val) {
        if (name != null && !name.isEmpty())
            additionalAttrib.put(name, val);
        return this;
    }

    public NetCDFRootAttributes setAtribute(String name, Object[] val) {
        if (name != null && !name.isEmpty())
            additionalAttrib.put(name, val);
        return this;
    }

    public NetCDFRootAttributes removeAtribute(String name) {
        if (name != null && !name.isEmpty())
            additionalAttrib.remove(name);
        return this;
    }

    public boolean write(NetcdfFileWriter writer) {
        try {
            if (title != null)
                writer.addGroupAttribute(null, new Attribute("title", title));
            if (summary != null)
                writer.addGroupAttribute(null, new Attribute("summary", summary));
            if (keywords != null)
                writer.addGroupAttribute(null, new Attribute("keywords", keywords));
            if (conventions != null)
                writer.addGroupAttribute(null, new Attribute("conventions", conventions));
            
            if (id != null)
                writer.addGroupAttribute(null, new Attribute("id", id));
            if (namingAuthority != null)
                writer.addGroupAttribute(null, new Attribute("naming_authority", namingAuthority));
            
            if (history != null)
                writer.addGroupAttribute(null, new Attribute("history", history));
            
            if (source != null)
                writer.addGroupAttribute(null, new Attribute("source", source));
            if (processingLevel != null)
                writer.addGroupAttribute(null, new Attribute("processingLevel", processingLevel));
            
            if (license != null)
                writer.addGroupAttribute(null, new Attribute("license", license));
            if (standardNameVocabulary != null)
                writer.addGroupAttribute(null, new Attribute("standard_name_vocabulary", standardNameVocabulary));
            
            if (dateCreated != null)
                writer.addGroupAttribute(null, new Attribute("date_created", dateCreated));
            if (dateModified != null)
                writer.addGroupAttribute(null, new Attribute("date_modified", dateModified));
            if (dateIssued != null)
                writer.addGroupAttribute(null, new Attribute("date_issued", dateIssued));
            if (dateMetadataModified != null)
                writer.addGroupAttribute(null, new Attribute("date_metadata_modified", dateMetadataModified));
            
            
            if (creatorName != null)
                writer.addGroupAttribute(null, new Attribute("creator_name", creatorName));
            if (creatorType != null)
                writer.addGroupAttribute(null, new Attribute("creator_type", creatorType));
            
            additionalAttrib.keySet().stream().forEach(name -> {
                Object val = additionalAttrib.get(name);
                if (val == null)
                    return;

                try {
                    if (val.getClass().isArray())
                        writer.addGroupAttribute(null, new Attribute(name, NetCDFVarElement.extractedArrayForAttribute(val)));
                    else if (val.getClass().isAssignableFrom(String.class))
                        writer.addGroupAttribute(null, new Attribute(name, (String) val));
                    else if (val.getClass().isAssignableFrom(Attribute.class))
                        writer.addGroupAttribute(null, new Attribute(name, (Attribute) val));
                    else if (val.getClass().isAssignableFrom(Array.class))
                        writer.addGroupAttribute(null, new Attribute(name, (Array) val));
                    else if (val.getClass().isAssignableFrom(List.class))
                        writer.addGroupAttribute(null, new Attribute(name, (List<?>) val));
                    else {
                        try {
                            Number number = (Number) val;
                            writer.addGroupAttribute(null, new Attribute(name, number));   
                        }
                        catch (Exception e) {
                            throw new Exception("Not valid attribute type!");
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(String.format("Error while writting attribute '$s'!", name), e);
                }
            });

        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
            return false;
        }
        
        return true;
    }
    
    public static NetCDFRootAttributes createDefault(String name, String filePath) {
        return new NetCDFRootAttributes().setTitle(name).setId(new File(filePath).getName());
    }
}

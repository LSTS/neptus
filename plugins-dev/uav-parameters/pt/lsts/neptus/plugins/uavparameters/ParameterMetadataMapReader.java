/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: dronekit.io, Manuel R.
 * Nov 22, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Parameter metadata parser extracted from parameters
 * 
 */
public class ParameterMetadataMapReader {

    private static final String METADATA_DISPLAYNAME = "DisplayName";
    private static final String METADATA_DESCRIPTION = "Description";
    private static final String METADATA_UNITS = "Units";
    private static final String METADATA_VALUES = "Values";
    private static final String METADATA_RANGE = "Range";

    public static HashMap<String, ParameterMetadata> open(String input, String metadataType) throws IOException {
        XmlPullParserFactory factory = null;
        XmlPullParser parser = null;
        Reader r = null;

        try {
            r = new FileReader(input);
            factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(r);

            return parseMetadata(parser, metadataType);

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                r.close();
            } catch (IOException e) { /* nop */
            }
        }
        return null;
    }

    private static HashMap<String, ParameterMetadata> parseMetadata(XmlPullParser parser, String metadataType) {
        String name;
        boolean parsing = false;
        ParameterMetadata metadata = null;
        HashMap<String, ParameterMetadata> metadataMap = new HashMap<String, ParameterMetadata>();

        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        // name == metadataType: start collecting metadata(s)
                        // metadata == null: create new metadata w/ name
                        // metadata != null: add to metadata as property
                        if (metadataType.equals(name)) {
                            parsing = true;
                        } else if (parsing) {
                            if (metadata == null) {
                                metadata = new ParameterMetadata();
                                metadata.setName(name);
                            } else {
                                addMetaDataProperty(metadata, name, parser.nextText());
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        // name == metadataType: done
                        // name == metadata.name: add metadata to metadataMap
                        if (metadataType.equals(name)) {
                            return metadataMap;
                        } else if (metadata != null && metadata.getName().equals(name)) {
                            metadataMap.put(metadata.getName(), metadata);
                            metadata = null;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        // no metadata
        return null;
    }

    private static void addMetaDataProperty(ParameterMetadata metaData, String name, String text) {
        if (name.equals(METADATA_DISPLAYNAME))
            metaData.setDisplayName(text);
        else if (name.equals(METADATA_DESCRIPTION))
            metaData.setDescription(text);
        else if (name.equals(METADATA_UNITS))
            metaData.setUnits(text);
        else if (name.equals(METADATA_RANGE))
            metaData.setRange(text);
        else if (name.equals(METADATA_VALUES))
            metaData.setValues(text);
    }
}

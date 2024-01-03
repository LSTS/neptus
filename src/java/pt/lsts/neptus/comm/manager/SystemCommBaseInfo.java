/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 2007/05/19
 */
package pt.lsts.neptus.comm.manager;

import java.util.LinkedHashMap;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.messages.IMessage;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author Paulo Dias
 * 
 * @param <M> extends Message
 * @param <I> This represents the id for this comm.
 */
public abstract class SystemCommBaseInfo<M extends IMessage, Mi extends MessageInfo, I> extends
        CommonCommBaseImplementation<M, Mi> implements MessageListener<Mi, M>, PropertiesProvider {

    /**
     * As defined as the id of the {@link VehicleType}.
     */
    protected String systemIdName = null;

    /**
     * The id used for the comm. for the vehicle.
     */
    protected I systemCommId = null;

    /**
     * This HashTable will hold this class properties (for the related vehicle)
     */
    protected LinkedHashMap<String, DefaultProperty> properties = new LinkedHashMap<String, DefaultProperty>();

    /**
     * <p style="color='DARKVIOLET'">
     * This constructor SHOULD always be called.
     * </p>
     */
    public SystemCommBaseInfo() {
        preferencesListener.preferencesUpdated();
        GeneralPreferences.addPreferencesListener(preferencesListener);

        messageProcessor = new MessageProcessor(this.getClass().getSimpleName() + " [" + this.hashCode() + "] :: "
                + MessageProcessor.class.getSimpleName());
        messageProcessor.start();
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     */
    public void stopMsgProcessing() {
        GeneralPreferences.removePreferencesListener(preferencesListener);
        // FIXME
        // if(privateNode != null)
        // privateNode.stop();
        stopSystemComms();

        if (messageProcessor != null) {
            messageProcessor.stopProcessing();
            messageProcessor = null;
        }

        msgQueue.clear();
        infoQueue.clear();

        setActive(false, null, null);
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     */
    public void resumeMsgProcessing() {
        GeneralPreferences.addPreferencesListener(preferencesListener);

        if (messageProcessor != null)
            messageProcessor.stopProcessing();

        messageProcessor = new MessageProcessor(this.getClass().getSimpleName() + " [" + this.hashCode() + "] :: "
                + MessageProcessor.class.getSimpleName());
        messageProcessor.start();

        stopSystemComms();
        initSystemComms();
        startSystemComms();
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return Returns the {@link #systemCommId}.
     */
    public I getSystemCommId() {
        return systemCommId;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param commId The {@link #systemCommId} to set.
     */
    public void setSystemCommId(I commId) {
        this.systemCommId = commId;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return Returns the vehicleId as defined in {@link VehicleType}.
     */
    public String getSystemIdName() {
        return systemIdName;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param vehicleId The vehicleId to set as defined in {@link VehicleType}.
     */
    public void setSystemIdName(String vehicleId) {
        this.systemIdName = vehicleId;
    }

    abstract protected boolean initSystemComms();

    abstract protected boolean startSystemComms();

    abstract protected boolean stopSystemComms();

    // .. .................................... ..//
    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * The Vehicle ID, STANAG ID and LOI are regenerated.<br>
     * All other properties are taken from the <b>properties</b> HashTable.
     */
    public DefaultProperty[] getProperties() {

        return properties.values().toArray(new DefaultProperty[0]);
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @see PropertiesProvider#setProperties(Property[])
     */
    public void setProperties(Property[] props) {
        for (Property p : props) {
            properties.put(p.getName(), PropertiesEditor.getPropertyInstance(p));
        }
    }

    /**
     * Add or change a property for this vehicle comm info
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param prop The property to be added / changed
     */
    public void setProperty(Property prop) {
        properties.put(prop.getName(), PropertiesEditor.getPropertyInstance(prop));
    }

    /**
     * Return the value of the given property name
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param propertyName The name of the property
     * @return The value of the given property or <b>null</b> if the property is not set.
     */
    public Object getPropertyValue(String propertyName) {
        DefaultProperty dp = properties.get(propertyName);
        if (dp != null)
            return dp.getValue();
        else {
            NeptusLog.pub().error(this + ": this property " + propertyName + " has not been set. Returning NULL");
            return null;
        }
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @see PropertiesProvider#getPropertiesDialogTitle()
     */
    public String getPropertiesDialogTitle() {
        return "Communications settings for " + getSystemIdName() + " vehicle";
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @see PropertiesProvider#getPropertiesErrors(Property[])
     */
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
}

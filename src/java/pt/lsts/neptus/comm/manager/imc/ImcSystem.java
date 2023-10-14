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
 * 2009/03/14
 */
package pt.lsts.neptus.comm.manager.imc;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.CommUtil;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBControl;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.comm.protocol.IMCArgs;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author pdias
 */
/**
 * @author pdias
 *
 */
public class ImcSystem implements Comparable<ImcSystem> {

    private static final int TIMEOUT_FOR_NOT_ANNOUNCE_STATE = 12000;
    
    protected String name = ImcId16.NULL_ID.toString();
	protected ImcId16 id = ImcId16.NULL_ID;
	protected SystemTypeEnum type = SystemTypeEnum.UNKNOWN;
	protected VehicleTypeEnum typeVehicle = VehicleTypeEnum.UNKNOWN;
	protected CommMean commsInfo = null;
	
	protected boolean active = false;
	protected PlanType activePlan = null;
	protected final CoordinateSystem location = new CoordinateSystem();
	protected long locationTimeMillis = -1;
    protected long attitudeTimeMillis = -1;
	
	protected String emergencyPlanId = "";
	protected String emergencyStatusStr = "";
	
	private String servicesProvided = "";
	
	protected boolean onErrorState = false;
	protected String onErrorStateStr = "";
	protected long lastErrorStateReceived = -1;

	protected boolean onIdErrorState = false;
	protected long lastIdErrorStateReceived = -1;
	protected String lastUid = null;

	protected boolean onAnnounceState = false;
	protected long lastAnnounceStateReceived = -1;
	
	protected final PlanDBControl planDBControl = new PlanDBControl();

	private final Map<String, Object> dataStorage = (Map<String, Object>) Collections.synchronizedMap(new HashMap<String, Object>());
    private final Map<String, Long> dataStorageTime = (Map<String, Long>) Collections.synchronizedMap(new HashMap<String, Long>());

    // Authority WIP
    // FIXME add description to each one of the states
    public enum IMCAuthorityState { OFF, NONE, /* PAYLOAD_MONITOR, PAYLOAD, SYSTEM_MONITOR,*/ SYSTEM_FULL };
    protected IMCAuthorityState authorityState = IMCAuthorityState.NONE;
    
	/**
	 * @param vehicle
	 */
	public ImcSystem(VehicleType vehicle) {
		setType(SystemTypeEnum.VEHICLE);
		setName(vehicle.getId());
		ImcId16 tmpId = vehicle.getImcId();
		setId((tmpId == null)?ImcId16.NULL_ID:tmpId);
		CommMean commMean = CommUtil.getActiveCommMeanForProtocol(
                vehicle, CommMean.IMC);
        setCommsInfo((commMean == null) ? createEmptyCommMean() : commMean);
	}

    /**
	 * @param id
	 */
	public ImcSystem(ImcId16 id) {
		this((id == null)?ImcId16.NULL_ID.toString():id.toString(), id);
        setCommsInfo(createEmptyCommMean());
	}

	/**
	 * @param name
	 * @param id
	 */
	public ImcSystem(String name, ImcId16 id) {
		setType(SystemTypeEnum.UNKNOWN);
		setName(name);
		setId((id == null)?ImcId16.NULL_ID:id);
        setCommsInfo(createEmptyCommMean());
	}

	/**
	 * @param id
	 * @param commMean
	 */
	public ImcSystem(ImcId16 id, CommMean commMean) {
		this(id);
		if (commMean != null)
		    setCommsInfo(commMean);
	}

	/**
	 * @param name
	 * @param id
	 * @param commMean
	 */
	public ImcSystem(String name, ImcId16 id, CommMean commMean) {
		this(name, id);
        if (commMean != null)
            setCommsInfo(commMean);
	}

    private CommMean createEmptyCommMean() {
        return ImcSystem.createCommMean("localhost", 0, 0, getId(), false, false);
    }

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
        planDBControl.setRemoteSystemId(this.name);
	}

	/**
	 * @return the id
	 */
	public ImcId16 getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(ImcId16 id) {
		this.id = id;
	}

	/**
	 * @return the location
	 */
	public LocationType getLocation() {
		return location.getNewAbsoluteLatLonDepth();
	}
	
	/**
	 * @param location the location to set
	 */
	public void setLocation(LocationType location) {
		this.location.setLocation(location);
		this.location.convertToAbsoluteLatLonDepth();
		setLocationTimeMillis(System.currentTimeMillis());
	}

	/**
	 * Only is override if locationTimeMillis is newer than already there.
	 * 
	 * @param location
	 * @param locationTimeMillis
	 */
	public boolean setLocation(LocationType location, long locationTimeMillis) {
	    if (locationTimeMillis < getLocationTimeMillis())
	        return false;
        this.location.setLocation(location);
        this.location.convertToAbsoluteLatLonDepth();
	    setLocationTimeMillis(locationTimeMillis);
	    return true;
	}

    public void setAttitudeDegrees(double rollDegrees, double pitchDegrees, double yawDegrees) {
        location.setRoll(rollDegrees);
        location.setPitch(pitchDegrees);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(System.currentTimeMillis());
    }

    /**
     * Only is override if attitudeTimeMillis is newer than already there.
     * 
     * @param rollDegrees
     * @param pitchDegrees
     * @param yawDegrees
     * @param locationTimeMillis
     * @return 
     */
    public boolean setAttitudeDegrees(double rollDegrees, double pitchDegrees, double yawDegrees, long attitudeTimeMillis) {
        if (attitudeTimeMillis < getAttitudeTimeMillis())
            return false;
        location.setRoll(rollDegrees);
        location.setPitch(pitchDegrees);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(attitudeTimeMillis);
        return true;
    }

    public void setAttitudeDegrees(double yawDegrees) {
        location.setRoll(0);
        location.setPitch(0);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(System.currentTimeMillis());
    }

    /**
     * Only is override if attitudeTimeMillis is newer than already there.
     * 
     * @param yawDegrees
     * @param locationTimeMillis
     * @return 
     */
    public boolean setAttitudeDegrees(double yawDegrees, long attitudeTimeMillis) {
        if (attitudeTimeMillis < getAttitudeTimeMillis())
            return false;
        location.setRoll(0);
        location.setPitch(0);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(attitudeTimeMillis);
        return true;
    }

    public double getRollDegrees() {
        return location.getRoll();
    }

    public double getPitchDegrees() {
        return location.getPitch();
    }

    public double getYawDegrees() {
        return location.getYaw();
    }

	/**
     * @return the locationTime
     */
    public long getLocationTimeMillis() {
        return locationTimeMillis;
    }
    
    /**
     * @param locationTimeMillis the locationTimeMillis to set
     */
    public void setLocationTimeMillis(long locationTimeMillis) {
        this.locationTimeMillis = locationTimeMillis;
    }
	
    /**
     * @return the attitudeTimeMillis
     */
    public long getAttitudeTimeMillis() {
        return attitudeTimeMillis;
    }
    
    /**
     * @param attitudeTimeMillis the attitudeTimeMillis to set
     */
    public void setAttitudeTimeMillis(long attitudeTimeMillis) {
        this.attitudeTimeMillis = attitudeTimeMillis;
    }
    
	/**
	 * @return the type
	 */
	public SystemTypeEnum getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(SystemTypeEnum type) {
	    if (this.type == SystemTypeEnum.UNKNOWN && type != SystemTypeEnum.UNKNOWN) {
	        if (type != SystemTypeEnum.CCU)
                setAuthorityState(IMCAuthorityState.SYSTEM_FULL);
	    }
		this.type = type;
	}

	/**
	 * @return the typeVehicle
	 */
	public VehicleTypeEnum getTypeVehicle() {
		return typeVehicle;
	}
	
	/**
	 * @param typeVehicle the typeVehicle to set
	 */
	public void setTypeVehicle(VehicleTypeEnum typeVehicle) {
		this.typeVehicle = typeVehicle;
	}
	
	/**
	 * Tries to get a corresponding vehicle in case the type of this system is VEHICLE
	 * @return the corresponding VehicleType or null if no vehicle has the same IMC ID
	 */
	public VehicleType getVehicle() {
		if (type == SystemTypeEnum.VEHICLE) {
			return VehiclesHolder.getVehicleWithImc(getId());
		}
		return null;
	}
	
	/**
     * @return the planDBControl
     */
    public PlanDBControl getPlanDBControl() {
        return planDBControl;
    }

    /**
     * 
     */
    public boolean isWithAuthority() {
        if (authorityState != ImcSystem.IMCAuthorityState.NONE)
            return true;
        else
            return false;
    }
    
	/**
     * @return the authorityState
     */
    public IMCAuthorityState getAuthorityState() {
        synchronized (authorityState) {
            return authorityState;
        }
    }

    /**
     * @param authorityState the authorityState to set
     */
    public void setAuthorityState(IMCAuthorityState authorityState) {
        synchronized (authorityState) {
            this.authorityState = authorityState;
        }
    }

    /**
	 * @return the commsInfo
	 */
	public CommMean getCommsInfo() {
		return commsInfo;
	}

	/**
	 * @param commsInfo the commsInfo to set
	 */
	public void setCommsInfo(CommMean commsInfo) {
		this.commsInfo = commsInfo;
	}

	/**
	 * @return
	 */
	public String getHostAddress() {
		if (getCommsInfo() == null)
			return null;
		else 
			return getCommsInfo().getHostAddress();
	}
	
	/**
	 * Remember that {@link #createCommMean(String, int, ImcId16, boolean, boolean)}
	 * should be called first to create it first or call {@link #setCommsInfo(CommMean)}
	 * if it is already created from outside.
	 * @param hostAddress
	 * @return
	 */
	public boolean setHostAddress(String hostAddress) {
		if (getCommsInfo() == null)
			return false;
		try {
			getCommsInfo().setHostAddress(hostAddress);
			return true;
		} catch (Exception e) {
			NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
					+ getId() + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * @return
	 */
	public boolean isUDPOn() {
	    if (getCommsInfo() == null)
	        return false;
	    try {
	        return ((IMCArgs) (getCommsInfo().getProtocolsArgs()
	                .get(CommMean.IMC))).isUdpOn();
	    } catch (Exception e) {
	        NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
	                + getId() + ": " + e.getMessage());
	        return false;
	    }
	}

	/**
	 * Remember that {@link #createCommMean(String, int, ImcId16, boolean, boolean)}
	 * should be called first to create it first or call {@link #setCommsInfo(CommMean)}
	 * if it is already created from outside.
	 * @return
	 */
	public boolean setUDPOn(boolean on) {
	    if (getCommsInfo() == null)
	        return false;
	    try {
	        ((IMCArgs) (getCommsInfo().getProtocolsArgs()
	                .get(CommMean.IMC))).setUdpOn(on);
	        return true;
	    } catch (Exception e) {
	        NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
	                + getId() + ": " + e.getMessage());
	        return false;
	    }
	}

	/**
     * @return
     */
    public boolean isTCPOn() {
        if (getCommsInfo() == null)
            return false;
        try {
            return ((IMCArgs) (getCommsInfo().getProtocolsArgs()
                    .get(CommMean.IMC))).isTcpOn();
        } catch (Exception e) {
            NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
                    + getId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Remember that {@link #createCommMean(String, int, ImcId16, boolean, boolean)}
     * should be called first to create it first or call {@link #setCommsInfo(CommMean)}
     * if it is already created from outside.
     * @return
     */
    public boolean setTCPOn(boolean on) {
        if (getCommsInfo() == null)
            return false;
        try {
            ((IMCArgs) (getCommsInfo().getProtocolsArgs()
                    .get(CommMean.IMC))).setTcpOn(on);
            return true;
        } catch (Exception e) {
            NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
                    + getId() + ": " + e.getMessage());
            return false;
        }
    }

	/**
	 * @return the UDP remote port or if some error occur return '0'
	 */
	public int getRemoteUDPPort() {
		if (getCommsInfo() == null)
			return 0;
		try {
			int remoteport = ((IMCArgs) (getCommsInfo().getProtocolsArgs()
			        .get(CommMean.IMC))).getPort();
			return remoteport;
		} catch (Exception e) {
			NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
					+ getId() + ": " + e.getMessage());
			return 0;
		}
	}

	/**
	 * Remember that {@link #createCommMean(String, int, ImcId16, boolean, boolean)}
	 * should be called first to create it first or call {@link #setCommsInfo(CommMean)}
	 * if it is already created from outside.
	 * @param port
	 * @return
	 */
	public boolean setRemoteUDPPort(int port) {
		if (getCommsInfo() == null)
			return false;
		try {
			((IMCArgs) (getCommsInfo().getProtocolsArgs()
			        .get(CommMean.IMC))).setPort(port);
			return true;
		} catch (Exception e) {
			NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
					+ getId() + ": " + e.getMessage());
			return false;
		}
	}

	/**
     * @return the TCP remote port or if some error occur return '0'
     */
    public int getRemoteTCPPort() {
        if (getCommsInfo() == null)
            return 0;
        try {
            int remoteport = ((IMCArgs) (getCommsInfo().getProtocolsArgs()
                    .get(CommMean.IMC))).getPortTCP();
            return remoteport;
        } catch (Exception e) {
            NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
                    + getId() + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Remember that {@link #createCommMean(String, int, ImcId16, boolean, boolean)}
     * should be called first to create it first or call {@link #setCommsInfo(CommMean)}
     * if it is already created from outside.
     * @param port
     * @return
     */
    public boolean setRemoteTCPPort(int port) {
        if (getCommsInfo() == null)
            return false;
        try {
            ((IMCArgs) (getCommsInfo().getProtocolsArgs()
                    .get(CommMean.IMC))).setPortTCP(port);
            return true;
        } catch (Exception e) {
            NeptusLog.pub().error(this.getClass().getSimpleName() + ":"
                    + getId() + ": " + e.getMessage());
            return false;
        }
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}
	
	/**
	 * @param hostAddress
	 * @param port
	 * @param imcId
	 * @param rtpsOn
	 * @param udpOn
	 * @return
	 */
	public static CommMean createCommMean(String hostAddress, int port, int portTCP,
			ImcId16 imcId, boolean udpOn, boolean tcpOn) {
		CommMean cm = new CommMean();
		cm.setName("ethernet");
		cm.setType("ethernet");
		cm.setActive(true);
		cm.setHostAddress(hostAddress);
		
		cm.getProtocols().add(CommMean.IMC);
		IMCArgs nArgs = new IMCArgs();
        nArgs.setPort(port);
        nArgs.setPortTCP(portTCP);
		nArgs.setImcId(imcId);
		nArgs.setUdpOn(udpOn);
		nArgs.setTcpOn(tcpOn);
		
		cm.getProtocolsArgs().put(CommMean.IMC, nArgs);
		return cm;
	}

	public static InetSocketAddress parseInetSocketAddress(String inetSocketAddress) {
		String[] lstr = inetSocketAddress.split(":");
		if (lstr.length != 2)
			return null;
		
		try {
			int port = Integer.parseInt(lstr[1]);
			return new InetSocketAddress(lstr[0], port);
		} catch (NumberFormatException e) {
		    NeptusLog.pub().error(e.getMessage());
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ImcSystem o) {
	    if (getId() == ImcId16.NULL_ID)
	        return getName().compareTo(o.getName());
		return getId().compareTo(o.getId());
	}
	
	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * @return the activePlan
	 */
	public PlanType getActivePlan() {
		return activePlan;
	}
	
	/**
	 * @param activePlan the activePlan to set
	 */
	public void setActivePlan(PlanType activePlan) {
		this.activePlan = activePlan;
	}
	
	/**
	 * @return the emergencyPlanId
	 */
	public String getEmergencyPlanId() {
		return emergencyPlanId;
	}
	
	/**
	 * @param emergencyPlanId the emergencyPlanId to set
	 */
	public void setEmergencyPlanId(String emergencyPlanId) {
		this.emergencyPlanId = emergencyPlanId;
	}
	
	/**
	 * @return the emergencyStatusStr
	 */
	public String getEmergencyStatusStr() {
		return emergencyStatusStr;
	}
	
	/**
	 * @param emergencyStatusStr the emergencyStatusStr to set
	 */
	public void setEmergencyStatusStr(String emergencyStatusStr) {
		this.emergencyStatusStr = emergencyStatusStr;
	}
	
	/**
	/*
	 * @return the servicesProvided
	 */
	public String getServicesProvided() {
		return servicesProvided;
	}
	
	/**
	 * @param servicesProvided the servicesProvided to set
	 */
	public void setServicesProvided(String servicesProvided) {
		this.servicesProvided = servicesProvided;
	}
	
	public boolean isServiceProvided(String name) {
		//return StringUtils.isTokenInList(servicesProvided, name, "[ ,:]+");
		return IMCUtils.isServiceProvided(servicesProvided, "*", name);
	}
	

	public boolean isServiceProvided(String scheme, String name) {
		return IMCUtils.isServiceProvided(servicesProvided, scheme, name);
	}

	public Vector<URI> getServiceProvided(String name) {
		return IMCUtils.getServiceProvided(servicesProvided, "*", name);
	}

	public Vector<URI> getServiceProvided(String scheme, String name) {
		return IMCUtils.getServiceProvided(servicesProvided, scheme, name);
	}

	/**
	 * @return the onErrorState
	 */
	public boolean isOnErrorState() {
		return onErrorState;
	}
	
	/**
	 * @param onErrorState the onErrorState to set
	 */
	public void setOnErrorState(boolean onErrorState) {
		this.onErrorState = onErrorState;
		setLastErrorStateReceived(System.currentTimeMillis());
	}
	
	/**
     * @return the onErrorStateStr
     */
    public String getOnErrorStateStr() {
        return onErrorStateStr;
    }
    
    /**
     * @param onErrorStateStr the onErrorStateStr to set
     */
    public void setOnErrorStateStr(String onErrorStateStr) {
        this.onErrorStateStr = onErrorStateStr;
    }
	
	/**
	 * @return the lastErrorStateReceived
	 */
	public long getLastErrorStateReceived() {
		return lastErrorStateReceived;
	}
	
	/**
	 * @param lastErrorStateReceived the lastErrorStateReceived to set
	 */
	private void setLastErrorStateReceived(long lastErrorStateReceived) {
		this.lastErrorStateReceived = lastErrorStateReceived;
	}
	
	/**
	 * @return the onIdErrorState
	 */
	public boolean isOnIdErrorState() {
		return onIdErrorState;
	}
	
	/**
	 * @param onIdErrorState the onIdErrorState to set
	 */
	public void setOnIdErrorState(boolean onIdErrorState) {
		this.onIdErrorState = onIdErrorState;
		setLastIdErrorStateReceived(System.currentTimeMillis());
	}
	
	/**
	 * @return the lastIdErrorStateReceived
	 */
	public long getLastIdErrorStateReceived() {
		return lastIdErrorStateReceived;
	}
	
	/**
	 * @param lastIdErrorStateReceived the lastIdErrorStateReceived to set
	 */
	private void setLastIdErrorStateReceived(long lastIdErrorStateReceived) {
		this.lastIdErrorStateReceived = lastIdErrorStateReceived;
	}
	
	/**
	 * @return the lastUid
	 */
	public String getLastUid() {
		return lastUid;
	}
	
	/**
	 * @param lastUid the lastUid to set
	 */
	public void setLastUid(String lastUid) {
		this.lastUid = lastUid;
	}

	/**
     * @return the onAnnounceState
     */
    public boolean isOnAnnounceState() {
        if (onAnnounceState && System.currentTimeMillis() - getLastAnnounceStateReceived() >= TIMEOUT_FOR_NOT_ANNOUNCE_STATE) {
            onAnnounceState = false;
        }
        return onAnnounceState;
    }
    
    /**
     * @param onAnnounceState the onAnnounceState to set
     */
    public void setOnAnnounceState(boolean onAnnounceState) {
        this.onAnnounceState = onAnnounceState;
        setLastAnnounceStateReceived(System.currentTimeMillis());
    }

    /**
     * @return the lastAnnounceStateReceived
     */
    public long getLastAnnounceStateReceived() {
        return lastAnnounceStateReceived;
    }
    
    /**
     * @param lastAnnounceStateReceived the lastAnnounceStateReceived to set
     */
    public void setLastAnnounceStateReceived(long lastAnnounceStateReceived) {
        this.lastAnnounceStateReceived = lastAnnounceStateReceived;
    }

    /**
     * @return the dataStorage keys
     */
    public Collection<String> getDataStorageKeys() {
        synchronized (dataStorage) {
            return Arrays.asList(dataStorage.keySet().toArray(new String[0]));
        }
    }
    
    /**
     * @param key
     * @return
     */
    public boolean containsData(String key) {
        synchronized (dataStorage) {
            return dataStorage.containsKey(key);
        }
    }

    public boolean containsData(String key, long ageMillis) {
        synchronized (dataStorage) {
            boolean ret = dataStorage.containsKey(key);
            if (ret && ageMillis > 0) {
                long time = dataStorageTime.get(key);
                if (System.currentTimeMillis() - time > ageMillis)
                    return false;
            }
            return ret;
        }
    }

	/**
	 * This will retrieve the data stored or {@code null} if not found.
	 * @param key
	 * @param ageMillis
	 * @return
	 */
	public Object retrieveData(String key, long ageMillis) {
	    synchronized (dataStorage) {
	        if (containsData(key, ageMillis))
	            return retrieveData(key);
        }
	    return null;
	}

	   /**
     * This will retrieve the data stored or {@code null} if not found.
     * @param key
     * @return
     */
    public Object retrieveData(String key) {
        Object ret = null;
        synchronized (dataStorage) {
            ret = dataStorage.get(key);
        }
        return ret;
    }

	/**
	 * @param key
	 * @return
	 */
	public long retrieveDataTimeMillis(String key) {
	    long ret = -1;
	    synchronized (dataStorage) {
            ret = dataStorage.containsKey(key) ? (dataStorageTime.containsKey(key) ? dataStorageTime.get(key) : -1)
                    : -1;
	    }
	    return ret;
	}

    /**
     * This will store some data with a {@link String} key.
     * The previous data if existed will be overwritten.
     * @param key
     * @param data
     */
    public boolean storeData(String key, Object data) {
        return storeData(key, data, System.currentTimeMillis(), true);
    }

    /**
     * @param key
     * @param data
     * @param timeMillis
     */
    public boolean storeData(String key, Object data, long timeMillis, boolean keepNewest) {
        synchronized (dataStorage) {
            if (keepNewest && dataStorage.containsKey(key) && dataStorageTime.containsKey(key)
                    && dataStorageTime.get(key) > timeMillis)
                return false;
            dataStorage.put(key, data);
            dataStorageTime.put(key, timeMillis);
            return true;
        }
    }

    public boolean removeData(String key) {
        synchronized (dataStorage) {
            if (dataStorage.containsKey(key)) {
                dataStorage.remove(key);
                dataStorageTime.remove(key);
                return true;
            }
            
            return false;
        }
    }

    /**
     * Checks if there is a "SimulatedState" message in the {@link #dataStorage} with age less than a minute.
     */
    public boolean isSimulated() {
        synchronized (dataStorage) {
            if (containsData("SimulatedState")) {
                if (System.currentTimeMillis() - retrieveDataTimeMillis("SimulatedState") < DateTimeUtil.MINUTE) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 
     */
    public void clearStoredData() {
        synchronized (dataStorage) {
            dataStorage.clear();
            dataStorageTime.clear();
        }
    }
    
    @Override
    public int hashCode() {
        if (getId() == ImcId16.NULL_ID)
            return getName().hashCode();
        return id.hashCode();
    }
    
    /**
     * @param type
     * @return
     */
    public static SystemTypeEnum translateSystemTypeFromMessage(String type) {
        if (type == null)
            return SystemTypeEnum.UNKNOWN;
        
        if ("CCU".equalsIgnoreCase(type.trim()))
            return SystemTypeEnum.CCU;
        else if ("HUMANSENSOR".equalsIgnoreCase(type))
            return SystemTypeEnum.MOBILESENSOR;
        else if ("UUV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("AUV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("ROV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("USV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("ASV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("UAV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("UGV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("AGV".equalsIgnoreCase(type))
            return SystemTypeEnum.VEHICLE;
        else if ("STATICSENSOR".equalsIgnoreCase(type))
            return SystemTypeEnum.STATICSENSOR;
        else if ("MOBILESENSOR".equalsIgnoreCase(type))
            return SystemTypeEnum.MOBILESENSOR;
        else if ("WSN".equalsIgnoreCase(type))
            return SystemTypeEnum.STATICSENSOR;
        else
            return SystemTypeEnum.UNKNOWN;
    }

    public static VehicleTypeEnum translateVehicleTypeFromMessage(String type) {
        if (type == null)
            return VehicleTypeEnum.UNKNOWN;

        if ("UUV".equalsIgnoreCase(type))
            return VehicleTypeEnum.UUV;
        else if ("AUV".equalsIgnoreCase(type))
            return VehicleTypeEnum.UUV;
        else if ("ROV".equalsIgnoreCase(type))
            return VehicleTypeEnum.UUV;
        else if ("USV".equalsIgnoreCase(type))
            return VehicleTypeEnum.USV;
        else if ("ASV".equalsIgnoreCase(type))
            return VehicleTypeEnum.USV;
        else if ("UAV".equalsIgnoreCase(type))
            return VehicleTypeEnum.UAV;
        else if ("UGV".equalsIgnoreCase(type))
            return VehicleTypeEnum.UGV;
        else if ("AGV".equalsIgnoreCase(type))
            return VehicleTypeEnum.UGV;
        else
            return VehicleTypeEnum.UNKNOWN;
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		ConfigFetch.initialize();
//		VehicleType vehicle = VehiclesHolder.getVehicleById("lauv-blue");
//		ImcSystem imcSystem = new ImcSystem(vehicle);
//		NeptusLog.pub().info("<###>Id: " + imcSystem.getId() +
//				" | Name: " + imcSystem.getName() + 
//				" | Type: " + imcSystem.getType());
//		NeptusLog.pub().info("<###> "+imcSystem.getInetSocketAddress());
//		imcSystem = new ImcSystem(new ImcId16("e3:33"));
//		NeptusLog.pub().info("<###>Id: " + imcSystem.getId() +
//				" | Name: " + imcSystem.getName() + 
//				" | Type: " + imcSystem.getType());
//		NeptusLog.pub().info("<###> "+imcSystem.getInetSocketAddress());
		
		NeptusLog.pub().info("<###> " + translateSystemTypeFromMessage("CCU"));
	}
}
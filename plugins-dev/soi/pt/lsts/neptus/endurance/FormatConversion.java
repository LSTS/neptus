package pt.lsts.neptus.endurance;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.bind.DatatypeConverter;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import pt.lsts.imc.Announce;
import pt.lsts.imc.AnnounceService;
import pt.lsts.imc.Base64;
import pt.lsts.imc.Goto;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCFieldType;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Loiter;
import pt.lsts.imc.MessageFactory;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.PopUp;
import pt.lsts.imc.def.SystemType;
import pt.lsts.neptus.messages.TupleList;

public class FormatConversion {

    /** To avoid initialization */
    private FormatConversion() {
    }
    
	public static IMCMessage fromJson(String json) throws ParseException {
		return fromJson(Json.parse(json).asObject());
	}
	
	public static String asJson(IMCMessage msg) {
		return asJsonObject(msg, true).toString();
	}

	private static IMCMessage fromJson(JsonObject obj) throws ParseException {
		String abbrev = obj.getString("abbrev", "");
		if (abbrev.isEmpty())
			throw new ParseException("Object doesn't have 'abbrev' field", 0);
		IMCMessage msg = MessageFactory.getInstance().createTypedMessage(abbrev, IMCDefinition.getInstance());
		if (msg == null)
			throw new ParseException("No message named '"+abbrev+"'", 0);
		
		msg.setTimestamp(obj.getDouble("timestamp", msg.getTimestamp()));
		msg.setSrc(obj.getInt("src", msg.getSrc()));
		msg.setSrcEnt(obj.getInt("src_ent", msg.getSrcEnt()));
		msg.setDst(obj.getInt("dst", msg.getDst()));
		msg.setDstEnt(obj.getInt("dst_ent", msg.getDstEnt()));

		for (String fieldName : msg.getMessageType().getFieldNames()) {
		    IMCFieldType fieldType = msg.getMessageType().getFieldType(fieldName);
		    try {
		        String unit = msg.getMessageType().getFieldUnits(fieldName);
		        if (unit != null && unit.equalsIgnoreCase("Enumerated")) {
		            JsonValue val = obj.get(fieldName);
		            if (!val.isNumber()) {
		                long valEnumLong = 0;
		                LinkedHashMap<String, Long> meanings = msg.getMessageType().getFieldMeanings(fieldName);
		                String key = val.asString().trim();
		                if (!meanings.containsKey(key)) {
		                    valEnumLong = meanings.values().iterator().next();
		                }
		                else {
		                    valEnumLong = meanings.get(key);
		                }
		                
		                switch (fieldType) {
		                    case TYPE_INT8:
		                    case TYPE_INT16:
		                    case TYPE_INT32:
		                    case TYPE_UINT8:
		                    case TYPE_UINT16:
		                        msg.setValue(fieldName, (int) valEnumLong);
		                        break;
		                    case TYPE_UINT32:
		                    case TYPE_INT64:
                                msg.setValue(fieldName, valEnumLong);
		                        break;
                            default:
                                break;
                        }
		                continue;
		            }
		        }
		        else if (unit != null && unit.equalsIgnoreCase("Bitfield")) {
		            JsonValue val = obj.get(fieldName);
		            if(!val.isNumber()) {
		                String tmp = val.asString();
		                LinkedHashMap<String, Boolean> bfv = msg.getBitmask(fieldName);
		                for (String key : bfv.keySet()) {
                            bfv.put(key, false);
                        }
		                String[] elements = tmp.replaceAll("^\\[", "").replaceAll("\\]$", "").split(",");
		                for (String elm : elements) {
                            if(bfv.containsKey(elm.trim()))
                                bfv.put(elm.trim(), true);
                        }
		                msg.setBitMask(fieldName, bfv);
		                continue;
		            }
		        }
		        else if (unit != null && unit.equalsIgnoreCase("TupleList")) {
		            msg.setValue(fieldName, new TupleList(obj.getString(fieldName, "").toString()));
		            continue;
		        }
                switch (fieldType) {
		            case TYPE_PLAINTEXT:
		                msg.setValue(fieldName, obj.getString(fieldName, ""));
		                break;
		            case TYPE_RAWDATA:
		                String data = obj.getString(fieldName, "");
		                msg.setValue(fieldName, DatatypeConverter.parseHexBinary(data));
		                break;
		            case TYPE_MESSAGE:
		                JsonObject objjson = obj.get(fieldName).asObject();
		                if (objjson != null && !objjson.isNull() && !objjson.isEmpty())
		                    msg.setValue(fieldName, fromJson(objjson));
		                break;
		            case TYPE_MESSAGELIST:
		                Vector<IMCMessage> msgs = new Vector<>();
		                JsonArray array = obj.get(fieldName).asArray();
		                for (JsonValue v : array.values()) {
		                    msgs.add(fromJson(v.asObject()));
		                }
		                msg.setMessageList(msgs, fieldName);
		                break;
		            case TYPE_FP32:
		                msg.setValue(fieldName, obj.getFloat(fieldName, 0f));
		                break;
		            case TYPE_FP64:
		                msg.setValue(fieldName, obj.getDouble(fieldName, 0d));
		                break;
		            case TYPE_INT8:
		            case TYPE_INT16:
		            case TYPE_INT32:
		            case TYPE_UINT8:
		            case TYPE_UINT16:
		                msg.setValue(fieldName, obj.getInt(fieldName, 0));
		                break;
		            case TYPE_UINT32:
		            case TYPE_INT64:
		                msg.setValue(fieldName, obj.getLong(fieldName, 0L));
		                break;
		            default:
		                //msg.setValue(fieldName, obj.getString(fieldName, ""));
		                break;
		        }
		    }
		    catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		
		return msg;
	}
	
	private static JsonObject asJsonObject(IMCMessage msg, boolean toplevel) {
		JsonObject obj = new JsonObject();

		if (msg == null)
			return obj;

		obj.add("abbrev", msg.getClass().getSimpleName());

		if (toplevel) {
			obj.add("timestamp", msg.getTimestamp());
			obj.add("src", msg.getSrc());
			obj.add("src_ent", msg.getSrcEnt());
			obj.add("dst", msg.getDst());
			obj.add("dst_ent", msg.getDstEnt());
		}

		for (String fieldName : msg.getMessageType().getFieldNames()) {
		    IMCFieldType fieldType = msg.getMessageType().getFieldType(fieldName);
		    try {
		        switch (fieldType) {
		            case TYPE_PLAINTEXT:
		                obj.add(fieldName, msg.getValue(fieldName).toString());
		                break;
		            case TYPE_RAWDATA:
		                byte[] bytes = msg.getRawData(fieldName);
		                if (bytes == null) {
		                    obj.add(fieldName, "");                 
		                } 
		                else {
		                    obj.add(fieldName, Base64.encode(bytes));
		                }
		                break;
		            case TYPE_MESSAGE:
		                IMCMessage msg1 = null;
		                msg1 = msg.getMessage(fieldName);
		                if (msg1 != null)
		                    obj.add(fieldName, asJsonObject(msg1, false));
		                else
		                    obj.add(fieldName, new JsonObject());
		                break;
		            case TYPE_MESSAGELIST:
		                Vector<IMCMessage> msgs = null;
		                msgs = msg.getMessageList(fieldName);
		                if (msgs == null)
		                    msgs = new Vector<IMCMessage>();
		                JsonArray arr = new JsonArray();
		                for (IMCMessage m : msgs) {
		                    arr.add(asJsonObject(m, false));
		                }
		                obj.add(fieldName, arr);
		                break;
		            case TYPE_FP32:
		                obj.add(fieldName, msg.getFloat(fieldName));
		                break;
		            case TYPE_FP64:
		                obj.add(fieldName, msg.getDouble(fieldName));
		                break;
		            case TYPE_INT8:
		            case TYPE_INT16:
		            case TYPE_INT32:
		            case TYPE_UINT8:
		            case TYPE_UINT16:
		                obj.add(fieldName, msg.getInteger(fieldName));
		                break;
		            case TYPE_UINT32:
		            case TYPE_INT64:
		                obj.add(fieldName, msg.getLong(fieldName));
		                break;
		            default:
		                obj.add(fieldName, msg.getAsString(fieldName));
		                break;
		        }
		    }
		    catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		return obj;
	}

	public static void main(String[] args) {
        Collection<String> msStrLst = IMCDefinition.getInstance().getConcreteMessages();
        long total = msStrLst.size();
        long passed = 0;
        for (String str : msStrLst) {
            IMCMessage msg = IMCDefinition.getInstance().create(str, new Object[0]);
            String jsonStr = asJson(msg);
            System.out.println("Original:\n" + jsonStr);
            try {
                IMCMessage msg2 = fromJson(jsonStr);
                String jsonStr2 = asJson(msg2);
                System.out.println("Created from JSON\n" + jsonStr2);
                boolean eql = jsonStr.equalsIgnoreCase(jsonStr2);
                if (eql)
                    passed++;
                System.out.println("Is it equal?   " + eql);
            }
            catch (ParseException e) {
                System.out.println("Error on " + msg.getAbbrev());
                e.printStackTrace();
            }
        }
        
        ArrayList<IMCMessage> testMessages = new ArrayList<>();
        
        AnnounceService announceService = new AnnounceService();
        announceService.setService("test");
        announceService.setServiceType((short) 2);
        testMessages.add(announceService);
        
        Announce announce = new Announce();
        announce.setLat(Math.toRadians(41));
        announce.setLon(Math.toRadians(-9));
        announce.setSysName("lauv-explore-1");
        announce.setSysType(SystemType.UAV);
        testMessages.add(announce);
        
        Goto gotoMsg = new Goto();
        gotoMsg.setCustom("param1=1; param2 = \"hello\";");
        testMessages.add(gotoMsg);
        
        PlanDB planDB = new PlanDB();
        planDB.setOp(PlanDB.OP.SET);
        PlanSpecification pSpec = new PlanSpecification();
        Vector<IMCMessage> mans = new Vector<>();
        mans.add(gotoMsg);
        Loiter loiter = new Loiter();
        mans.add(loiter);
        pSpec.setMessageList(mans, "maneuvers");
        planDB.setArg(pSpec);
        testMessages.add(planDB);
        
        PopUp popup = new PopUp();
        LinkedHashMap<String, Boolean> popupFlagsBitmask = new LinkedHashMap<>();
        popupFlagsBitmask.put("CURR_POS", true);
        popupFlagsBitmask.put("STATION_KEEP", true);
        popup.setBitMask("flags", popupFlagsBitmask);
        testMessages.add(popup);
        
        long total1 = testMessages.size();
        long passed1 = 0;
        for (IMCMessage msg : testMessages) {
            String jsonStr = asJson(msg);
            System.out.println("Original:\n" + jsonStr);
            try {
                IMCMessage msg2 = fromJson(jsonStr);
                String jsonStr2 = asJson(msg2);
                System.out.println("Created from JSON\n" + jsonStr2);
                boolean eql = jsonStr.equalsIgnoreCase(jsonStr2);
                if (eql)
                    passed1++;
                System.out.println("Is it equal?   " + eql);
            }
            catch (ParseException e) {
                System.out.println("Error on " + msg.getAbbrev());
                e.printStackTrace();
            }
        }

        Map<String, String> jsonMsgsStr = new LinkedHashMap<>();
        
        jsonMsgsStr.put("{\"abbrev\":\"AnnounceService\",\"timestamp\":1.51673901476E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"service\":\"test\",\"service_type\":2}", null);
        jsonMsgsStr.put("{\"abbrev\":\"AnnounceService\",\"timestamp\":1.51673901476E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"service\":\"test\",\"service_type\":\"LOCAL\"}",
                "{\"abbrev\":\"AnnounceService\",\"timestamp\":1.51673901476E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"service\":\"test\",\"service_type\":2}");
        
        jsonMsgsStr.put("{\"abbrev\":\"Announce\",\"timestamp\":1.516740375683E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"sys_name\":\"lauv-explore-1\",\"sys_type\":4,\"owner\":0,\"lat\":0.7155849933176751,\"lon\":-0.15707963267948966,\"height\":0,\"services\":\"\"}", null);
        jsonMsgsStr.put("{\"abbrev\":\"Announce\",\"timestamp\":1.516740375683E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"sys_name\":\"lauv-explore-1\",\"sys_type\":\"UAV\",\"owner\":0,\"lat\":0.7155849933176751,\"lon\":-0.15707963267948966,\"height\":0,\"services\":\"\"}",
                "{\"abbrev\":\"Announce\",\"timestamp\":1.516740375683E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"sys_name\":\"lauv-explore-1\",\"sys_type\":4,\"owner\":0,\"lat\":0.7155849933176751,\"lon\":-0.15707963267948966,\"height\":0,\"services\":\"\"}");
        
        jsonMsgsStr.put("{\"abbrev\":\"Goto\",\"timestamp\":1.516741118392E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"timeout\":0,\"lat\":0,\"lon\":0,\"z\":0,\"z_units\":0,\"speed\":0,\"speed_units\":0,\"roll\":0,\"pitch\":0,\"yaw\":0,\"custom\":\"param1=1; param2 = \\\"hello\\\";\"}", null);
        
        jsonMsgsStr.put("{\"abbrev\":\"PlanDB\",\"timestamp\":1.516741690675E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"type\":0,\"op\":0,\"request_id\":0,\"plan_id\":\"\",\"arg\":{\"abbrev\":\"PlanSpecification\",\"plan_id\":\"\",\"description\":\"\",\"vnamespace\":\"\",\"variables\":[],\"start_man_id\":\"\",\"maneuvers\":[{\"abbrev\":\"Goto\",\"timeout\":0,\"lat\":0,\"lon\":0,\"z\":0,\"z_units\":0,\"speed\":0,\"speed_units\":0,\"roll\":0,\"pitch\":0,\"yaw\":0,\"custom\":\"param1=1; param2 = \\\"hello\\\";\"},{\"abbrev\":\"Loiter\",\"timeout\":0,\"lat\":0,\"lon\":0,\"z\":0,\"z_units\":0,\"duration\":0,\"speed\":0,\"speed_units\":0,\"type\":0,\"radius\":1,\"length\":1,\"bearing\":0,\"direction\":0,\"custom\":\"\"}],\"transitions\":[],\"start_actions\":[],\"end_actions\":[]},\"info\":\"\"}", null);
        
        jsonMsgsStr.put("{\"abbrev\":\"PopUp\",\"timestamp\":1.516741932809E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"timeout\":0,\"lat\":0,\"lon\":0,\"z\":0,\"z_units\":0,\"speed\":0,\"speed_units\":0,\"duration\":0,\"radius\":1,\"flags\":5,\"custom\":\"\"}", null);
        jsonMsgsStr.put("{\"abbrev\":\"PopUp\",\"timestamp\":1.516741932809E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"timeout\":0,\"lat\":0,\"lon\":0,\"z\":0,\"z_units\":0,\"speed\":0,\"speed_units\":0,\"duration\":0,\"radius\":1,\"flags\":\"CURR_POS,STATION_KEEP\",\"custom\":\"\"}",
                "{\"abbrev\":\"PopUp\",\"timestamp\":1.516741932809E9,\"src\":0,\"src_ent\":0,\"dst\":65535,\"dst_ent\":255,\"timeout\":0,\"lat\":0,\"lon\":0,\"z\":0,\"z_units\":0,\"speed\":0,\"speed_units\":0,\"duration\":0,\"radius\":1,\"flags\":5,\"custom\":\"\"}");
        
        long total2 = jsonMsgsStr.size();
        long passed2 = 0;
        for (String jsonStr : jsonMsgsStr.keySet()) {
            System.out.println("JSON Original:\n" + jsonStr);
            try {
                IMCMessage msg2 = fromJson(jsonStr);
                String jsonStr2 = asJson(msg2);
                System.out.println("Created from JSON\n" + jsonStr2);
                String jsonTest = jsonStr;
                if (jsonMsgsStr.containsKey(jsonStr)) {
                    jsonTest = jsonMsgsStr.get(jsonStr);
                    if (jsonTest == null)
                        jsonTest = jsonStr;
                }
                boolean eql = jsonTest.equalsIgnoreCase(jsonStr2);
                if (eql)
                    passed2++;
                System.out.println("Is it equal?   " + eql);
            }
            catch (ParseException e) {
                System.out.println("Error on ");
                e.printStackTrace();
            }
        }
        
        System.out.println("Test1  ::  Total " + total + " passed " + passed);
        System.out.println("Test2  ::  Total " + total1 + " passed " + passed1);
        System.out.println("Test3  ::  Total " + total2 + " passed " + passed2);
	}
}

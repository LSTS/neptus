/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: lsts
 * 10/04/2017
 */
package pt.lsts.neptus.nvl.imc.dsl

import org.dom4j.Element;
import com.l2fprod.common.propertysheet.Property
import pt.lsts.imc.EntityParameter
import pt.lsts.imc.IMCMessage
import pt.lsts.imc.SetEntityParameters
import pt.lsts.neptus.gui.PropertiesEditor
import pt.lsts.neptus.mp.actions.PlanActions

/**
 * @author lsts
 *
 */
class Payload {
    
    
    HashMap <String,String[]> available_params //TODO verify entityParameters settings
    String name
    List<EntityParameter> params
    
    public Payload(String nombre) {
        available_params = new HashMap<>()
        params = new ArrayList<>()
//        available_params.put("Multibeam",["Range"])
//        available_params.put("Camera",["Frequency"])
        name=nombre
      }
    
    public void active(boolean isActive){
        //TODO verify if it is a IMC payload ou Comm mean requirement (acoustic modem)
        isActive ?  params.add(new EntityParameter("Active","true")) : params.add(new EntityParameter("Active","false"))
    }
    public void property (String prop,int value){
        params.add(new EntityParameter(prop,r.toString()))
    }
    public void property (int value){
        params.add(new EntityParameter("Frequency",value.toString()))
    }

    public static Property  properties(String maneuver,List<Payload> payloads) {
        //Maneuver -> startActions -> set entityParameters -> name-> param: Active, value: true
        
        List<SetEntityParameters> setEntities = new ArrayList<>()
        payloads.each{
            def sEntityP = new SetEntityParameters() //new SetEntityParameters(it.name,it.params)
            sEntityP.setName it.name
            sEntityP.setParams it.params
            setEntities.add sEntityP
            } 
        PlanActions startActions = new PlanActions()
        Vector<IMCMessage> msg = new Vector<>()
        setEntities.each{ msg.add(it) }
        startActions.parseMessages(msg)
        Property startActionsProperty = PropertiesEditor.getPropertyInstance("start-actions", maneuver + " start actions",
                PlanActions.class, startActions, false)
//        Node nd = doc.selectSingleNode("./node/actions/start-actions");
//        if (nd != null) {
//            man.startActions.load((Element) nd);
//    }
        startActionsProperty
  }
}

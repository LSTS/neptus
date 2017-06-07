package pt.lsts.neptus.plugins.nvl.dsl

import pt.lsts.imc.IMCMessage
import pt.lsts.nvl.runtime.NodeSet
import pt.lsts.nvl.runtime.Node
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager
import pt.lsts.neptus.plugins.nvl.*
import pt.lsts.neptus.types.mission.plan.PlanType
import pt.lsts.neptus.comm.IMCSendMessageUtils
import pt.lsts.neptus.comm.IMCUtils
import pt.lsts.imc.PlanDB
import pt.lsts.imc.PlanDB.OP
import pt.lsts.imc.PlanDB.TYPE

class Instructions {

  static IMCPlanTask imcPlan(String id) {
    NeptusPlatform.INSTANCE.getPlatformTask(id)
  }
  
  static IMCPlanTask imcPlan(Closure cl) {
      def dslPlan = new DSLPlan()

      def code = cl.rehydrate(dslPlan, this, this)
      code.resolveStrategy = Closure.DELEGATE_ONLY
      code()
      def ps = dslPlan.asPlanSpecification()
      NeptusPlatform.INSTANCE.storeInConsole(ps)
      println "Got here"
      new IMCPlanTask(ps)

  }
  
  static void sendMessage(NodeSet nodes, IMCMessage message) {
     for (Node n : nodes) {
       NeptusPlatform.INSTANCE.displayMessage 'Sending \'%s\' to \'%s\'', 
                                               message.getAbbrev(), 
                                               n.getId()
       ImcMsgManager.getManager().sendMessageToSystem message,
                                                      n.getId()         
     }
  }  
     static void storePlan(NodeSet nodes, IMCPlanTask task) {
         def message = new PlanDB(TYPE.REQUEST,OP.SET,IMCSendMessageUtils.getNextRequestId(),task.id,task.getPlanSpecification(),"NVL Task")

                  
         for (Node n : nodes) {
           NeptusPlatform.INSTANCE.displayMessage 'Sending \'%s\' to \'%s\'',
                                                   task.id,
                                                   n.getId()
           ImcMsgManager.getManager().sendMessageToSystem message,
                                                          n.getId()
         }
  }
  
  static main(args) {
    NeptusPlatform.INSTANCE.displayMessage 'Neptus language extensions loaded!'
  }
}


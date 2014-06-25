/*
 * Constraints.cc
 *
 *  Created on: Jun 16, 2014
 *      Author: fpy
 */

#include "LatLonDist.hh"

#include <PLASMA/CFunctions.hh>
#include <PLASMA/Module.hh>

namespace EUROPA {
  
  // Define the function ll_dist based on the ll_distance constraint
  DECLARE_FUNCTION_TYPE(LatLonDist, ll_dist,
                        "ll_distance", EUROPA::FloatDT, 4);

} // EUROPA

class neptus_module :public EUROPA::Module {
public:
  neptus_module():EUROPA::Module("LSTS Neptus") {}
  
  void initialize();
  void uninitialize();
  
  void initialize(EUROPA::EngineId engine);
  void uninitialize(EUROPA::EngineId engine);
  
};

extern "C" {
  
  EUROPA::ModuleId initializeModule() {
    return (new neptus_module())->getId();
  }
  
}

/*
 * class neptus_module
 */


void neptus_module::initialize() {
  // This was jsut to check that the module is properly loaded by java
  // std::cerr<<"Module "<<getName()<<" loaded."<<std::endl;
}

void neptus_module::uninitialize(){}

using namespace EUROPA;

void neptus_module::initialize(EngineId engine) {
  ConstraintEngineId ce;
  ce = dynamic_cast<EUROPA::ConstraintEngine *>(engine->getComponent("ConstraintEngine"))->getId();
  CESchemaId s = ce->getCESchema();
  
  REGISTER_CONSTRAINT(s, LatLonDist, "ll_distance", "Default");
  // this should define the function ll_dist
  s->registerCFunction((new LatLonDistFunction())->getId());
}

void neptus_module::uninitialize(EUROPA::EngineId engine) {
}


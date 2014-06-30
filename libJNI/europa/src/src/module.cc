/*
 * Constraints.cc
 *
 *  Created on: Jun 16, 2014
 *      Author: fpy
 */

#include "LatLonDist.hh"
#include "to_float.hh"
#include "sqrt.hh"

#include <PLASMA/CFunctions.hh>
#include <PLASMA/Module.hh>
#include <PLASMA/Debug.hh>

#include <fstream>
#include <ctime>
#include <locale.h>

namespace EUROPA {
  
  // Define the function ll_dist based on the ll_distance constraint
  DECLARE_FUNCTION_TYPE(LatLonDist, ll_dist,
                        "ll_distance", EUROPA::FloatDT, 4);
  DECLARE_FUNCTION_TYPE(float_to_int, to_float,
			"float_from_int", EUROPA::FloatDT, 1); 
  DECLARE_FUNCTION_TYPE(sqrt_cstr, sqrt,
			"sqrtf", EUROPA::FloatDT, 1);

} // EUROPA

class neptus_module :public EUROPA::Module {
public:
  neptus_module():EUROPA::Module("LSTS Neptus") {}
  
  void initialize();
  void uninitialize();
  
  void initialize(EUROPA::EngineId engine);
  void uninitialize(EUROPA::EngineId engine);
  
  
  std::ofstream m_dbg_out;
};

extern "C" {

  EUROPA::ModuleId initializeModule() {
	  // NDDL Parser requires is locale-dependent (en_US.UTF8 and this one are known to work)
	  std::cout << "Former locale was " << setlocale(LC_ALL, NULL) << std::endl;
	  setlocale(LC_ALL, "C");
	  std::cout << "Current locale is " << setlocale(LC_ALL, NULL) << std::endl;
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
  
  std::string where = engine->getConfig()->getProperty("neptus.cfgPath");
  if( !where.empty() ) {
    // First make sure that debug messages will be redirected to a file
    m_dbg_out.open((where+"/Europa.log").c_str(),
                   std::ofstream::out | std::ofstream::app);
    // Produce a Marker
    std::time_t my_date = std::time(NULL);
    m_dbg_out<<"\n>>>>>>>>>>>> "<<std::asctime(std::localtime(&my_date))
             <<std::endl;
    DebugMessage::setStream(m_dbg_out);
    std::cout<<"Europa debug redirected to "<<where<<"/Europa.log"<<std::endl;
    
    // Then load the configuration
    where += "/Debug.cfg";
    std::ifstream dbg(where.c_str());
    DebugMessage::readConfigFile(dbg);
  }
  
  
  
  REGISTER_CONSTRAINT(s, LatLonDist, "ll_distance", "Default");
  REGISTER_CONSTRAINT(s, float_to_int, "float_from_int", "Default");
  REGISTER_CONSTRAINT(s, sqrt_cstr, "sqrtf", "Default");
  // this should define the function ll_dist
  s->registerCFunction((new LatLonDistFunction())->getId());
  s->registerCFunction((new float_to_intFunction())->getId());
  s->registerCFunction((new sqrt_cstrFunction())->getId());
}

void neptus_module::uninitialize(EUROPA::EngineId engine) {
}


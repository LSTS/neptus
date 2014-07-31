#include "to_float.hh"
#include <PLASMA/Number.hh>

using namespace EUROPA;

/*
 * class float_to_int
 */
float_to_int::float_to_int(LabelStr const &name,
			   LabelStr const &propagator,
			   ConstraintEngineId const &cstrEngine,
			   std::vector<ConstrainedVariableId> const &args)
  :Constraint(name, propagator, cstrEngine, args),
  m_float(getCurrentDomain(m_variables[0])),
  m_int(getCurrentDomain(m_variables[1])) {}

void float_to_int::handleExecute() {
  edouble f_lo, f_hi, i_lo, i_hi;
  
  // Restrict firts the domain of the float to be integer
  m_float.getBounds(f_lo, f_hi);
  f_lo = std::ceil(f_lo);
  f_hi = std::floor(f_hi);
  m_int.getBounds(i_lo, i_hi);
  
  i_lo = std::max(i_lo, f_lo);
  i_hi = std::min(i_hi, f_hi);
  
  m_float.intersect(i_lo, i_hi);
  m_int.intersect(i_lo, i_hi);
}

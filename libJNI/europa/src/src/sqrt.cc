#include "sqrt.hh"
#include <PLASMA/Domains.hh>

using namespace EUROPA;

/*
 * class float_to_int
 */
sqrt_cstr::sqrt_cstr(LabelStr const &name,
		     LabelStr const &propagator,
		     ConstraintEngineId const &cstrEngine,
		     std::vector<ConstrainedVariableId> const &args)
  :Constraint(name, propagator, cstrEngine, args),
   m_root(getCurrentDomain(m_variables[0])),
   m_square(getCurrentDomain(m_variables[1])) {}

void sqrt_cstr::handleExecute() {
  static IntervalDomain const positive(0.0,
                                       std::numeric_limits<edouble>::infinity());
  static edouble const acceptable_epsilon(1e-4);

  edouble r_lo, r_hi, s_lo, s_hi;
  
  // Start by enforcing that voth are positive
  if( m_root.intersect(positive) && m_root.isEmpty() )
    return;
  if( m_square.intersect(positive) && m_square.isEmpty() )
    return;
  // Now do the propagation
  
  for(bool done=false; !done; ) {
    done = true;
    
    m_root.getBounds(r_lo, r_hi);
    m_square.getBounds(s_lo, s_hi);

    edouble tmp_lo, tmp_hi;
    
    // compute upper bound of the square root
    if( s_hi>=std::numeric_limits<edouble>::infinity() )
      tmp_hi = s_hi;
    else
      tmp_hi = std::sqrt(s_hi);
    if( r_hi-acceptable_epsilon > tmp_hi )
      r_hi = tmp_hi;
    // compute lower bound of the square root
    tmp_lo = std::sqrt(s_lo);
    if( r_lo+acceptable_epsilon < tmp_lo )
      r_lo = tmp_lo;
    
    // Apply the computed domain
    if( m_root.intersect(r_lo, r_hi) && m_root.isEmpty() )
      return;
    
    // Compute upper bound of the square
    tmp_hi = r_hi * r_hi;
    if( s_hi-acceptable_epsilon > tmp_hi )
      s_hi = tmp_hi;
    tmp_lo = r_lo * r_lo;
    if( s_lo+acceptable_epsilon < tmp_lo )
      s_lo = tmp_lo;
    if( m_square.intersect(s_lo, s_hi) ) {
      if( m_square.isEmpty() )
        return;
      done = false;
    }
  }
}

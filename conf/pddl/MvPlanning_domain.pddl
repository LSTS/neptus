(define (domain LSTS)
  (:requirements :typing :durative-actions :fluents :equality)
  (:types location vehicle payload interest task profile - object
          )
(:predicates (at ?v - vehicle ?l - location) ; position of the vehicle
             (base ?v - vehicle ?l - location) ; base (depot) of the vehicle
             (at_oi ?o - oi ?l - location) ; position of the object of interest
             (safe_path ?l1 - location ?l2 - locaion) ; can compute a safe path from l1 to l2

             ;; task constraints ;;
             (IsAvailable ?v - vehicle)
             (HasPayload ?t - task ?v - vehicle) ; the vehicle has the payloads needed by the task
             (HasTcpOn ?v - vehicle)
             (HasSafeLocationSet ?v - vehicle)
)

(:functions (distance ?l1 ?l2 - location)
            (surveillance_distance ?a - area)
            (speed ?v - vehicle)
            (battery-consumption-move ?v - vehicle) ;battery consumption of the vehicle per 1 distance unit
            (battery-consumption-payload ?p - payload) ;;battery consumption of the payload per 1 time unit

            ;; task constraints ;;
            (battery-level ?v - vehicle)
)


(:durative-action CoverageArea
                  :parameters (?v - vehicle ?t - task)
                  :duration (= ?duration 15)
                  :condition (and (at start (IsAvailable ?v))
                                  (at start (IsActive ?v))
                                  (at start (HasPayload ?t ?v))
                                  (at start (HasTcpOn ?v))
                                  (at start (HasSafeLocationSet ?v))
                                  (at start (>= (battery-level ?v) 60))
                                  )
                  )
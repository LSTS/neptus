; author: Lukas Chrpa

(define (domain LSTS)
(:requirements :typing :durative-actions :fluents :equality)
(:types location vehicle payload interest task - object
        area oi - interest
        auv - vehicle
        camera sidescan multibeam ctd rhodamine edgetech - payload
)
(:predicates (at ?v - vehicle ?l - location) ;position of the vehicle
             (base ?v - vehicle ?l - location) ;base (depot) of the vehicle
             (at_oi ?o - oi ?l - location) ; position of the object of interest
             (entry ?a - area ?l - location) ;entry point to the area of interest
             (exit  ?a - area ?l - location) ;exit point to the area of interest
             (having ?p - payload ?v - vehicle) ;the vehicle has the payload
             (task_desc ?t - task ?i - interest ?p - payload) ; a description of the task that sample (or survey) the point of interest by the payload
	     (sampled ?t - task ?v - vehicle) ;obtained data from sampling or surveilance by the vehicle
             ;(surveyed ?v - vehicle ?a - area)
             (communicated_data ?t - task) ;data are acquired for the vehicle
             (free ?l - location) ;no vehicle in the location
             (available ?a - area) ;no vehicle is surveiling the area
)

(:functions (distance ?l1 ?l2 - location)
            (surveillance_distance ?a - area)
            (speed ?v - vehicle)
            (battery-level ?v - vehicle)
            (battery-consumption-move ?v - vehicle) ;battery consumption of the vehicle per 1 distance unit
            (battery-consumption-payload ?p - payload) ;;battery consumption of the payload per 1 time unit
)

(:durative-action move
:parameters (?v - vehicle ?l1 ?l2 - location)
:duration (= ?duration (/ (distance ?l1 ?l2)(speed ?v)))
:condition (and (at start (at ?v ?l1))(at end (free ?l2))(at start (>= (battery-level ?v)(* (battery-consumption-move ?v)(distance ?l1 ?l2)))))
:effect (and (at start (not (at ?v ?l1)))(at end (at ?v ?l2))(at start (free ?l1))(at end (not (free ?l2)))(at start (decrease (battery-level ?v)(* (battery-consumption-move ?v)(distance ?l1 ?l2)))))
)

(:durative-action sample
:parameters (?v - vehicle ?l - location ?t -task ?o -oi ?p - payload)
:duration (= ?duration 60)
:condition (and (over all (at_oi ?o ?l))(over all (task_desc ?t ?o ?p))(over all (at ?v ?l))(over all (having ?p ?v))(at start (>= (battery-level ?v)(* (battery-consumption-payload ?p) 60))))
:effect (and (at end (sampled ?t ?v))(at start (decrease (battery-level ?v)(* (battery-consumption-payload ?p) 60))))
)

(:durative-action survey-one-payload
:parameters (?v - vehicle ?l1 ?l2 - location ?t -task ?a -area ?p - payload)
:duration (= ?duration (/ (surveillance_distance ?a)(speed ?v)))
:condition (and (over all (entry ?a ?l1))(over all (exit ?a ?l2))(over all (having ?p ?v))(over all (task_desc ?t ?a ?p))(at start (at ?v ?l1))(at end (free ?l2))(at start (available ?a))(at start (>= (battery-level ?v)(+ (* (battery-consumption-move ?v)(surveillance_distance ?a))(* (battery-consumption-payload ?p) (/ (surveillance_distance ?a)(speed ?v)))))))
:effect (and (at end (sampled ?t ?v))(at start (not (available ?a)))(at end (available ?a))(at start (not (at ?v ?l1)))(at end (at ?v ?l2))(at start (free ?l1))(at end (not (free ?l2)))(at start (decrease (battery-level ?v)(+ (* (battery-consumption-move ?v)(surveillance_distance ?a))(* (battery-consumption-payload ?p) (/ (surveillance_distance ?a)(speed ?v)))))))
)

(:durative-action survey-two-payload
:parameters (?v - vehicle ?l1 ?l2 - location ?t ?t2 -task ?a -area ?p ?p2 - payload)
:duration (= ?duration (/ (surveillance_distance ?a)(speed ?v)))
:condition (and (over all (entry ?a ?l1))(over all (exit ?a ?l2))(over all (having ?p ?v))(over all (task_desc ?t ?a ?p))(over all (having ?p2 ?v))(over all (task_desc ?t2 ?a ?p2))(over all (not (= ?t ?t2)))(at start (at ?v ?l1))(at end (free ?l2))(at start (available ?a))(at start (>= (battery-level ?v)(+ (* (battery-consumption-move ?v)(surveillance_distance ?a))(* (+ (battery-consumption-payload ?p)(battery-consumption-payload ?p2)) (/ (surveillance_distance ?a)(speed ?v)))))))
:effect (and (at end (sampled ?t ?v))(at end (sampled ?t2 ?v))(at start (not (available ?a)))(at end (available ?a))(at start (not (at ?v ?l1)))(at end (at ?v ?l2))(at start (free ?l1))(at end (not (free ?l2)))(at start (decrease (battery-level ?v)(+ (* (battery-consumption-move ?v)(surveillance_distance ?a))(* (+ (battery-consumption-payload ?p)(battery-consumption-payload ?p2)) (/ (surveillance_distance ?a)(speed ?v)))))))
)

(:durative-action survey-three-payload
:parameters (?v - vehicle ?l1 ?l2 - location ?t ?t2 ?t3 -task ?a -area ?p ?p2 ?p3 - payload)
:duration (= ?duration (/ (surveillance_distance ?a)(speed ?v)))
:condition (and (over all (entry ?a ?l1))(over all (exit ?a ?l2))(over all (having ?p ?v))(over all (task_desc ?t ?a ?p))(over all (having ?p2 ?v))(over all (task_desc ?t2 ?a ?p2))(over all (having ?p3 ?v))(over all (task_desc ?t3 ?a ?p3))(over all (not (= ?t ?t2)))(over all (not (= ?t ?t3)))(over all (not (= ?t3 ?t2)))(at start (at ?v ?l1))(at end (free ?l2))(at start (available ?a))(at start (>= (battery-level ?v)(+ (* (battery-consumption-move ?v)(surveillance_distance ?a))(* (+ (battery-consumption-payload ?p)(+ (battery-consumption-payload ?p2)(battery-consumption-payload ?p3))) (/ (surveillance_distance ?a)(speed ?v)))))))
:effect (and (at end (sampled ?t ?v))(at end (sampled ?t2 ?v))(at end (sampled ?t3 ?v))(at start (not (available ?a)))(at end (available ?a))(at start (not (at ?v ?l1)))(at end (at ?v ?l2))(at start (free ?l1))(at end (not (free ?l2)))(at start (decrease (battery-level ?v)(+ (* (battery-consumption-move ?v)(surveillance_distance ?a))(* (+ (battery-consumption-payload ?p)(+ (battery-consumption-payload ?p2)(battery-consumption-payload ?p3))) (/ (surveillance_distance ?a)(speed ?v)))))))
)

(:durative-action communicate
:parameters (?v - vehicle ?l - location ?t - task)
:duration (= ?duration 60)
:condition (and (over all (base ?v ?l))(over all (at ?v ?l))(at start (sampled ?t ?v)))
:effect (and (at end (communicated_data ?t))(at end (not (sampled ?t ?v))))
)

)

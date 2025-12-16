package org.matsim.myproject.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;

// MATSim Public Tutorial 14.x (2022), Seminar 4
// handling departureEvent by putting time and personID in Map
// handling arrivalEvent by reading arrivalTime for personID
// subtracting corresponding departureTime
// print out

public class SimplePersonEventHandler implements PersonArrivalEventHandler, PersonDepartureEventHandler {

    // a simple var would be overridden by other events
    private final Map<Id<Person>, Double> personToDepartureTime = new HashMap<>();

    @Override
    public void handleEvent( PersonDepartureEvent personDepartureEvent ) {

        var departureTime = personDepartureEvent.getTime();
        var personId = personDepartureEvent.getPersonId();

        personToDepartureTime.put( personId, departureTime );

        // System.out.println("Departure: " + personDepartureEvent.getTime() + ": " + personDepartureEvent.getPersonId());
    }

    @Override
    public void handleEvent( PersonArrivalEvent personArrivalEvent ) {
        var departureTime = personToDepartureTime.get( personArrivalEvent.getPersonId() );
        var arrivalTime = personArrivalEvent.getTime();
        // after a agent arrives, one can calculate the travel time
        var travelTime = arrivalTime - departureTime;

        System.out.println("Person " + personArrivalEvent.getPersonId() + " travelled " + travelTime + " seconds.");
        //System.out.println("Arrival: " + personArrivalEvent.getTime() + ": " + personArrivalEvent.getPersonId());
    }
}

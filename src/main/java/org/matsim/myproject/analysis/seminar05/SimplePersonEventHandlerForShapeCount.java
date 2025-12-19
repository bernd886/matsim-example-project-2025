package org.matsim.myproject.analysis.seminar05;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

public class SimplePersonEventHandlerForShapeCount implements PersonArrivalEventHandler, PersonDepartureEventHandler {

    @Override
    public void handleEvent( PersonDepartureEvent event ) {
        Id<Person> personId = event.getPersonId();
        double timeOfDeparture = event.getTime();
        String mode = event.getLegMode();
    }

    @Override
    public void handleEvent( PersonArrivalEvent event ) {
        event.getLinkId();
    }


}

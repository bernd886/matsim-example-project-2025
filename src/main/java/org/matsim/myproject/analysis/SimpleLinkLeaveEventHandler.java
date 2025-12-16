package org.matsim.myproject.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// MATSim Public Tutorial 14.x (2022), Seminar 4
// when linkLeaveEvents on linkOfInterest
// adding time to array and increasing count in timeBin of Map
// offering return

public class SimpleLinkLeaveEventHandler implements LinkLeaveEventHandler {

    private static final Id<Link> linkOfInterest = Id.createLinkId("3624560720003f");

    private final ArrayList<Double> linkLeaveTimesArray = new ArrayList<>();

    private final Map<String, Integer> linkLeaveTimesMap = new HashMap<>();

    ArrayList<Double> getLinkLeaveTimesArray() {
        return linkLeaveTimesArray;
    }

    Map<String, Integer> getLinkLeaveTimesMap() {
        return linkLeaveTimesMap;
    }

    @Override
    public void handleEvent( LinkLeaveEvent linkLeaveEvent) {

        Id<Link> linkId = linkLeaveEvent.getLinkId();
        var leaveTime = linkLeaveEvent.getTime();

        if ( Objects.equals( linkId, linkOfInterest ) ) {
            // int hourBin = (int) ( leaveTime / 3600 );
            linkLeaveTimesArray.add( leaveTime );

            String key = getKey(linkLeaveEvent.getTime());
            //  int currentCount = volumes.get(key);
            //  int newCount = currentCount + 1;
            //  volumes.put(key, newCount);

            // shorter version
            linkLeaveTimesMap.merge(key, 1, Integer::sum);

        } else {}
    }

    private String getKey(double time) {
        return Integer.toString((int) (time / 3600));
    }
}

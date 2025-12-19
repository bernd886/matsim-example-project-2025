package org.matsim.myproject.analysis.seminar05;

import org.apache.commons.csv.CSVFormat;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

// MATSim Public Tutorial 14.x (2022), Seminar 5
// task:  count trips between Mitte and Friedrichshain-Kreuzberg

public class CountActivitiesBetweenDistrictsByHandler {

    private static final URL context = IOUtils.getFileUrl( "C:\\Users\\lenovo\\Desktop\\05" ) ;
    private static final URL shapeFileUrl = IOUtils.extendUrl( context, "Berlin_Bezirke.shp" ) ;
    private static final URL eventsFileUrl = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_events.xml.gz" ) ;
    private static final URL networkFileUrl = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_network.xml.gz" ) ;
    private static final URL populationFileUrl = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_plans.xml.gz" ) ;

    public static void main(String[] args) throws IOException {

        var features = GeoFileReader.getAllFeatures( shapeFileUrl ) ;

        String nameFromDistrict = "001";
        String nameToDistrict = "002" ;

        var fromGeometry = features.stream()
                .filter( simpleFeature -> simpleFeature.getAttribute( "Gemeinde_s" ).equals( "001" ) )
                .map( simpleFeature -> simpleFeature.getDefaultGeometry() )
                .findAny()
                .orElseThrow() ;

        var network = NetworkUtils.readNetwork( networkFileUrl.toString()) ;
        var population = PopulationUtils.readPopulation( populationFileUrl.toString() ) ;

        var handler = new SimplePersonEventHandlerForShapeCount();

        // using constructor method from matsimUtils
        // manager manages the EventsHandlers, pushes the events into the EventsHandlers
        var manager = EventsUtils.createEventsManager();

        manager.addHandler( handler );

        EventsUtils.readEvents( manager, eventsFileUrl.toString() );

        //var linkLeaveTimesArray = linkHandler.getLinkLeaveTimesArray();

        //var linkLeaveTimesMap = linkHandler.getLinkLeaveTimesMap();

        // writing out array
//        try ( var writer = Files.newBufferedWriter( Paths.get(outFile)); var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("Time").print(writer)) {
//            for (Double time : linkLeaveTimesArray) {
//                writer.write(time + "\n");  // Each number on a new line
//            }
//            System.out.println("File written successfully: " + outFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        /*
        // writing out map
        try ( var writer = Files.newBufferedWriter( Paths.get(outFile)); var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("Hour", "Value").print(writer)) {
            for (var volume : linkLeaveTimesMap.entrySet()) {
                printer.printRecord(volume.getKey(), volume.getValue());
                printer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
}

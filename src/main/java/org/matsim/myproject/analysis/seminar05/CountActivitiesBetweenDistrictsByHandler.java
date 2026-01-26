package org.matsim.myproject.analysis.seminar05;

import org.apache.commons.csv.CSVFormat;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

// MATSim Public Tutorial 14.x (2022), Seminar 5
// task:  count trips between Mitte and Friedrichshain-Kreuzberg

public class CountActivitiesBetweenDistrictsByHandler {

    private static final URL context = IOUtils.getFileUrl( "C:\\Users\\lenovo\\Desktop\\05" ) ;
    private static final URL shapeFileUrl = IOUtils.extendUrl( context, "Berlin_Bezirke.shp" ) ;
    private static final URL eventsFileUrl = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_events.xml.gz" ) ;
    private static final URL networkFileUrl = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_network.xml.gz" ) ;
    private static final URL populationFileUrl = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_plans.xml.gz" ) ;

    private static final String nameFromDistrict = "001";
    private static final String nameToDistrict = "002";

    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:3857");


    public static void main(String[] args) throws IOException {

        var features = GeoFileReader.getAllFeatures( shapeFileUrl ) ;

        var fromGeometry = getGeometry(nameFromDistrict, features);
        var toGeometry = getGeometry(nameToDistrict, features);

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

    private static Geometry getGeometry( String identifier, Collection<SimpleFeature> features ) {
        return features.stream()
                .filter(feature -> feature.getAttribute("Gemeinde_s").equals( identifier ))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .findAny()
                .orElseThrow();
    }
}

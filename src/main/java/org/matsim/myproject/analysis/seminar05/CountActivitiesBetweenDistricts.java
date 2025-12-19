package org.matsim.myproject.analysis.seminar05;

import org.apache.commons.csv.CSVFormat;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// MATSim Public Tutorial 14.x (2022), Seminar 5
// task:  count trips between Mitte and Friedrichshain-Kreuzberg

public class CountActivitiesBetweenDistricts {
    public static void main(String[] args){

        final URL context = IOUtils.getFileUrl( "C:\\Users\\lenovo\\Desktop\\05" ) ;
        final URL urlShapeFile = IOUtils.extendUrl( context, "Berlin_Bezirke.shp" ) ;
        final URL urlPlansFile = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_plans.xml.gz" ) ;

        // transformation from MATSim coordinates into the ones of the shape file
        // EPSG from plans file: 31468
        // EPSG from Web Mercator: 3857
        var transformation = TransformationFactory.getCoordinateTransformation( "EPSG:31468", "EPSG:3857" ) ;

        var features = GeoFileReader.getAllFeatures( urlShapeFile ) ;

        var geometryFHain  = features.stream()
                // Filtern nach key "Gemeinde_s" == "002"; Bezirk == FHain
                .filter( simpleFeature -> simpleFeature.getAttribute( "Gemeinde_s" ).equals( "002" ))
                // exchange the filtered-out feature into geometry object
                .map( simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry() )
                // in most times the stream operation ends with Collect operation
                .toList().getFirst();

        var geometryMitte  = features.stream()
                // Filtern nach key "Gemeinde_s" == "001"; Bezirk == Mitte
                .filter( simpleFeature -> simpleFeature.getAttribute( "Gemeinde_s" ).equals( "001" ))
                // exchange the filtered-out feature into geometry object
                .map( simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry() )
                // in most times the stream operation ends with Collect operation
                .toList().getFirst();

        // check, if the trips of persons are starting/ ending in the districts
        // reading the plans file
        var population = PopulationUtils.readPopulation( String.valueOf( urlPlansFile ) );

        int numberOfTripsBetweenMitteFHain = 0 ;
        int numberOfTripsBetweenFHainMitte = 0 ;

        List<Map.Entry<String, Double>> ListOfMainLegsMitte2FHain = new ArrayList<>();
        List<Map.Entry<String, Double>> ListOfMainLegsFHain2Mitte = new ArrayList<>();

        for ( Person person : population.getPersons().values() ) {

            var plan = person.getSelectedPlan() ;

            // Utils class to extract from plans
            var trips =  TripStructureUtils.getTrips( plan ) ;

            // we still have a list over which we have to iterate again
            for ( var trip : trips ) {

                String mainTripLegMode = "" ;
                double mainTripLegDistance = 0 ;

                var legs = trip.getLegsOnly() ;

                for ( var leg : legs ) {
                    double legDistance = leg.getRoute().getDistance() ;
                    if ( legDistance > mainTripLegDistance ) {
                        mainTripLegDistance = legDistance;
                        mainTripLegMode = leg.getMode();
                    }
                }

                Coord destinationActivityCoord = trip.getDestinationActivity().getCoord();
                Coord originActivityCoord = trip.getOriginActivity().getCoord();

                // transform into coordinate system of shape file
                var transformedDestinationActivityCoord = transformation.transform( destinationActivityCoord ) ;
                var transformedOriginActivityCoord = transformation.transform( originActivityCoord ) ;

                // this coordinate is a MATSim coordinate data structure
                // has to be transformed into structure of GeoTools for shape files
                Point destinationPoint = MGC.coord2Point( transformedDestinationActivityCoord );
                Point originPoint = MGC.coord2Point( transformedOriginActivityCoord );

                // finally we test
                // Mitte - FHain
                if ( geometryMitte.contains( originPoint) ){
                    if ( geometryFHain.contains( destinationPoint ) ) {
                        numberOfTripsBetweenMitteFHain++ ;
                        ListOfMainLegsMitte2FHain.add( new AbstractMap.SimpleEntry<>( mainTripLegMode, mainTripLegDistance ) ) ;
                    }
                }
                // FHain - Mitte
                if ( geometryFHain.contains( originPoint ) ) {
                    if ( geometryMitte.contains( destinationPoint ) ) {
                        numberOfTripsBetweenFHainMitte++ ;
                        ListOfMainLegsFHain2Mitte.add( new AbstractMap.SimpleEntry<>( mainTripLegMode, mainTripLegDistance ) ) ;
                    }
                }
            }
        }

        try ( var writer = Files.newBufferedWriter( Path.of( "C:\\Users\\lenovo\\Desktop\\05\\MitteFHain.csv" )); var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("Origin", "Destination", "Mode", "Distance").print(writer)) {
            for (var volume : ListOfMainLegsMitte2FHain) {
                printer.printRecord("Mitte", "FHain", volume.getKey(), volume.getValue());
            }
            for (var volume : ListOfMainLegsFHain2Mitte) {
                printer.printRecord("FHain", "Mitte", volume.getKey(), volume.getValue());
            }
        } catch ( IOException e) {
            e.printStackTrace();
        }

        System.out.println( "Number of activities from" ) ;
        System.out.println( "FHain to Mitte: " + numberOfTripsBetweenFHainMitte ) ;
        System.out.println( "Mitte to FHain: " + numberOfTripsBetweenMitteFHain ) ;
    }
}

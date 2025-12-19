package org.matsim.myproject.analysis.seminar05;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;

import java.net.URL;

// MATSim Public Tutorial 14.x (2022), Seminar 5
// task: Write a script together which counts number of activities in Berlin Mitte

public class CountActivitiesInDistrictMitte {

    public static void main(String[] args){

        final URL context = IOUtils.getFileUrl( "E:\\files-sd\\Uni\\Master\\Matsim\\MATSim Public Tutorial 14.x (2022)\\05\\" ) ;
        final URL urlShapeFile = IOUtils.extendUrl( context, "Berlin_Bezirke.shp" ) ;
        final URL urlPlansFile = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_plans.xml.gz" ) ;

        // transformation from MATSim coordinates into the ones of the shape file
        // EPSG from plans file: 31468
        // EPSG from Web Mercator: 3857
        var transformation = TransformationFactory.getCoordinateTransformation( "EPSG:31468", "EPSG:3857" ) ;

        var features = GeoFileReader.getAllFeatures( urlShapeFile ) ;

        var geometries  = features.stream()
                // Filtern nach key "Gemeinde_s" == "001"; Bezirk == Mitte
                .filter( simpleFeature -> simpleFeature.getAttribute( "Gemeinde_s" ).equals( "001" ))
                // exchange the filtered-out feature into geometry object
                .map( simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry() )
                // in most times the stream operation ends with Collect operation
                .toList();

        var districtGeometry = geometries.getFirst() ;

        // check, if the activities of persons are in the district
        // reading the plans file
        var population = PopulationUtils.readPopulation( String.valueOf( urlPlansFile ) );

        int counter = 0;
        int activityCounter = 0;

        for ( Person person : population.getPersons().values() ) {

            var plan = person.getSelectedPlan() ;

            // Utils class to extract from plans
            // takes the selected plan and info how to handle interaction activities ("stageActivities")
            var activities =  TripStructureUtils.getActivities( plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities ) ;

            // we still have a list over which we have to iterate again
            for ( var activity : activities ) {

                activityCounter++ ;

                var activityCoord = activity.getCoord();

                // transform into coordinate system of shape file
                var transformedActivityCoord = transformation.transform( activityCoord ) ;

                // this coordinate is a MATSim coordinate data structure
                // has to be transformed into structure of GeoTools for shape files
                // (M)ATSim(G)eotool(C)onversion
                var geotoolsActivityCoord = MGC.coord2Coordinate( transformedActivityCoord ) ;

                // ".contains" expects a point, not coordinate
                var geotoolsActivityPoint = MGC.coord2Point( transformedActivityCoord ) ;

                // finally we test
                if ( districtGeometry.contains( geotoolsActivityPoint) ){
                    counter++ ;
                }
            }
        }

        System.out.println("Number of activities in district Mitte: " + counter + " out of a total of " + activityCounter);

    }
}

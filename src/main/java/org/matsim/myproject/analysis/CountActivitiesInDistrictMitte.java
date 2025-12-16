package org.matsim.myproject.analysis;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.URL;
import java.util.Collection;

public class CountActivitiesInDistrictMitte {

    public static void main(String[] args){

        final URL context = IOUtils.getFileUrl( "E:\\files-sd\\Uni\\Master\\Matsim\\MATSim Public Tutorial 14.x (2022)\\05\\" ) ;
        final URL urlShapeFile = IOUtils.extendUrl( context, "Berlin_Bezirke.shp" ) ;
        final URL urlPlansFile = IOUtils.extendUrl( context, "berlin-v5.5.3-1pct.output_plans.xml.gz" ) ;

        Shape
        var features = ShapeFileReader.getAllFeatures( urlShapeFile );

        features.stream()
                .filter( simpleFeature -> simpleFeature.getAttribute( "Gemeinde_s" ) ).equals( "001" )
                .map



    }
}

package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.net.URL;
import java.util.List;
import java.util.Map;

class RunMatsimFromExamplesUtils{

	public static void main( String[] args ){
//        directory name in examples: https://github.com/matsim-org/matsim-libs/tree/main/examples/scenarios
//		URL context = org.matsim.examples.ExamplesUtils.getTestScenarioURL( "equil" );
        URL context = org.matsim.examples.ExamplesUtils.getTestScenarioURL( "pt-simple" );
//        config-file name
//		URL url = IOUtils.extendUrl( context, "config.xml" );
        URL url = IOUtils.extendUrl( context, "config.xml" );


        Config config = ConfigUtils.loadConfig( url );
		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
        config.controller().setLastIteration( 1 );
        config.controller().setOutputDirectory("pt-simple-output");

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config );
        scenario.getPopulation().ad
		// ---

		Controler controler = new Controler( scenario );

		// ---

		controler.run();

	}

}

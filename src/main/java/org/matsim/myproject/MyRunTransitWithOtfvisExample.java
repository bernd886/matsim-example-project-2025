/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.myproject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.MalformedURLException;
import java.net.URL;

public class MyRunTransitWithOtfvisExample {

	public static void main( final String[] args ) {

        Config config = null ;
        if ( args != null && args.length >= 1 ) {
            config = ConfigUtils.loadConfig( args[0], new OTFVisConfigGroup() ) ;
        } else {
            //final String filename = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/cottbus/cottbus-tutorial-2016/config01.xml" ;

            final URL context = IOUtils.getFileUrl( "C:\\Users\\lenovo\\IdeaProjects\\matsim-example-project-2025\\src\\main\\java\\org\\matsim\\myproject\\" ) ;
            final URL url = IOUtils.extendUrl( context, "config.xml" ) ;

            config = ConfigUtils.loadConfig( url, new OTFVisConfigGroup() ) ;
        }

        config.controller().setOutputDirectory( "myoutput" ) ;
        config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;

        config.controller().setLastIteration( 1 ) ;

        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves ) ;
        config.qsim().setSnapshotStyle( SnapshotStyle.kinematicWaves ) ;
        config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.wait ) ;

        {
            // replanning: adding new selector strategy, which is non-innovative (from lecture 4 (2022))
            {
                ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
                stratSets.setWeight( 1. );
                stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta );
                config.replanning().addStrategySettings( stratSets );
            }
            // replanning: adding new mutator strategy (innovative)
            // "changing the location (do shopping somewhere else) is a contrib"
            {
                ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
                stratSets.setWeight( 1. );
                stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode );
                // "ChangeSingleTripMode works better than ChangeTripMode"
                config.replanning().addStrategySettings( stratSets );
            }
            {
                ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
                stratSets.setWeight( 1. );
                stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice );
                // with material dependencies in leg chains (car, pt)
                config.replanning().addStrategySettings( stratSets );
            }
            // "the mode choice modules need to know which modes are in the system
            // there are four different places, where a different mode needs to be entered
            // replanning: must be able to say: use this mode
            // router: must be able to produce a route for this mode
            // simulation: must be able to process it
            // scoring: must be able to give it a score
            String[] modes = { TransportMode.car, TransportMode.bike, "eScooter" } ;
            // preconfigured string constants
            config.changeMode().setModes( modes );
            String[] submodes ;
            config.subtourModeChoice().setModes( submodes );
        }

        //config.transit().setUseTransit( true ) ;

        OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
        visConfig.setDrawTime( true ) ;
        visConfig.setDrawNonMovingItems( true ) ;
        visConfig.setAgentSize( 125 ) ;
        visConfig.setLinkWidth( 10 ) ;
        visConfig.setDrawTransitFacilityIds( false ) ;
        visConfig.setDrawTransitFacilities( false ) ;

        if ( args.length > 1 && args[1] != null ) {
            ConfigUtils.loadConfig( config, args[1] );
            // (this loads a second config file, if you want to insist on overriding the settings so far but don't want to touch the code.  kai, aug'16)
        }

        Scenario scenario = ScenarioUtils.loadScenario( config ) ;

        final Controler controler = new Controler( scenario ) ;

//        controler.addOverridingModule( new OTFVisLiveModule() ) ;


        controler.run() ;
    }

}

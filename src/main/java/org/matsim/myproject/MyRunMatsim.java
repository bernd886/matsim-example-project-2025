/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.SimWrapperModule;

/**
 * @author nagel
 *
 */
public class MyRunMatsim {

	private static final double SAMPLESIZE = 0.1;

	public static void main( String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
        config.controller().setLastIteration( 1 );
        config.controller().setOutputDirectory("my-output");
		// possibly modify config here (first/ last iteration, learning functions, etc.)

		// downsampling
		// config.qsim().setFlowCapFactor( SAMPLESIZE );
		// config.qsim().setStorageCapFactor( SAMPLESIZE );

		// innovation switch-off and averaging for convergence of scores
		// config.replanning().setFractionOfIterationsToDisableInnovation( 0.8 ) ;
		// no more innovation (mutation), only selection between existing plans
		// config.scoring().setFractionOfIterationsToStartScoreMSA( 0.8 ) ;
		// score averaging averages the scores everytime a plan is used

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// possibly modify scenario here (infrastructure: links, persons, plans)

		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controller here (how to control the program)
        // all possible modifications subsumed under the most important ones: addOverridingModule, addOverridingQSimModule

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;

//		controler.addOverridingModule( new SimWrapperModule() );
		
		// ---
		
		controler.run();
	}
	
}

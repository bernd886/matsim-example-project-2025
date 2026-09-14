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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.SimWrapperModule;


public class MyRunMatsim {

	public static void main( String[] args ) {

		// --------------------------------------------------------------------
		// --- CONFIG ---------------------------------------------------------
		// --------------------------------------------------------------------
		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" ) ;
		} else {
			config = ConfigUtils.loadConfig( args ) ;
		}

		config.controller().setOutputDirectory( "./output/" ) ;
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) ;
		config.controller().setLastIteration( 20 ) ;

		/* possibly modify config here (first/ last iteration, learning functions, etc.) */

		// --------------------------------------------------------------------
		// --- CONFIG --- REPLANNING ------------------------------------------
		// --------------------------------------------------------------------
		/* Plan innovation (or "strategy")

		 * innovation switch-off.
		 * no more innovation (mutation), only selection between existing plans.
		 * Should be used with averaging scores (see SCORING). */
		config.replanning().setFractionOfIterationsToDisableInnovation( 0.8 ) ;

		// --------------------------------------------------------------------
		// --- CONFIG --- ROUTING ---------------------------------------------
		// --------------------------------------------------------------------
        /* For realistic movement from/ towards activities/ modes (subnetwork of correct type/ mode)
        Should be default (always in use) for multimodal networks. Distorts equil scenario. */
		//config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink ) ;

		// --------------------------------------------------------------------
		// --- CONFIG --- SCORING ---------------------------------------------
		// --------------------------------------------------------------------
		/* Averaging for convergence of scores.
		 * Averages the scores everytime a plan is used.
		 * Should be used with innovation switch-off (see REPLANNING). */
		config.scoring().setFractionOfIterationsToStartScoreMSA( 0.8 ) ;

		// --------------------------------------------------------------------
		// --- CONFIG --- QSIM ------------------------------------------------
		// --------------------------------------------------------------------
		/* DownSampling: 0.0 - 1.0 */
		final double SAMPLESIZE = 1.0 ;
		config.qsim().setFlowCapFactor( SAMPLESIZE ) ;
		config.qsim().setStorageCapFactor( SAMPLESIZE ) ;

		/* Behavior, if vehicle needed is not present?
		 * exception   ~ Simulation will break
		 * wait        ~ (Example:) for the one available, but busy, car of household / bus.
		 * teleport    ~ Do not enforce particle consistency. */
		config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.teleport ) ;

		/* How do vehicles interact?
		 * FIFO      ~ "first in, first out": vehicles leaving in the same order of entering the link
		 * PassingQ  ~ Vehicles are stuck behind each other, only if they are in a queue.
		 *             Enable vehicles passing each other.
		 *             Vehicles sorted by earliestLinkExitTime (when no congestion), but stuck in congestion together. */
		config.qsim().setLinkDynamics( QSimConfigGroup.LinkDynamics.PassingQ ) ;

		// -------------------------------------------------------------------
		// --- SCENARIO ------------------------------------------------------
		// -------------------------------------------------------------------
		/* Till here, we were building the config. */
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		/* Possibly modify scenario here (infrastructure: links, persons, plans) */

		// --------------------------------------------------------------------
		// --- CONTROLER ------------------------------------------------------
		// --------------------------------------------------------------------
		Controler controler = new Controler( scenario ) ;

		/* possibly modify controller here (how to control the program)
		 * all possible modifications subsumed under the most important ones:
		 * addOverridingModule, addOverridingQSimModule */
		//controler.addOverridingModule( new OTFVisLiveModule() ) ;
		controler.addOverridingModule( new SimWrapperModule() ) ;
		controler.run() ;

	}
}

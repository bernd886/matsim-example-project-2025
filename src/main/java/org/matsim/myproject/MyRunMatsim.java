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
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


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

		/* Plan memory size: default individual.
         * Decrease for less RAM usage. larger = better. */
		config.replanning().setMaxAgentPlanMemorySize( 5 );

		/* Plan removal. In default: Plan with the lowest score is removed, if number of plans is too large.
		 * To remember: Genetic algorithms alone do not maintain diversity is these populations; "we end up with n copies of best plan". */
		config.replanning().setPlanSelectorForRemoval( DefaultPlanStrategiesModule.DefaultPlansRemover.WorstPlanSelector.toString());

		/* Innovation switch-off.
		 * No more innovation (mutation) at the end of sim.
		 * Only selection between existing plans.
		 * Should be used with averaging scores (see SCORING). */
		config.replanning().setFractionOfIterationsToDisableInnovation( 0.8 );

		// --------------------------------------------------------------------
		// --- CONFIG --- REPLANNING --- SELECTOR -----------------------------
		// --------------------------------------------------------------------
		/* Adding new selector strategy, which is non-innovative (from lecture 4 (2022)).
		 * For illustrative example: full controll with BestScore + Random can be useful.
		 * BestScore     ~ used alone, gets stuck with suboptimal plans.
		 * ExpBeta       ~ balances exploitation + exploration.
		 * ChangeExpBeta ~ faster + robust  */
		{
			ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
			stratSets.setWeight( .9 );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.BestScore );
			config.replanning().addStrategySettings( stratSets );
		}

		// --------------------------------------------------------------------
		// --- CONFIG --- REPLANNING --- MUTATOR ------------------------------
		// --------------------------------------------------------------------
		/* Adding new mutator strategy (innovative).
		 * "Changing the location (go shopping somewhere else) is a contrib." */
		{
			ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
			stratSets.setWeight( .1 );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ReRoute );
			/* "ChangeSingleTripMode works better than ChangeTripMode" */
			config.replanning().addStrategySettings( stratSets );
		}

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

		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves ) ;
		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves ) ;

		/* Behavior, if vehicle needed is not present?
		 * exception   ~ Simulation will break
		 * wait        ~ (Example:) for the one available, but busy, car of household / bus.
		 * teleport    ~ Do not enforce particle consistency. */
		/* in "equil" "wait" lets agents get stuck waiting in sim */
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
		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
		visConfig.setDrawTime( true ) ;
		visConfig.setDrawNonMovingItems( true ) ;
		visConfig.setAgentSize( 125 ) ;
		visConfig.setLinkWidth( 10 ) ;
		visConfig.setDrawTransitFacilityIds( false ) ;
		visConfig.setDrawTransitFacilities( false ) ;

		/* Till here, we were building the config. */
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		/* Possibly modify scenario here (infrastructure: links, persons, plans) */

		// --------------------------------------------------------------------
		// --- CONTROLER ------------------------------------------------------
		// --------------------------------------------------------------------
		Controler controler = new Controler( scenario ) ;

		/* Possibly modify controller here (how to control the program).
		 * All possible modifications subsumed under the most important ones:
		 * addOverridingModule, addOverridingQSimModule.
		 * For OTFVis visualization after Java 16 add JVM arguments:
		 * --add-exports java.base/java.lang=ALL-UNNAMED
		 * --add-exports java.desktop/sun.awt=ALL-UNNAMED
		 * --add-exports java.desktop/sun.java2d=ALL-UNNAMED
		 * More Run > Modify ... > Modify Options >> Add VM Options */
		controler.addOverridingModule( new OTFVisLiveModule() ) ;
		controler.addOverridingModule( new SimWrapperModule() ) ;
		controler.run() ;

	}
}

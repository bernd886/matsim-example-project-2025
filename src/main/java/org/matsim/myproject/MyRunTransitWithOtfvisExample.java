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
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.HashSet;

public class MyRunTransitWithOtfvisExample {

    public static void main( String[] args ) {

        // --------------------------------------------------------------------
        // --- CONFIG ---------------------------------------------------------
        // --------------------------------------------------------------------

        Config config = null ;
        if ( args != null && args.length >= 1 ) {
            config = ConfigUtils.loadConfig( args[0], new OTFVisConfigGroup() ) ;
        } else {
            //final String filename = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/cottbus/cottbus-tutorial-2016/config01.xml" ;

            final URL context = IOUtils.getFileUrl( "C:\\Users\\lenovo\\IdeaProjects\\matsim-example-project-2025\\src\\main\\java\\org\\matsim\\myproject\\" ) ;
            final URL url = IOUtils.extendUrl( context, "config.xml" ) ;

            config = ConfigUtils.loadConfig( url, new OTFVisConfigGroup() ) ;
        }

        config.controller().setOutputDirectory( "output" ) ;
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) ;
        config.controller().setLastIteration( 30 ) ;

        /* possibly modify config here (first/ last iteration, learning functions, etc.) */

        /* Time interval size for which link travel times are calculated (default).
         * For sims with small scale changes (evacuation), switching calculator to HashMap useful (where?) */
        config.travelTimeCalculator().setTraveltimeBinSize( 900 ) ;

        // config.transit().setUseTransit( true ); ;

        // --------------------------------------------------------------------
        // --- CONFIG --- MODE CHOICE -----------------------------------------
        // --------------------------------------------------------------------
        /* Configuring mode choice strategies.
         * Modes for modeChoice: declaring available modes; preconfigured string constants.
         * "The mode choice modules need to know which modes are in the system.
         * There are four different places, where a different mode needs to be entered.
         * Replanning:   must be able to say: use this mode.
         * Router:       must be able to produce a route for this mode.
         * Simulation:   must be able to process it.
         * Scoring:      must be able to give it a score". */
        var modes = new HashSet<String>() ;
        final String MY_MODE = "eScooter" ;
        modes.add( TransportMode.car ) ;
        modes.add( TransportMode.bike ) ;
        modes.add( MY_MODE ) ;
        /* Conversion, because of internal inconsistencies in MATSim. */
        config.changeMode().setModes( modes.toArray( String[]::new ) ) ;
        config.subtourModeChoice().setModes( modes.toArray( String[]::new ) ) ;

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING ------------------------------------------
        // --------------------------------------------------------------------
        /* Plan innovation (or "strategy") */

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
            stratSets.setWeight( .7 );
            stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta );
            config.replanning().addStrategySettings( stratSets );
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING --- MUTATOR ------------------------------
        // --------------------------------------------------------------------
        /* Adding new mutator strategy (innovative).
         * "Changing the location (go shopping somewhere else) is a contrib." */
        {   /* Putting in a mode choice module. */
            ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
            stratSets.setWeight( .2 );
            stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode );
            /* "ChangeSingleTripMode works better than ChangeTripMode" */
            config.replanning().addStrategySettings( stratSets );
        }
        {   /* Putting in a sub mode choice module. */
            ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
            stratSets.setWeight( .1 );
            stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice );
            /* SubtourModeChoice ensure mass conservation for relevant modes (car, bike) */
            config.replanning().addStrategySettings( stratSets );
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- ROUTING ---------------------------------------------
        // --------------------------------------------------------------------
        /* For realistic movement from/ towards activities/ modes (subnetwork of correct type/ mode)
         * Should be default (always in use) for multimodal networks. Distorts equil scenario. */
        config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink );

        {
            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams( MY_MODE );
            params.setTeleportedModeSpeed( 35. / 3.6 ) ;
            config.routing().addTeleportedModeParams( params );
        }
        {
            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams( TransportMode.walk );
            params.setTeleportedModeSpeed( 3. / 3.6 ) ;
            config.routing().addTeleportedModeParams( params );
        }
        {
            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams( TransportMode.bike );
            params.setTeleportedModeSpeed( 20. / 3.6 ) ;
            config.routing().addTeleportedModeParams( params );
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- SCORING ---------------------------------------------
        // --------------------------------------------------------------------
        /* Averaging for convergence of scores.
         * Averages the scores everytime a plan is used.
         * Should be used with innovation switch-off (see REPLANNING). */
        config.scoring().setFractionOfIterationsToStartScoreMSA( 0.8 );

        {
            ScoringConfigGroup.ModeParams params = new ScoringConfigGroup.ModeParams( MY_MODE );
            params.setMarginalUtilityOfTraveling( 0. ) ;
            config.scoring().addModeParams( params );
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- QSIM ------------------------------------------------
        // --------------------------------------------------------------------
        /* downsampling: 0.0 - 1.0 */
        final double SAMPLESIZE = 1.0;
        config.qsim().setFlowCapFactor( SAMPLESIZE );
        config.qsim().setStorageCapFactor( SAMPLESIZE );

        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves ) ;
        config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves ) ;

        /* Behavior, if vehicle needed is not present?
         * exception   ~ Simulation will break
         * wait        ~ (Example:) for the one available, but busy, car of household.
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
        /* Till here, we were building the config. */

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

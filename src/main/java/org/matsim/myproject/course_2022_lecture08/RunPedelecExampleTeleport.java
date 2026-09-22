package org.matsim.myproject.course_2022_lecture08;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;


/*
MATSim Public Tutorial 14.x (2022), Lecture 08
Implementation of multimodality in simple "equil" scenario via teleportation.
Additional "pedelec" mode is NOT ON the network.
Not visible in VIA as vehicle. only represented in plans of agents.
For that the new mode has be put in QSim and router.
For teleported mode WITH routing, see comment in RunPedelecExample
*/

public class RunPedelecExampleTeleport {

    public static void main( String[] args ) {
        // --------------------------------------------------------------------
        // --- CONFIG ---------------------------------------------------------
        // --------------------------------------------------------------------
        var url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
        Config config = ConfigUtils.loadConfig( url );
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

        config.controller().setLastIteration( 1 );

        /* possibly modify config here (first/ last iteration, learning functions, etc.) */

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING --- MUTATOR ------------------------------
        // --------------------------------------------------------------------
		/* Adding new mutator strategy (innovative). */
        {
            // Putting in a mode choice module
            ReplanningConfigGroup.StrategySettings params = new ReplanningConfigGroup.StrategySettings();
            params.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode );
            params.setWeight( 1. ); // is high, to see effect of mode change
            config.replanning().addStrategySettings( params );
        }

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
        final String[] modes = { "car", "pedelec" };
        config.changeMode().setModes( modes );

        // --------------------------------------------------------------------
        // --- CONFIG --- ROUTING ---------------------------------------------
        // --------------------------------------------------------------------
        /* Execute modes on the network.
        * When implementing new modes, we first are using teleport.
        * When adding teleportation mode, pre-existing ones are removed.
        * Like constructors: overriding the default, deletes all predefined.
        * Clear for no errors. */
        config.routing().clearTeleportedModeParams();
        {
            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams( "pedelec" );
            params.setTeleportedModeSpeed( 15. / 3.6 );
            params.setBeelineDistanceFactor( 1.3 );
            // More details with freespeed travel time on network (multiplied by X)
            // When used, speed and distanceFactor have to be null
            params.setTeleportedModeFreespeedFactor( null );
            config.routing().addTeleportedModeParams( params );
        }
        {
            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams( "walk" );
            params.setTeleportedModeSpeed( 5. / 3.6 );
            params.setBeelineDistanceFactor( 1.3 );
            config.routing().addTeleportedModeParams( params );
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- SCORING ---------------------------------------------
        // --------------------------------------------------------------------
        {
            ScoringConfigGroup.ModeParams params = new ScoringConfigGroup.ModeParams( "pedelec" );
            params.setMarginalUtilityOfTraveling( 0. );
            config.scoring().addModeParams( params );
        }
        {
            ScoringConfigGroup.ModeParams params = new ScoringConfigGroup.ModeParams( "car" );
            params.setMarginalUtilityOfTraveling( 0. );
            config.scoring().addModeParams( params );
        }

        // -------------------------------------------------------------------
        // --- SCENARIO ------------------------------------------------------
        // -------------------------------------------------------------------
        /* Till here, we were building the config. */
        Scenario scenario = ScenarioUtils.loadScenario( config );

        /* Possibly modify scenario here (infrastructure: links, persons, plans) */

        // --------------------------------------------------------------------
        // --- CONTROLER ------------------------------------------------------
        // --------------------------------------------------------------------
        Controler controler = new Controler( scenario ) ;
        /* Possibly modify controller here (how to control the program). */
        controler.run();























    }
}

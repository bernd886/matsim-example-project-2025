package org.matsim.myproject.lecture08;

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
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.utils.collections.CollectionUtils;
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

        var url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
        Config config = ConfigUtils.loadConfig( url );
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

        config.controller().setLastIteration( 1 );

        // ### Plans innovation (or "strategy") ###

        {
            // Putting in a mode choice module
            ReplanningConfigGroup.StrategySettings params = new ReplanningConfigGroup.StrategySettings();
            params.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode );
            params.setWeight( 1. ); // is high, to see effect of mode change
            config.replanning().addStrategySettings( params );
        }

        // Configuring mode choice module
        final String[] modes = { "car", "pedelec" };
        config.changeMode().setModes( modes );

        // ### Routing ###

        // When implementing new modes, we are using teleport
        // When teleporting, pre-existing are removed, clear for no errors
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

        // ### Scoring ###
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

        Scenario scenario = ScenarioUtils.loadScenario( config );
        Controler controler = new Controler( scenario );
        controler.run();























    }
}

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
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;


/*
MATSim Public Tutorial 14.x (2022), Lecture 08
implementation of multimodality in simple "equil" scenario via teleportation.
additional "pedelec" mode is ON the network.
put in QSim (setMainModes), Router (setNetworkModes), Network (setAllowedModes)

*/

public class RunPedelecExample {

    public static void main( String[] args ) {

        var url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
        Config config = ConfigUtils.loadConfig( url );
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

        config.controller().setLastIteration( 1 );



        // plans innovation (or "strategy"):
        {
            // putting in a mode choice module
            ReplanningConfigGroup.StrategySettings params = new ReplanningConfigGroup.StrategySettings();
            params.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode );
            params.setWeight( 1. ); // is high to see effect of mode change
            config.replanning().addStrategySettings( params );
        }
        // configuring mode choice module
        final String[] modes = { "car", "pedelec" };
        config.changeMode().setModes( modes );

        // routing:

        // when implementing new modes, we are using teleport
        // when teleporting, pre-existing are removed, clear for no errors
        config.routing().clearTeleportedModeParams();
        /*
        {
            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams( "pedelec" );
            params.setTeleportedModeSpeed( 15. / 3.6 );
            params.setBeelineDistanceFactor( 1.3 );
            config.routing().addTeleportedModeParams( params );
        }
        */
        {
            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams( "walk" );
            params.setTeleportedModeSpeed( 5. / 3.6 );
            params.setBeelineDistanceFactor( 1.3 );
            config.routing().addTeleportedModeParams( params );
        }
        config.routing().setNetworkModes( CollectionUtils.stringArrayToSet( modes ) );

        // scoring:
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

        // qsim
        // let the "modes" be executed on the network
        config.qsim().setMainModes( CollectionUtils.stringArrayToSet( modes ) );
        // conversion because of internal inconsistencies in MATSim

        Scenario scenario = ScenarioUtils.loadScenario( config );
        // "pedelec" has to be allowed on network links
        for ( var link : scenario.getNetwork().getLinks().values() ) {
            link.setAllowedModes( CollectionUtils.stringArrayToSet( modes ) );
        }

        Controler controler = new Controler( scenario );
        controler.run();























    }
}

package org.matsim.myproject.lecture08;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
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
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;


/*
MATSim Public Tutorial 14.x (2022), Lecture 08
Implementation of multimodality in simple "equil" scenario via teleportation.
Additional "pedelec" mode is ON the network.
Put in QSim (setMainModes), Router (setNetworkModes), Network (setAllowedModes)

*/

public class RunPedelecExample {

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

        // Execute modes on the network
        config.routing().setNetworkModes( CollectionUtils.stringArrayToSet( modes ) );

        // Should be standard for multimodal networks. for realistic movement from/ towards activities/ modes
        config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink );

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

        // ### QSim ###

        // Let the "modes" be executed on the network
        // Conversion, because of internal inconsistencies in MATSim
        // To TELEPORT additional modes (on calculated routes), remove from "modes" or comment out
         config.qsim().setMainModes( CollectionUtils.stringArrayToSet( modes ) );

        // Where is the vehicle coming from?
        // When using fromVehiclesData, every vehicle must be predefined. Must be assigned to persons.
        config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

        // Enable vehicles passing each other.
        // Vehicles sorted by earliestLinkExitTime (when no congestion), but stuck in congestion together.
        config.qsim().setLinkDynamics( QSimConfigGroup.LinkDynamics.PassingQ );


        Scenario scenario = ScenarioUtils.loadScenario( config );

        // "pedelec" has to be allowed on network links
        for ( var link : scenario.getNetwork().getLinks().values() ) {
            link.setAllowedModes( CollectionUtils.stringArrayToSet( modes ) );
        }
        
        // Adding vehicles
        // Slow down "pedelec" mode.
        // Without vehicles attribute, limits are enforced by link attribute.
        // Because VehicleType is data class (like links, nodes, persons, plans),
        // one has to go via a polymorphic factory. Not constructors.
        // Creational methods for data objects are in indirect factory syntax.
        VehiclesFactory vf = scenario.getVehicles().getFactory();

        // No pre-configured VehicleType Id. Using the general one.
        {
            VehicleType type = vf.createVehicleType( Id.create( "pedelec", VehicleType.class ) );
            type.setPcuEquivalents( 0.25 );
            type.setMaximumVelocity( 15./3.6 );
            type.setNetworkMode( "pedelec" );
            scenario.getVehicles().addVehicleType( type );
        }
        {
            VehicleType type = vf.createVehicleType( Id.create( "car", VehicleType.class ) );
            type.setMaximumVelocity( 200./3.6 );
            scenario.getVehicles().addVehicleType( type );
        }

        Controler controler = new Controler( scenario );
        controler.run();























    }
}

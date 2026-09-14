package org.matsim.myproject.course_2022_lecture08;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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

import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.HashSet;



/* MATSim Public Tutorial 14.x (2022), Lecture 08
 * Implementation of multimodality in simple "equil" scenario via teleportation.
 * Additional "pedelec" mode is ON the network.
 * Put in QSim (setMainModes), Router (setNetworkModes), Network (setAllowedModes). */

public class RunPedelecExample {

    public static void main( String[] args ) {

        // --------------------------------------------------------------------
        // --- CONFIG ---------------------------------------------------------
        // --------------------------------------------------------------------
        var url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
        Config config = ConfigUtils.loadConfig( url );
        config.controller().setOutputDirectory( "./output/" ) ;
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) ;
        config.controller().setLastIteration( 20 ) ;

        // --------------------------------------------------------------------
        // --- CONFIG --- MODE CHOICE -----------------------------------------
        // --------------------------------------------------------------------
        final var MY_MODE = "myMode" ;
        final var MY_SPEED = 200 / 3.6 ;

        /* Configuring mode choice module. */
        var modes = new HashSet<String>() ;
        modes.add( MY_MODE ) ;
        modes.add( TransportMode.car ) ;
        /* Conversion, because of internal inconsistencies in MATSim. */
        config.changeMode().setModes( modes.toArray( String[]::new ) ) ;

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING ------------------------------------------
        // --------------------------------------------------------------------
        /* Plan innovation (or "strategy")
         *
         * innovation switch-off.
         * no more innovation (mutation), only selection between existing plans.
         * Should be used with averaging scores (see SCORING). */
        //config.replanning().setFractionOfIterationsToDisableInnovation( 0.8 );

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING --- MODE CHANGE --------------------------
        // --------------------------------------------------------------------
        {   /* Putting in a mode choice module. */
            ReplanningConfigGroup.StrategySettings params = new ReplanningConfigGroup.StrategySettings();
            params.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode ) ;
            params.setWeight( 1. ) ; // is high, to see effect of mode change
            config.replanning().addStrategySettings( params ) ;
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- ROUTING ---------------------------------------------
        // --------------------------------------------------------------------
        /* Execute modes on the network */
        config.routing().setNetworkModes( modes );
        /* For realistic movement from/ towards activities/ modes (subnetwork of correct type/ mode)
        Should be default (always in use) for multimodal networks. Distorts equil scenario. */
        //config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink );

        // --------------------------------------------------------------------
        // --- CONFIG --- SCORING ---------------------------------------------
        // --------------------------------------------------------------------
        //config.scoring().setFractionOfIterationsToStartScoreMSA( 0.8 );
        {
            ScoringConfigGroup.ModeParams params = new ScoringConfigGroup.ModeParams( MY_MODE );
            params.setMarginalUtilityOfTraveling( 0. ) ;
            config.scoring().addModeParams( params );
        }
        {
            ScoringConfigGroup.ModeParams params = new ScoringConfigGroup.ModeParams( TransportMode.car );
            params.setMarginalUtilityOfTraveling( 0. ) ;
            config.scoring().addModeParams( params );
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- QSIM ------------------------------------------------
        // --------------------------------------------------------------------
        /* DownSampling: 0.0 - 1.0 */
        final double SAMPLESIZE = 1.0 ;
        config.qsim().setFlowCapFactor( SAMPLESIZE ) ;
        config.qsim().setStorageCapFactor( SAMPLESIZE ) ;

        /* Let the "modes" be executed on the network.
         * To TELEPORT additional modes (on calculated routes), remove from "modes" or comment out. */
        config.qsim().setMainModes( modes ) ;

        /* Where is the vehicle coming from?
         * defaultVehicle                      ~ Same car for all.
         * fromVehiclesData                    ~ Xml file defining every vehicle. Must be assigned to persons.
         * modeVehicleTypesFromVehiclesData    ~ Xml file defining types. Vehicles are auto-generated.
         * Xml file can be generated via coding. */
        config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData ) ;

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

        /* Adding attributes to objects.
         * "myMode" has to be allowed on network links */
        for ( var link : scenario.getNetwork().getLinks().values() ) {
            link.setAllowedModes( modes );
        }

        /* Arbitrary attributes */
        for ( var link : scenario.getNetwork().getLinks().values() ) {
            link.getAttributes().putAttribute( "key", "value" ) ;
            link.getAttributes().getAttribute( "key" ) ;

            link.getAttributes().putAttribute( "surface", "cobblestone" ) ;
            /* Cast is necessary, because type not known at compile time. */
            var linkSurface = (String) link.getAttributes().getAttribute( "surface" ) ;

            link.getAttributes().putAttribute( "heightMax", 4.2 ) ;
            var linkHeightMax = (Double) link.getAttributes().getAttribute( "heightMax" );
        }

        /* Adding vehicles
         *
         * Slow down "myMode"
         * Without vehicles attribute, limits are enforced by link attribute.
         * Because VehicleType is data class (like links, nodes, persons, plans),
         * one has to go via a polymorphic factory. Not constructors.
         * Creational methods for data objects are in indirect factory syntax.
         *
         * Everytime we create objects , we put into a MATSim container,
         * we need a factory. A factory we get out of the container.
         * Upper-level containers are: vehicles, population, network, facilities, ...
         * convention: "id" of type (here: key) has to be same as "mode". */
        VehiclesFactory vf = scenario.getVehicles().getFactory();

        {   /* No pre-configured VehicleType Id. Using the general one. */
            VehicleType type = vf.createVehicleType( Id.create( MY_MODE, VehicleType.class ) );
            type.setNetworkMode( MY_MODE ) ;
            type.setMaximumVelocity( MY_SPEED ) ;
            type.setPcuEquivalents( 0.25 ) ;
            scenario.getVehicles().addVehicleType( type ) ;
        }
        {
            VehicleType type = vf.createVehicleType( Id.create( TransportMode.car, VehicleType.class ) );
            type.setNetworkMode( TransportMode.car ) ;
            type.setMaximumVelocity( 200 / 3.6 ) ;
            type.setPcuEquivalents( 1 ) ;
            scenario.getVehicles().addVehicleType( type ) ;
        }

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
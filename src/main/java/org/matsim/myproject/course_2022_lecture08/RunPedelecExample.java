package org.matsim.myproject.course_2022_lecture08;

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
import org.matsim.simwrapper.SimWrapperModule;
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

        config.controller().setLastIteration( 20 );

        // ### PLANNING innovation (or "strategy") ###
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

        // ### ROUTING ###

        // Execute modes on the network
        config.routing().setNetworkModes( CollectionUtils.stringArrayToSet( modes ) );

        // For realistic movement from/ towards activities/ modes (subnetwork of correct type/ mode)
        // Should be default (always in use) for multimodal networks.
        config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink );

        // ### SCORING ###

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

        // ### QSIM ###

        // Let the "modes" be executed on the network
        /*
        Conversion, because of internal inconsistencies in MATSim
        To TELEPORT additional modes (on calculated routes), remove from "modes" or comment out
        */
        config.qsim().setMainModes( CollectionUtils.stringArrayToSet( modes ) );

        // Where is the vehicle coming from?
        /*
        defaultVehicle ~ same car for all
        fromVehiclesData ~ Xml file defining every vehicle. Must be assigned to persons.
        modeVehicleTypesFromVehiclesData ~ Xml file defining types. Vehicles are auto-generated
        xml file can be generated via coding
        */
        config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

        // How do vehicles interact?
        /*
        * FIFO      ~ "first in, first out": vehicles leaving in the same order of entering the link
        * PassingQ  ~ vehicles are stuck behind each other, only if they are in a queue.
        *             Enable vehicles passing each other.
        *             Vehicles sorted by earliestLinkExitTime (when no congestion), but stuck in congestion together.
        * */
        config.qsim().setLinkDynamics( QSimConfigGroup.LinkDynamics.PassingQ );

        // Behavior, if vehicle needed is not present
        /*
        // exception ~ simulation will break
        // wait ~ (example:) for the one available but busy car of household
        // teleport ~ do not enforce particle consistency
        */
        config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.teleport );

        // till here, we were building the config
        Scenario scenario = ScenarioUtils.loadScenario( config );

        // Adding attributes to objects
        {
            // "pedelec" has to be allowed on network links
            for ( var link : scenario.getNetwork().getLinks().values() ) {
                link.setAllowedModes( CollectionUtils.stringArrayToSet( modes ) );
            }
        }
        {
            // arbitrary attributes
            for ( var link : scenario.getNetwork().getLinks().values() ) {
                link.getAttributes().putAttribute( "key", "value" ) ;
                link.getAttributes().getAttribute( "key" ) ;

                link.getAttributes().putAttribute( "surface", "cobblestone" ) ;
                // cast is necessary because type not known at compile time
                var linkSurface = (String) link.getAttributes().getAttribute( "surface" ) ;

                link.getAttributes().putAttribute( "heightMax", 4.2 ) ;
                var linkHeightMax = (Double) link.getAttributes().getAttribute( "heightMax" );
            }
        }

        // Adding vehicles
        /*
         Slow down "pedelec" mode.

         Without vehicles attribute, limits are enforced by link attribute.
         Because VehicleType is data class (like links, nodes, persons, plans),
         one has to go via a polymorphic factory. Not constructors.
         Creational methods for data objects are in indirect factory syntax.

         Everytime we create objects , we put into a MATSim container,
         we need a factory. A factory we get out of the container.
         Upper-level container are: vehicles, population, network, facilities, ...
         convention: "id" of type (here: key) has to be same as "mode"
        */
        VehiclesFactory vf = scenario.getVehicles().getFactory() ;
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
        controler.addOverridingModule ( new SimWrapperModule() );
        controler.run();

    }
}

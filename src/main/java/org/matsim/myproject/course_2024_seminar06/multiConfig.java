package org.matsim.myproject.course_2024_seminar06;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.HashSet;

public class multiConfig {

    public static void main( String[] args ) {
        // --------------------------------------------------------------------
        // --- CONFIG ---------------------------------------------------------
        // --------------------------------------------------------------------

        Config config = ConfigUtils.loadConfig( "scenarios/equil/multi_config.xml" ) ;

        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) ;

        config.controller().setLastIteration( 3 ) ;

        // --------------------------------------------------------------------
        // --- CONFIG --- MODE CHOICE -----------------------------------------
        // --------------------------------------------------------------------

        /* Ensure, that only car in networked modes;
         * not teleported bike. */
        var modes = new HashSet<String>() ;
        modes.add( TransportMode.car ) ;
        modes.add( TransportMode.bike ) ;
        config.routing().setNetworkModes( modes );

        var subModes = new HashSet<String>() ;
        subModes.add( TransportMode.car ) ;
        subModes.add( TransportMode.bike ) ;
        config.subtourModeChoice().setModes( subModes.toArray( String[]::new ) ) ;

        // --------------------------------------------------------------------
        // --- CONFIG --- ROUTING ---------------------------------------------
        // --------------------------------------------------------------------

//        {
//            RoutingConfigGroup.TeleportedModeParams params = new RoutingConfigGroup.TeleportedModeParams() ;
//            params.setMode( TransportMode.bike ) ;
//            params.setTeleportedModeSpeed( 1.39 ) ;
//            params.setBeelineDistanceFactor( 1.1 ) ;
//            params.setTeleportedModeFreespeedFactor( null ) ;
//            //params.setTeleportedModeFreespeedFactor( 3. ) ;
//            //params.setBeelineDistanceFactor( null ) ;
//            //params.setTeleportedModeSpeed( null ) ;
//            config.routing().addTeleportedModeParams( params ) ;
//        }

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING ------------------------------------------
        // --------------------------------------------------------------------

        //config.replanning().setFractionOfIterationsToDisableInnovation( 0.8 );

        {   /* Putting in a sub mode choice module.
            * SubtourModeChoice ensure mass conservation for relevant modes (car, bike).
            * This strategy has a default setting for pt. To avoid errors in non-pt sims,
            * edit config.subtourModeChoice(). */
            ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings() ;
            stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice ) ;
            stratSets.setWeight( 0.2 ) ;
            config.replanning().addStrategySettings( stratSets );
        }

        // --------------------------------------------------------------------
        // --- CONFIG --- QSIM ------------------------------------------------
        // --------------------------------------------------------------------

        config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData ) ;
        config.qsim().setMainModes( modes );

        // --------------------------------------------------------------------
        // --- CONFIG --- SCORING ---------------------------------------------
        // --------------------------------------------------------------------

        //config.scoring().setFractionOfIterationsToStartScoreMSA( 0.8 );

        // -------------------------------------------------------------------
        // --- SCENARIO ------------------------------------------------------
        // -------------------------------------------------------------------

        Scenario scenario = ScenarioUtils.loadScenario( config ) ;

        var vf = VehicleUtils.getFactory() ;
        {
            var type = vf.createVehicleType( Id.create( TransportMode.bike, VehicleType.class ) );
            type.setNetworkMode( TransportMode.bike );
            type.setMaximumVelocity( 15. / 3.6 );
            type.setPcuEquivalents( 0.25 );
            scenario.getVehicles().addVehicleType( type ) ;
        }
        {
            var type = vf.createVehicleType( Id.create( TransportMode.car, VehicleType.class ) );
            type.setNetworkMode( TransportMode.car ) ;
            type.setMaximumVelocity( 200. / 3.6 ) ;
            type.setPcuEquivalents( 1. ) ;
            scenario.getVehicles().addVehicleType( type ) ;
        }

        for ( var link : scenario.getNetwork().getLinks().values() ) {
            link.setAllowedModes( modes ) ;
        }

        // --------------------------------------------------------------------
        // --- CONTROLER ------------------------------------------------------
        // --------------------------------------------------------------------

        Controler controler = new Controler(  scenario ) ;

        controler.run() ;
    }
}

/* Statistics for runs.
* run_setting,      bike,   car
* tele,             9,      42
* teleMSA,          0,      75
* freeSpeed,        29,     40
* freeSpeedMSA,     16,     84
* teleRoute,        17,     28
* teleRouteMSA,     11,     66
* teleRouteVeh,     7,      36
* teleRouteVehMSA,  0,      75
* qsim,             8,      36
* qsimMSA,          1,      75
 */
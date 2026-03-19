package org.matsim.myproject.lecture09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunDrtExample {
    // todo:
    // * have at least one drt use case in the "examples" project, so it can be addressed via ExamplesUtils
    // * remove the DrtRoute.class thing; use Attributable instead (Route will have to be made implement Attributable).  If impossible, move the DrtRoute
    // class thing to the core.
    // * move consistency checkers into the corresponding config groups.
    // * make MultiModeDrt and normal DRT the same.  Make config accordingly so that 1-mode drt is just multi-mode with one entry.

    private static final Logger log = LogManager.getLogger( RunDrtExample.class );
    private static final String DRT_A = "drt_A";
    private static final String DRT_B = "drt_B";
    private static final String DRT_C = "drt_C";

    public static void main( String... args ) {
        run(true, args);
    }

    public static void run(boolean otfvis, String... args ){
        Config config;
        if ( args!=null && args.length>=1 ) {
            config = ConfigUtils.loadConfig( args );
        } else {
            // config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "dvrp-grid" ), "multi_mode_one_shared_taxi_config.xml" ) );
            // the above is there, but is totally different.  --> consolidate.  kai, jan'23

            config = ConfigUtils.loadConfig( "./scenarios/multi_mode_one_shared_taxi/multi_mode_one_shared_taxi_config.xml" );
            config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
        }

        config.controller().setLastIteration( 5 );

        config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true ); // necessary
        // KinWaves "best setting"
        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );
        config.qsim().setSnapshotStyle( SnapshotStyle.kinematicWaves );

        @SuppressWarnings("unused")
        // Base config
        DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
        dvrpConfig.getTravelTimeMatrixParams().addParameterSet( new SquareGridZoneSystemParams() );
        // (config group needs to be "materialized")

        // Definition of different taxi companies
        MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        // Clear all xml data, start over and construct again
        // multiModeDrtCfg.getModalElements().clear();

        // MODIFY: modes
        {
            // Has multiple elements (config for each mode)
            // "stopDuration" ~ boarding time
            // condition for rejection
            // "maxWaitTime" ~ until taxi arrives for boarding
            // "maxTravelTimeAlpha" ~ (%) how much longer trip is allowed to take, compared to a direct trip
            // "maxTravelTimeBeta" ~ (s) how much longer trip is allowed to take, compared to a direct trip
//            for ( DrtConfigGroup modalConfig : multiModeDrtCfg.getModalElements() ) {
//                modalConfig.
//            }
        }

        {
            DrtConfigGroup drtConfig = new DrtConfigGroup();
            drtConfig.setMode( DRT_A );
            drtConfig.setStopDuration(60.);
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxWaitTime", String.valueOf( 900 ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxTravelTimeAlpha", String.valueOf( 1.3 ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxTravelTimeBeta", String.valueOf( 10. * 60. ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "rejectRequestIfMaxWaitOrTravelTimeViolated", String.valueOf(false) );
            drtConfig.setVehiclesFile("one_shared_taxi_vehicles_A.xml");
            drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
            drtConfig.setDrtInsertionSearchParams( new ExtensiveInsertionSearchParams() );
            multiModeDrtCfg.addParameterSet(drtConfig);
        }
        {
            DrtConfigGroup drtConfig = new DrtConfigGroup();
            drtConfig.setMode(DRT_B);
            drtConfig.setStopDuration(60.);
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxWaitTime", String.valueOf( 900 ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxTravelTimeAlpha", String.valueOf( 1.3 ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxTravelTimeBeta", String.valueOf( 10. * 60. ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "rejectRequestIfMaxWaitOrTravelTimeViolated", String.valueOf(false) );
            drtConfig.setVehiclesFile("one_shared_taxi_vehicles_B.xml");
            drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
            drtConfig.setDrtInsertionSearchParams( new ExtensiveInsertionSearchParams() );
            multiModeDrtCfg.addParameterSet(drtConfig);
        }
        {
            DrtConfigGroup drtConfig = new DrtConfigGroup();
            drtConfig.setMode(DRT_C);
            drtConfig.setStopDuration(60.);
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxWaitTime", String.valueOf( 900 ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxTravelTimeAlpha", String.valueOf( 1.3 ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "maxTravelTimeBeta", String.valueOf( 10. * 60. ) );
            drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam( "rejectRequestIfMaxWaitOrTravelTimeViolated", String.valueOf(false) );
            drtConfig.setVehiclesFile("one_shared_taxi_vehicles_C.xml");
            drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
            drtConfig.setDrtInsertionSearchParams( new ExtensiveInsertionSearchParams() );
            multiModeDrtCfg.addParameterSet(drtConfig);
        }

        for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
            DrtConfigs.adjustDrtConfig(drtCfg, config.scoring(), config.routing());
        }
        {
            // add params so that scoring works:
            config.scoring().addModeParams( new ModeParams( DRT_A ) );
            config.scoring().addModeParams( new ModeParams( DRT_B ) );
            config.scoring().addModeParams( new ModeParams( DRT_C ) );
        }
        {
            // clear strategy settings from config file:
            config.replanning().clearStrategySettings();

            // configure mode innovation so that travellers start using drt:
            config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultStrategy.ChangeSingleTripMode ).setWeight( 0.1 ) );
            config.changeMode().setModes( new String[]{TransportMode.car, DRT_A, DRT_B, DRT_C} );

            // have a "normal" plans choice strategy:
            config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 1. ) );
        }

        // MODIFY scoring to obtain a mode choice reaction
        {
//            ModeParams params = config.scoring().getModes().get( DRT_A );
//            params.setConstant( -100 );
        }

        // === Scenario
        Scenario scenario = ScenarioUtils.createScenario( config );
        // Since injector lives at controler level, one has to announces, that able to handle drt routing
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );
        ScenarioUtils.loadScenario( scenario );
        // yyyy in long run, try to get rid of the route factory thing

        // MODIFY scenario
        {
//            for ( Person person : scenario.getPopulation().getPersons().values() ) {
//                // let everybody want to depart at same time
//                Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get( 0 );
//                firstActivity.setEndTime( 0 );
//                // let everyone switch to the same mode. so there is only one taxi.
//                Leg firstLeg = (Leg) person.getSelectedPlan().getPlanElements().get( 1 );
//                firstLeg.setMode( DRT_A );
//            }
        }


        // === Controler
        Controler controler = new Controler( scenario ) ;

        controler.addOverridingModule( new DvrpModule() ) ;
        controler.addOverridingModule( new MultiModeDrtModule( ) ) ;

        controler.configureQSimComponents( DvrpQSimComponents.activateModes( DRT_A, DRT_B, DRT_C ) ) ;
        // yyyy in long run, try to get rid of the above line

        if (otfvis) {
            OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
            otfVisConfigGroup.setLinkWidth(5);
            otfVisConfigGroup.setDrawNonMovingItems(true);
            // controler.addOverridingModule(new OTFVisLiveModule());
        }

        controler.run() ;
    }

}
package org.matsim.myproject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
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
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.HashSet;

/* MATSim Public Tutorial 14.x (2022), Lecture 08
* Implementation of multimodality in simple "equil" scenario via teleportation.
* Additional "pedelec" mode is ON the network.
* Put in QSim (setMainModes), Router (setNetworkModes), Network (setAllowedModes). */

public class MyCleanTestTun {

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

        config.controller().setOutputDirectory( "./output/" ) ;
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) ;
        config.controller().setLastIteration( 1 ) ;

        /* possibly modify config here (first/ last iteration, learning functions, etc.) */

        /* Time interval size for which link travel times are calculated (default).
        * For sims with small scale changes (evacuation), switching calculator to HashMap useful (where?) */
        config.travelTimeCalculator().setTraveltimeBinSize( 900 ) ;

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
        modes.add( TransportMode.car ) ;
        modes.add( TransportMode.bike ) ;
        modes.add( "eScooter" ) ;
        config.changeMode().setModes( modes.toArray( String[]::new ) ) ;

        var subModes = new HashSet<String>() ;
        config.subtourModeChoice().setModes( subModes.toArray( String[]::new ) ) ;

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING ------------------------------------------
        // --------------------------------------------------------------------
        /* Plan innovation (or "strategy") */

        /* Plan memory size: default individual.
        * Decrease for less RAM usage. larger = better. */
        config.replanning().setMaxAgentPlanMemorySize( 5 );

        /* Plan removal. In default: plan with the lowest score is removed, if number of plans is too large.
        * But genetic algorithms do not maintaining diversity; "we end up with n copies of best plan". */
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
        {
            ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
            stratSets.setWeight( .2 );
            stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode );
            /* "ChangeSingleTripMode works better than ChangeTripMode" */
            config.replanning().addStrategySettings( stratSets );
        }
        {
            ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
            stratSets.setWeight( .1 );
            stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice );
            // SubtourModeChoice ensure mass conservation for relevant modes (car, bike)
            config.replanning().addStrategySettings( stratSets );
        }




        // --------------------------------------------------------------------
        // --- CONFIG --- ROUTING ---------------------------------------------
        // --------------------------------------------------------------------
        /* For realistic movement from/ towards activities/ modes (subnetwork of correct type/ mode)
        Should be default (always in use) for multimodal networks. */
        config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink );

        // --------------------------------------------------------------------
        // --- CONFIG --- SCORING ---------------------------------------------
        // --------------------------------------------------------------------
        /* Averaging for convergence of scores.
        * Averages the scores everytime a plan is used.
        * Should be used with innovation switch-off (see REPLANNING). */
        config.scoring().setFractionOfIterationsToStartScoreMSA( 0.8 );

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
        config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.wait ) ;

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

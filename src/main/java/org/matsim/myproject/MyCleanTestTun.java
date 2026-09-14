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
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

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
        Config config;
        if ( args==null || args.length==0 || args[0]==null ){
            config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );
        } else {
            config = ConfigUtils.loadConfig( args );
        }

        config.controller().setOutputDirectory( "./output/" ) ;
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) ;
        config.controller().setLastIteration( 1 ) ;

        /* possibly modify config here (first/ last iteration, learning functions, etc.) */

        // --------------------------------------------------------------------
        // --- CONFIG --- REPLANNING ------------------------------------------
        // --------------------------------------------------------------------
        /* Plan innovation (or "strategy")

        * innovation switch-off.
        * no more innovation (mutation), only selection between existing plans.
        * Should be used with averaging scores (see SCORING). */
        config.replanning().setFractionOfIterationsToDisableInnovation( 0.8 );

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

        /* Behavior, if vehicle needed is not present?
        * exception   ~ Simulation will break
        * wait        ~ (Example:) for the one available, but busy, car of household.
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

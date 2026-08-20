package org.matsim.myproject.course_2022_seminar07;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class SimpleNetworkChange {

    // MATSim Public Tutorial 14.x (2022), Seminar 7
    // task:  alter a network
    // java class reading and writing out network,
    // providing method, to alter network. Usable outside in general run script via:
    // SimpleNetworkChange.changeNetwork( scenario.getNetwork() );

    public static void main(String[] args) {

        var network = NetworkUtils.readNetwork( "C:\\Users\\lenovo\\IdeaProjects\\matsim-example-project-2025\\src\\main\\java\\org\\matsim\\myproject\\network.xml" );

        changeNetwork( network );

        NetworkUtils.writeNetwork( network, "C:\\Users\\lenovo\\IdeaProjects\\matsim-example-project-2025\\src\\main\\java\\org\\matsim\\myproject\\network_edit.xml" );

    }

    public static void changeNetwork( Network network ) {
        for ( Link link : network.getLinks().values() ) {

            if ( link.getAllowedModes().contains( TransportMode.car ) ) {
                link.setFreespeed( 10 );
            }
        }
    }
}

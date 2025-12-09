package org.matsim.myproject.analysis;

import org.apache.commons.csv.CSVFormat;
import org.matsim.core.events.EventsUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// creating handler and manager
// reading events files
// let handlers handel events
// writing out data

public class SimpleAnalysis {

    private static final String eventsFile = "C:\\Users\\lenovo\\IdeaProjects\\matsim-serengeti-park-hodenhagen-2025\\scenarios\\serengeti-park-v1.0\\output\\output-serengeti-park-v1.0-run1\\serengeti-park-v1.0-run1.output_events.xml.gz" ;
    private static final String outFile = "C:\\Users\\lenovo\\IdeaProjects\\matsim-serengeti-park-hodenhagen-2025\\scenarios\\serengeti-park-v1.0\\output\\output-serengeti-park-v1.0-run1\\serengeti-park-v1.0.run1.output_link_count.csv" ;


    public static void main(String[] args) throws IOException {



        var handler = new SimplePersonEventHandler();
        var linkHandler = new SimpleLinkLeaveEventHandler();
        // using constructor method from matsimUtils
        // manager manages the EventsHandlers, pushes the events into the EventsHandlers
        var manager = EventsUtils.createEventsManager();
        manager.addHandler( handler );
        manager.addHandler( linkHandler );

        EventsUtils.readEvents( manager, eventsFile );

        var linkLeaveTimesArray = linkHandler.getLinkLeaveTimesArray();

        var linkLeaveTimesMap = linkHandler.getLinkLeaveTimesMap();

        // writing out array
//        try ( var writer = Files.newBufferedWriter( Paths.get(outFile)); var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("Time").print(writer)) {
//            for (Double time : linkLeaveTimesArray) {
//                writer.write(time + "\n");  // Each number on a new line
//            }
//            System.out.println("File written successfully: " + outFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // writing out map
        try (var writer = Files.newBufferedWriter(Paths.get(outFile)); var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("Hour", "Value").print(writer)) {
            for (var volume : linkLeaveTimesMap.entrySet()) {
                printer.printRecord(volume.getKey(), volume.getValue());
                printer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
      }
}

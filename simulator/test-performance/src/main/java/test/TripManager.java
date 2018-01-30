package test;

import model.TripEntry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages current trips in use
 */
public class TripManager {
    private ConcurrentHashMap<String, Boolean> tripMap = new ConcurrentHashMap<>();

    public TripManager() {
        initTripManager();
    }

    /**
     * Creates a Map with the entries: [id,taken]
     * taken ... the file is already in use
     * id ... a unique id of a trip (i.e. 1049)
     */
    private void initTripManager() {
        File tripFolder = new File("trips/");
        File[] listOfFiles = tripFolder.listFiles();
        for (File file : listOfFiles) {
            tripMap.put(FilenameUtils.getBaseName(file.getName()), false);
        }
    }

    /**
     * Returns the filename of a new and unused trip that is randomly chosen
     * @return A filename of the form (<tripID>.csv)
     */
    public String getNewUnusedTripId() {


        for (int i=0; i<tripMap.keySet().size(); i++) {
            Random random = new Random();
            List<String> keys      = new ArrayList<String>(tripMap.keySet());
            String randomKey = keys.get(random.nextInt(keys.size()));

            boolean taken = tripMap.get(randomKey);
            if (!taken) {
                tripMap.replace(randomKey, true);
                return randomKey;
            }
        }

        return null; // only if no more free Trips are available
    }

    /**
     * Mark a trip as non taken, i.e. it can be reused again.
     * @param id
     */
    public void releaseTripId(String id) {
        tripMap.replace(id, false);
    }

    /**
     * Reads a track containing timestamp, lat/lon
     * @param filename The full filename
     */
    public List<TripEntry> readTripFromFile(String filename) {
        List<TripEntry> trips = new ArrayList<>();
        File csvFile = new File("trips/" + filename);
        try {
            CSVParser csvParser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader("ts", "lat", "lon"));
            for (CSVRecord csvRecord : csvParser) {
                if (csvRecord.getRecordNumber() == 1)
                    continue; // skip Header
                long ts = Long.parseLong(csvRecord.get("ts"));
                double lat = Double.parseDouble(csvRecord.get("lat"));
                double lon = Double.parseDouble(csvRecord.get("lon"));
                TripEntry trackEntry = new TripEntry(ts, lat, lon);
                trips.add(trackEntry);
            }
        } catch (IOException e) {
            Log.error("Reading trip from file failed" + e.toString());
            return null;
        }
        return trips;
    }




}

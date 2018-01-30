package car;

import at.mkaran.thesis.common.CurveDTO;
import at.mkaran.thesis.common.CurveRecommendationDTO;
import at.mkaran.thesis.common.CurveRecommendationListDTO;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import model.CachedCurve;
import model.Location;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Singleton that holds the connection to a local MongoDB instance used for caching curves on the client.
 * The local cache is bounded in size, i.e. if new items are added to the database, when its full, old items are removed.
 * This is done using CappedCollections (@see <a href="http://mongodb.github.io/mongo-java-driver/3.5/driver/tutorials/databases-collections/">MongoDB Java Driver Doc</a>)
 */
public class LocalMongoCurveCache {

    private static final String MONGO_DB_NAME = "curveStorage";
    private static final String MONGO_COLLECTION_NAME = "curves";

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> dbCollection;


    public LocalMongoCurveCache() {
        initConnection();
    }


    /**
     * Initialize a connection to the local DB.
     * In case it doesn't exist yet, a new capped collection to store curves is created.
     */
    private void initConnection() {
        // connect to local mongodb instance
        mongoClient = new MongoClient("localhost");
        database = mongoClient.getDatabase(MONGO_DB_NAME);
        initCollection();
    }

    private void initCollection() {
        dbCollection = database.getCollection(MONGO_COLLECTION_NAME);
        dbCollection.createIndex(Indexes.geo2dsphere("centerPoint"));
    }

    public void closeConnection() {
        mongoClient.close();
    }

    public void removeCurve(CachedCurve cachedCurve) {
        dbCollection.deleteOne(new Document("_id", cachedCurve.getId()));
    }

    public void removeAll() {
        dbCollection.deleteMany(new Document());
    }

    public void insertMany(CurveRecommendationListDTO curveRecommendationListDTO) {
        List<Document> documents = new ArrayList<>();
        for (CurveRecommendationDTO curveRecommendationDTO : curveRecommendationListDTO.getCurveRecommondationsList()) {
            documents.add(curveRecToDocument(curveRecommendationDTO));
        }
        if (documents.size() > 0) {
            insertMany(documents);
        }
    }

    private void insertMany(List<Document> curveDocuments) {
        dbCollection.insertMany(curveDocuments);
    }

    public CachedCurve findNearestCurveResult(double lat, double lon, Double maxDistance, Double minDistance) {
        try {
            List<Document> documents = dbCollection.find(
                    Filters.near("centerPoint", new Point(new Position(lon, lat)), null, null)
            ).into(new ArrayList<>());
            if (documents.size() > 0) {
                return documentToCurveResult(documents.get(0));
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public List<CachedCurve> findAll() {
        List<CachedCurve> curves = new ArrayList<>();
        List<Document> documents = dbCollection.find().into(new ArrayList<Document>());
        for (Document doc : documents) {
            curves.add(documentToCurveResult(doc));
        }
        return curves;
    }

    private CachedCurve documentToCurveResult(Document document) {
        Document startPoint = document.get("startPoint", Document.class);
        List<Double> startCoords = (List<Double>) startPoint.get("coordinates");

        Document centerPoint = document.get("centerPoint", Document.class);
        List<Double> centerCoords = (List<Double>) centerPoint.get("coordinates");

        Document endPoint = document.get("endPoint", Document.class);
        List<Double> endCoords = (List<Double>) endPoint.get("coordinates");

        int recommendedSpeed = document.getInteger("recommendedSpeed");

        CachedCurve cachedCurve = new CachedCurve(
                document.getObjectId("_id"),
                new Location(centerCoords.get(0), centerCoords.get(1)),
                new Location(endCoords.get(0), endCoords.get(1)),
                new Location(startCoords.get(0), startCoords.get(1)),
                document.getDouble("radius"),
                document.getInteger("recommendedSpeed"),
                startPoint.getDouble("bearing"),
                endPoint.getDouble("bearing")
        );

        return cachedCurve;

    }

    /**
     * Converts a DTO object containing curve data to Mongo DB document object
     * @param dto DTO containing curve data
     * @return
     */
    private Document curveToDocument(CurveDTO dto) {
        Document doc = new Document(
                "radius", dto.getRadius())
                .append("length", dto.getLength())
                .append("type", dto.getTurntype())
                .append("centerPoint", new Document("type", "Point").append("coordinates", Arrays.asList(dto.getCenter().getLon(), dto.getCenter().getLat())))
                .append("startPoint", new Document("type", "Point").append("bearing", dto.getStartBearing()).append("coordinates", Arrays.asList(dto.getStart().getLon(), dto.getStart().getLat())))
                .append("endPoint", new Document("type", "Point").append("bearing", dto.getEndBearing()).append("coordinates", Arrays.asList(dto.getEnd().getLon(), dto.getEnd().getLat())));
        return doc;
    }

    /**
     * Converts a DTO object containing curve and recommendation data to Mongo DB document object
     * @param dto DTO containing curve data
     * @return
     */
    private Document curveRecToDocument(CurveRecommendationDTO dto) {
        Document document = curveToDocument(dto.getCurve());
        document.append("recommendedSpeed", dto.getRecommendedSpeed());
        return document;
    }


}

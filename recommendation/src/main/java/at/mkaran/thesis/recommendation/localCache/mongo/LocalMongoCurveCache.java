package at.mkaran.thesis.recommendation.localCache.mongo;

import at.mkaran.thesis.commons.mongodb.IMongoConnection;
import at.mkaran.thesis.commons.mongodb.IMongoDAO;
import at.mkaran.thesis.commons.mongodb.MongoDAO;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import org.bson.Document;

/**
 * Singleton that holds the connection to a local MongoDB instance used for caching curves.
 * Using a local cache reduces connections to the distributed cache (@see {@link at.mkaran.thesis.commons.mongodb.CurveStorage}).
 * The local cache is bounded in size, i.e. if new items are added to the database, when its full, old items are removed.
 * This is done using CappedCollections (@see <a href="http://mongodb.github.io/mongo-java-driver/3.5/driver/tutorials/databases-collections/">MongoDB Java Driver Doc</a>)
 */
public class LocalMongoCurveCache implements IMongoConnection {

    private static final String MONGO_DB_NAME = "curveCache";
    private static final String MONGO_COLLECTION_NAME = "cappedCachedCurves";
    private static final long CAPPED_COLLECTION_SIZE = 100 * 0x100000; // 100MB

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> dbCollection;
    private MongoDAO dao;


    private LocalMongoCurveCache() {
    }

    private static class LocalCurveCacheManagerHolder {
        private final static LocalMongoCurveCache instance = new LocalMongoCurveCache();
    }

    /**
     * Get the instance
     * @return Returns the singleton
     */
    public static LocalMongoCurveCache getInstance() {
        return LocalCurveCacheManagerHolder.instance;
    }


    /**
     * Initialize a connection to the local DB.
     * In case it doesn't exist yet, a new capped collection to store curves is created.
     * @param nodeId
     */
    public void initConnection(String nodeId) {
        // connect to local mongodb instance
        if (nodeId != null && !nodeId.isEmpty()) {
            mongoClient = new MongoClient("mongo-"+nodeId);
        } else {
            mongoClient = new MongoClient("mongo");
        }
        database = mongoClient.getDatabase(MONGO_DB_NAME);

        boolean collectionExists = false;
        for (String name : database.listCollectionNames()) {
            if (name.equals(MONGO_COLLECTION_NAME)) {
                dbCollection = database.getCollection(name);
                collectionExists = true;
                break;
            }
        }

        if (!collectionExists) {
            database.createCollection(MONGO_COLLECTION_NAME,
                    new CreateCollectionOptions().capped(true).sizeInBytes(CAPPED_COLLECTION_SIZE));
        }

        dbCollection = database.getCollection(MONGO_COLLECTION_NAME);
        this.dao = new MongoDAO(dbCollection);
    }

    @Override
    public IMongoDAO getDAO() {
        return this.dao;
    }


}

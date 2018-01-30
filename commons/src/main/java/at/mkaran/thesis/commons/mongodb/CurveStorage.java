package at.mkaran.thesis.commons.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

/**
 * Singleton that holds the connection to the distributed MongoDB.
 * The MongoDB Java driver manages connections to the DB itself using a pool.
 * The default maximum of connections per host is 100. (This might be too much considering AtlasDB only allows 100 simultanious connections)
 * http://api.mongodb.com/java/current/com/mongodb/MongoClientOptions.html#getConnectionsPerHost()
 */
public class CurveStorage implements IMongoConnection{

    private static final String MONGO_DB_NAME = "curveStorage";
    private static final String MONGO_CONNECTION_URI_FORMAT = "mongodb://%s:%s@%s/" + MONGO_DB_NAME; // username:password@host/database
    private static final String MONGO_ATLAS_CONNECTION_URI_FORMAT = "mongodb+srv://%s:%s@%s";

    private static final String MONGO_COLLECTION_NAME = "curves";

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> dbCollection;
    private MongoDAO dao;

    private CurveStorage() {
    }

    private static class CurveStorageManagerHolder {
        private final static CurveStorage instance = new CurveStorage();
    }

    /**
     * Get the instance
     * @return Returns the singleton
     */
    public static CurveStorage getInstance() {
        return CurveStorageManagerHolder.instance;
    }


    /**
     * Initialize a connection to a remote MongoDB instance
     * @param mongoUser the username
     * @param mongoPW   password for the instance
     * @param mongoURI  the complete URI to connect to the instance
     */
    public void initConnection(String mongoUser, String mongoPW, String mongoURI, boolean atlas) {
        String connectionString;
        if (atlas) {
            connectionString = String.format(MONGO_ATLAS_CONNECTION_URI_FORMAT, mongoUser, mongoPW, mongoURI);
        } else {
            connectionString = String.format(MONGO_CONNECTION_URI_FORMAT, mongoUser, mongoPW, mongoURI);
        }
        MongoClientURI uri = new MongoClientURI(connectionString);
        mongoClient = new MongoClient(uri);
        database = mongoClient.getDatabase(MONGO_DB_NAME);
        initCollection();
        if (!atlas) {
            initIndexes();
        }
        dao = new MongoDAO(dbCollection);
    }

    /**
     * For testing purposes init a Database without authentication
     * @param host The host where MongoDB is running
     */
    public void initConnectionNoAuth(String host) {
        // connect to local mongodb instance
        mongoClient = new MongoClient(host);
        database = mongoClient.getDatabase(MONGO_DB_NAME);
        initCollection();
        initIndexes();
        dao = new MongoDAO(dbCollection);
    }

    /**
     * Inits Collection and sets an Index to centerPoint (needed to perform geospatial queries)
     */
    private void initCollection() {
        dbCollection = database.getCollection(MONGO_COLLECTION_NAME);
    }

    private void initIndexes() {
        dbCollection.createIndex(Indexes.geo2dsphere("centerPoint"));
        dbCollection.createIndex(Indexes.text("geohash"));
    }

    public void closeConnection() {
        mongoClient.close();
    }

    public void removeAll() {
        dbCollection.drop();
        initCollection();
    }


    @Override
    public IMongoDAO getDAO() {
        return this.dao;
    }



}

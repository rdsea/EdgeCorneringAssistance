package at.mkaran.thesis.operator;

import at.mkaran.thesis.commons.mongodb.CurveStorage;
import at.mkaran.thesis.curvedetection.SimpleCurve;
import com.datatorrent.api.Context;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.common.util.BaseOperator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Operator to write detected curves into MongoDB.
 * Curves are aggregated given by the specified window count (default = 10 windows) and are then written in one batch.
 *
 * @since 0.9.0
 */
public class MongoDBOutputOperator extends BaseOperator {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBOutputOperator.class);


    private transient CurveStorage curveStorage;
    private List<Document> dataList = new ArrayList<>();
    private String mongoURI;
    private String mongoPW;
    private String mongoUser;
    private boolean appendGeohashesToCurves;
    private boolean useAtlasDb;
    private boolean enableAuth;

    public MongoDBOutputOperator() {
    }

    /**
     * Take every incoming curve tuple and write to a temporary list
     */
    public final transient DefaultInputPort<SimpleCurve> inputPort = new DefaultInputPort<SimpleCurve>() {
        @Override
        public void process(SimpleCurve tuple) {
            Document document = tuple.toDoc();
            if (appendGeohashesToCurves) {
                document = tuple.appendGeoHash(document);
            }
            dataList.add(document);
        }
    };

    @Override
    public void setup(Context.OperatorContext context) {
        super.setup(context);
        curveStorage = CurveStorage.getInstance();
        if (enableAuth) {
            curveStorage.initConnection(mongoUser, mongoPW, mongoURI, useAtlasDb);
        } else {
            curveStorage.initConnectionNoAuth(mongoURI);
        }
        LOG.info("Initialized mongo connection to " + mongoURI);
    }


    @Override
    public void beginWindow(long windowId) {
        // nothing
    }

    @Override
    public void endWindow() {
        // on each window end - write all curves from the temporary list to MongoDB
        //MyLogger.info(this, " cached data tuples: " + dataList.size());
        if (dataList.size() > 0) {
            LOG.info(" writing " + dataList.size() + " curves to MongoDB");
            curveStorage.getDAO().insertMany(dataList);
            LOG.info("writing to DB done.");
            dataList.clear();
        }
    }

    @Override
    public void teardown() {
        super.teardown();
    }

    public void setAppendGeohashesToCurves(boolean appendGeohashesToCurves) {
        this.appendGeohashesToCurves = appendGeohashesToCurves;
    }

    public void setMongoURI(String mongoURI) {
        this.mongoURI = mongoURI;
    }

    public void setMongoPW(String mongoPW) {
        this.mongoPW = mongoPW;
    }

    public void setMongoUser(String mongoUser) {
        this.mongoUser = mongoUser;
    }

    public void setUseAtlasDb(boolean useAtlasDb) {
        this.useAtlasDb = useAtlasDb;
    }

    public void setEnableAuth(boolean enableAuth) {
        this.enableAuth = enableAuth;
    }
}

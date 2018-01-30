package at.mkaran.thesis.commons.mongodb;

import at.mkaran.thesis.common.CurveDTO;
import at.mkaran.thesis.common.CurveListDTO;
import at.mkaran.thesis.common.PointDTO;
import ch.hsr.geohash.BoundingBox;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DAO Implementation to store and retrieve curves from a MongoDB collection
 */
public class MongoDAO implements IMongoDAO {

    private static final double KM_DIVIDE_FACTOR = 111.12; // @See https://stackoverflow.com/a/7841830/2350644

    private MongoCollection<Document> dbCollection;

    public MongoDAO(MongoCollection<Document> dbCollection) {
        this.dbCollection = dbCollection;
    }


    @Override
    /**
     * @inheritDoc
     */
    public CurveListDTO findCurves(BoundingBox bb) {
        List<Document> documents = dbCollection.find(
                Filters.geoWithinBox("centerPoint", bb.getMinLon(), bb.getMinLat(), bb.getMaxLon(), bb.getMaxLat())
        ).into(new ArrayList<Document>());
        return buildResult(documents);
    }

    @Override
    /**
     * @inheritDoc
     */
    public CurveListDTO findCurves(double lat, double lon, int radiusInMeters) {
        double kilometers = radiusInMeters/1000;
        double radius = kilometers / KM_DIVIDE_FACTOR;
        List<Document> documents = dbCollection.find(
                Filters.geoWithinCenter("centerPoint", lon, lat, radius)
        ).into(new ArrayList<Document>());
        return buildResult(documents);
    }

    @Override
    /**
     * @inheritDoc
     */
    public CurveListDTO findCurves(String geoHash) {
        List<Document> documents = dbCollection.find(
                Filters.eq("geohash", geoHash)
        ).into(new ArrayList<Document>());
        return buildResult(documents);
    }

    /**
     * Builds a DTO object from the give documents
     * @param documents Collection of MongoDB documents
     * @return DTO
     */
    private CurveListDTO buildResult(List<Document> documents) {
        CurveListDTO.Builder builder = CurveListDTO.newBuilder();
        if (documents.size() > 0) {
            for (Document document : documents) {
                CurveDTO curveDTO = documentToCurve(document);
                if (curveDTO != null) {
                    builder.addCurves(curveDTO);
                }
            }
            return builder.build();
        } else {
            return null;
        }
    }

    @Override
    /**
     * @inheritDoc
     */
    public void insertMany(List<Document> curveDocuments) {
        dbCollection.insertMany(curveDocuments);
    }

    @Override
    /**
     * @inheritDoc
     */
    public void insertMany(CurveListDTO curveListDTO) {
        List<Document> documents = new ArrayList<>();
        for (CurveDTO curveDTO : curveListDTO.getCurvesList()) {
            documents.add(curveToDocument(curveDTO));
        }
        if (documents.size() > 0) {
            insertMany(documents);
        }
    }

    @Override
    public void insertMany(CurveListDTO curveListDTO, String geohash) {
        List<Document> documents = new ArrayList<>();
        for (CurveDTO curveDTO : curveListDTO.getCurvesList()) {
            documents.add(curveToDocument(curveDTO).append("geohash", geohash));
        }
        if (documents.size() > 0) {
            insertMany(documents);
        }
    }

    /**
     * Converts a Mongo Document to a CurveDTO
     * @param document A MongoDB Document
     * @return
     */
    private CurveDTO documentToCurve(Document document) {
        CurveDTO.Builder curveBuilder = CurveDTO.newBuilder();

        Document startPoint = document.get("startPoint", Document.class);
        List<Double> startCoords = (List<Double>) startPoint.get("coordinates");

        Document centerPoint = document.get("centerPoint", Document.class);
        List<Double> centerCoords = (List<Double>) centerPoint.get("coordinates");

        Document endPoint = document.get("endPoint", Document.class);
        List<Double> endCoords = (List<Double>) endPoint.get("coordinates");


        curveBuilder
                .setStart(PointDTO.newBuilder()
                        .setLat(startCoords.get(1))
                        .setLon(startCoords.get(0))
                        .build())
                .setEnd(PointDTO.newBuilder()
                        .setLat(endCoords.get(1))
                        .setLon(endCoords.get(0))
                        .build())
                .setCenter(PointDTO.newBuilder()
                        .setLat(endCoords.get(1))
                        .setLon(endCoords.get(0))
                        .build())
                .setStartBearing(startPoint.getDouble("bearing"))
                .setEndBearing(endPoint.getDouble("bearing"))
                .setTurntype(document.getString("type"))
                .setLength(document.getDouble("length"))
                .setRadius(document.getDouble("radius"));
        return curveBuilder.build();

    }

    /**
     * @inheritDoc
     */
    @Override
    public Document curveToDocument(CurveDTO dto) {
        Document doc = new Document(
                "radius", dto.getRadius())
                .append("length", dto.getLength())
                .append("type", dto.getTurntype())
                .append("centerPoint", new Document("type", "Point").append("coordinates", Arrays.asList(dto.getCenter().getLon(), dto.getCenter().getLat())))
                .append("startPoint", new Document("type", "Point").append("bearing", dto.getStartBearing()).append("coordinates", Arrays.asList(dto.getStart().getLon(), dto.getStart().getLat())))
                .append("endPoint", new Document("type", "Point").append("bearing", dto.getEndBearing()).append("coordinates", Arrays.asList(dto.getEnd().getLon(), dto.getEnd().getLat())));
        return doc;
    }

    @Override
    public List<CurveDTO> findAll() {
        List<CurveDTO> list = new ArrayList<CurveDTO>();
        List<Document> documents = dbCollection.find().into(new ArrayList<Document>());
        for (Document doc : documents) {
            list.add(documentToCurve(doc));
        }
        return list;
    }


}

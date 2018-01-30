package at.mkaran.thesis.commons.mongodb;

import at.mkaran.thesis.common.CurveDTO;
import at.mkaran.thesis.common.CurveListDTO;
import ch.hsr.geohash.BoundingBox;
import org.bson.Document;

import java.util.List;

/**
 * Interface for MongoDAO
 */
public interface IMongoDAO {
    /**
     * Finds all curves in the "curves" collection that lie within the given BoundingBox.
     * @param bb The BoundingBox to search for curves
     * @return curves within specified bounding box or <code>NULL</code> if none found
     */
    CurveListDTO findCurves(BoundingBox bb);

    /**
     * Finds all curves that lie within the given radius of the circle having the specified coordinates as center point.
     * @param lat Latitude of circle's center
     * @param lon Longitude of circle's center
     * @param radiusInMeters radius of the circle
     * @return
     */
    CurveListDTO findCurves(double lat, double lon, int radiusInMeters);

    /**
     * Finds all curves that have the given geohash;
     * @param geoHash A geohash
     * @return
     */
    CurveListDTO findCurves(String geoHash);


    /**
     * Insert curve documents to the "curves" collection
     * @param curveDocuments The curves to insert (converted to Documents)
     */
    void insertMany(List<Document> curveDocuments);

    /**
     * Insert curve dto's to the "curves" collection
     * @param curveListDTO DTO containing curves to insert
     */
    void insertMany(CurveListDTO curveListDTO);

    /**
     * Insert curve dto's to the "curves" collection and append a geohash
     * @param curveListDTO DTO containing curves to insert
     */
    void insertMany(CurveListDTO curveListDTO, String geohash);

    /**
     * Converts a DTO object containing curve data to Mongo DB document object
     * @param dto DTO containing curve data
     */
    Document curveToDocument(CurveDTO dto);

    List<CurveDTO> findAll();
}

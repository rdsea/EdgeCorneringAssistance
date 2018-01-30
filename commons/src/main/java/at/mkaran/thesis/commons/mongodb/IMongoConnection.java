package at.mkaran.thesis.commons.mongodb;

/**
 * Created by matthias on 18.11.17.
 */
public interface IMongoConnection {
    /**
     * Return DAO to do operations on database
     * @return The DAO
     */
    IMongoDAO getDAO();
}

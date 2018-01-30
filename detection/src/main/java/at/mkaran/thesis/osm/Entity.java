package at.mkaran.thesis.osm;

import org.json.simple.JSONObject;

/**
 * Created by matthias on 30.05.17.
 */
public abstract class Entity {
    protected long id;

    public Entity() {
    }

    public Entity(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public abstract void parseEntity(JSONObject json);
}

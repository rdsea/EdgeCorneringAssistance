package at.mkaran.thesis.recommendation.model;

/**
 * Created by matthias on 10.09.17.
 */
public class WeatherCondition {
    private String weatherName;
    private String weatherDesc;
    private float rain3h;
    private float temperature;
    private long timestamp;

    public WeatherCondition(String weatherName, String weatherDesc, float rain3h, float temperature, long timestamp) {
        this.weatherName = weatherName.toLowerCase();
        this.weatherDesc = weatherDesc.toLowerCase();
        this.rain3h = rain3h;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getWeatherName() {
        return weatherName;
    }

    public void setWeatherName(String weatherName) {
        this.weatherName = weatherName;
    }

    public String getWeatherDesc() {
        return weatherDesc;
    }

    public void setWeatherDesc(String weatherDesc) {
        this.weatherDesc = weatherDesc;
    }

    public float getRain3h() {
        return rain3h;
    }

    public void setRain3h(float rain3h) {
        this.rain3h = rain3h;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
}

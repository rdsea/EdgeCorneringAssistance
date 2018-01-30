package at.mkaran.thesis.recommendation;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by matthias on 08.09.17.
 */
public class OWMTest {

    @Test
    public void fetchCurrentWeatherTest() throws Exception {
        OpenWeatherMap owm = new OpenWeatherMap("<YOUR-OWM-KEY");
        CurrentWeather weather = owm.currentWeatherByCoordinates((float) 27.766718, (float) -82.8521871);
        Assert.assertNotNull(weather);
        System.out.println(weather.getRawResponse());
    }



}
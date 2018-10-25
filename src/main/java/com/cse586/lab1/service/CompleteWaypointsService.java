package com.cse586.lab1.service;

import com.cse586.lab1.model.Waypoint;
import com.cse586.lab1.repository.WapypointRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Blob;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;

@Service
public class CompleteWaypointsService {
    @Autowired
    private Environment env;
    @Autowired
    private WapypointRepository waypointRepository;

    private static final Logger LOG = LoggerFactory.getLogger(CompleteWaypointsService.class);
    private static final String WEATHER_API = "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s";
    private static final String GOOGLE_API = "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s";

    /**
     * fetch complete waypoint data
     * @param source
     * @param destination
     * @return
     */
    public String getData(String source, String destination) {
        if(source != null && !source.isEmpty() && destination != null && !destination.isEmpty()) {
            LOG.info("Fetching complete waypoint data");
            JSONObject dataObj = getDirectionsAndWeather(source, destination);
            LOG.info("Retrieved complete waypoint data");
            LOG.debug("Data retrieved is :" + dataObj.toJSONString());
            return dataObj.toJSONString();
        } else {
            LOG.error("Invalid input provided");
            return "";
        }
    }

    /**
     * request direction and weather data and also check if data is persisted previously
     * return persisted data if found else fetch new data
     * @param start
     * @param end
     * @return
     */
    private JSONObject getDirectionsAndWeather(String start, String end) {
        JSONObject dataObj = new JSONObject();
        JSONArray cities = new JSONArray();
        Date date = new Date();
        LOG.info("Check for cached data");
        String cachedMetadata = getCachedData(start,end,date);
        if (cachedMetadata != null && !cachedMetadata.isEmpty()) {
            LOG.info("Found cached data");
            JSONParser parser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(cachedMetadata);
                return jsonObject;
            } catch (ParseException e) {
                LOG.error("Error retrieving/parsing cached data" + e.getStackTrace());
            }
        } else {
            JSONArray routes = getDirections(start,end);
            if(routes != null && !routes.isEmpty()) {
                Long startTime = System.currentTimeMillis();
                Iterator iterator1 = routes.iterator();
                while (iterator1.hasNext()) {
                    JSONArray legs = (JSONArray) ((JSONObject) iterator1.next()).get("legs");
                    Iterator iterator2 = legs.iterator();
                    while (iterator2.hasNext()) {
                        JSONArray steps = (JSONArray) ((JSONObject) iterator2.next()).get("steps");
                        for (int i = 2; i < steps.size(); i = i + 3) {
                            JSONObject city = new JSONObject();
                            JSONObject coordObj = (JSONObject) ((JSONObject) steps.get(i)).get("start_location");
                            city.put("lat", coordObj.get("lat"));
                            city.put("lng", coordObj.get("lng"));
                            getWeather(city);
                            cities.add(city);
                        }
                        getSourceDestCitiesData(start, end, cities, steps);
                    }
                }
                Long endTime = System.currentTimeMillis();
                LOG.info("Weather data for all intermediate cities retrieved in(ms): " + (endTime-startTime));
                dataObj.put("directions", routes);
                dataObj.put("cities", cities);
                updateCachedData(dataObj,start,end);
            } else {
                LOG.error("No route found, hence pointing map to center");
            }
        }
        return dataObj;
    }

    /**
     * retrieve data from database if valid (requested within same day) else get new data
     * @param start
     * @param end
     * @param date
     * @return
     */
    private String getCachedData(String start, String end, Date date) {
        LOG.info("Checking if data is cached for " + start + "-" + end);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Calendar calendarNew = Calendar.getInstance();
        Waypoint route;
        String keyToFind1 = start + "-" + end;
        String keyToFind2 = end + "-" + start;
        boolean isDataCached1 = waypointRepository.existsById(keyToFind1);
        boolean isDataCached2 = waypointRepository.existsById(keyToFind2);
        if(isDataCached1 || isDataCached2) {
            Long startTime = System.currentTimeMillis();
            if(isDataCached1)
                route = waypointRepository.findById(keyToFind1).get();
            else
                route = waypointRepository.findById(keyToFind2).get();
            Long endTime = System.currentTimeMillis();
            Date cachedDate = route.getDate();
            calendarNew.setTime(cachedDate);
            if(calendar.get(Calendar.DAY_OF_MONTH) == calendarNew.get(Calendar.DAY_OF_MONTH)) {
                LOG.info("Found data cached, hence no fresh collection required");
                LOG.info("Cached data retrieved in(ms): " + (endTime-startTime));
                return route.getMetadata();
            } else {
                LOG.info("Route data cached is old and non-relevant, hence need to fetch current data for the day");
            }
        }
        return null;
    }


    /**
     * update the cached data for being invalid or for new request
     * @param dataObj
     * @param start
     * @param end
     */
    private void updateCachedData(JSONObject dataObj, String start, String end) {
        LOG.info("Updating/Persisting data to be cached for " + start + "-" + end);
        Waypoint route = new Waypoint();
        route.setDate(new java.sql.Date(System.currentTimeMillis()));
        route.setRoute(start + "-" + end);
        route.setMetadata(dataObj.toJSONString());
        LOG.debug("Data to be cached: \n" + route.getRoute() + "\n" + route.getDate() + "\n" + route.getMetadata());
        waypointRepository.save(route);
        LOG.info("Persisting data complete");
    }

    private void getSourceDestCitiesData(String start, String end, JSONArray cities, JSONArray steps) {
        JSONObject city = new JSONObject();
        JSONObject coordObj = (JSONObject) ((JSONObject)steps.get(0)).get("start_location");
        city.put("lat",coordObj.get("lat"));
        city.put("lng",coordObj.get("lng"));
        getWeather(city);
        city.put("name", start);
        cities.add(city);

        coordObj = (JSONObject) ((JSONObject)steps.get(steps.size()-1)).get("end_location");
        city.put("lat",coordObj.get("lat"));
        city.put("lng",coordObj.get("lng"));
        getWeather(city);
        city.put("name",end);
        cities.add(city);
    }

    /**
     * hit the google maps direction API, and fetch route data
     * @param start
     * @param end
     * @return
     */
    private JSONArray getDirections(String start, String end) {
        LOG.info("Fetching direction data from " + start + " to " + end);
        String api = String.format(GOOGLE_API,start.replaceAll(" ","+"),end.replaceAll(" ","+"),env.getProperty("google.key"));
        JSONArray routesObj = null;
        Long startTime = System.currentTimeMillis();
        JSONObject jsonObject = getDataFromEndpoint(api);
        Long endTime = System.currentTimeMillis();
        if(jsonObject != null && !jsonObject.isEmpty()) {
            routesObj = (JSONArray) jsonObject.get("routes");
            LOG.debug("Direction data: " + routesObj.toJSONString());
            LOG.info("Direction data retrieved in(ms): " + (endTime-startTime));
        } else {
            LOG.error("No direction data found/retrieved");
        }
        return routesObj;
    }

    /**
     * hit the Open weather API and fetch weather data for each intermediate city
     * @param city
     */
    private void getWeather(JSONObject city) {
        LOG.info("Fetching weather data");
        String api = String.format(WEATHER_API,city.get("lat"),city.get("lng"),env.getProperty("weather.key"));
        JSONObject jsonObject = getDataFromEndpoint(api);
        if(jsonObject != null && !jsonObject.isEmpty()) {
            JSONArray weatherObj = (JSONArray) jsonObject.get("weather");
            city.put("weather", ((JSONObject) weatherObj.get(0)).get("main"));
            city.put("icon", ((JSONObject) weatherObj.get(0)).get("icon"));
            JSONObject sysObj = (JSONObject) jsonObject.get("sys");
            city.put("name", jsonObject.get("name") + ", " + sysObj.get("country"));
            LOG.info("Retrieved weather data from " + city.get("name"));
            LOG.debug("Weather data: " + city.toJSONString());
        } else {
            LOG.error("No weather data found/retrieved");
        }
    }

    /**
     * common utility to make outbound connection and get response
     * @param api
     * @return
     */
    private JSONObject getDataFromEndpoint(String api) {
        LOG.info("Getting data from endpoint");
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection;
        URL url;
        JSONObject jsonObject = null;
        try {
            url = new URL(api);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            inputStream = httpURLConnection.getInputStream();
            JSONParser jsonParser = new JSONParser();
            jsonObject = (JSONObject)jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
        } catch (IOException | ParseException e) {
            LOG.error("Error retrieving data from endpoint", e.getStackTrace());
        } finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.error("Error in closing stream");
                }
            }
        }
        LOG.debug("Data from endpoint: " + jsonObject.toJSONString());
        return jsonObject;
    }
}

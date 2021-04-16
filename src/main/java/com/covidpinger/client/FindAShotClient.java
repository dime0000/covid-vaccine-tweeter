package com.covidpinger.client;


import com.covidpinger.models.TweetBuilder;
import com.covidpinger.utility.DistanceUtility;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class FindAShotClient {

    private final RestTemplate restTemplate;

    private final DistanceUtility distanceUtility;

    @Value("${urls.findashot}")
    private String findAShotUrl;

    @Value("${distance.max.miles}")
    private double myMaxMiles;

    @Autowired
    public FindAShotClient(RestTemplate restTemplate, DistanceUtility distanceUtility) {
        this.restTemplate = restTemplate;
        this.distanceUtility = distanceUtility;
    }

    public List<Element> pullOpenAppointments() throws RestClientException {
        String source = restTemplate.getForObject(findAShotUrl,
                String.class);
        List<Element> successAppointments = new ArrayList<>();

        if (source != null) {
            Document doc = Jsoup.parse(source);
            Elements appointments = doc.select(".findashot-location");
            appointments.forEach(appt -> {
                        if (!appt.select(".has-text-success").isEmpty()) {
                            successAppointments.add(appt);
                        }
                    }
            );
        }
        return successAppointments;
    }

    public List<TweetBuilder> extractAppointmentsForArea(List<Element> successAppointments) {

        List<TweetBuilder> tweets = new ArrayList<>();

        successAppointments.forEach(appt -> {
            String store = appt.select(".title").text();
            String location = appt.select(".subtitle").text();
            try {
                double locationLat = Double.parseDouble(appt.select(".relative-distance").attr("data-lat"));
                double locationLong = Double.parseDouble(appt.select(".relative-distance").attr("data-lon"));
                double distance = distanceUtility.distance(locationLat, locationLong);

                if (distance <= myMaxMiles) {
                    StringBuilder message = new StringBuilder();
                    TweetBuilder tweet = new TweetBuilder();
                    tweet.distance = distance;
                    message.append(store).append(" - ").append(location).append("\n");
                    tweet.message = message.toString();
                    tweets.add(tweet);
                }
            } catch (NumberFormatException e) {
                // do nothing
            }
        });

        return tweets;
    }
}

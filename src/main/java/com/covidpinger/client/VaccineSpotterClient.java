package com.covidpinger.client;

import com.covidpinger.models.TweetBuilder;
import com.covidpinger.models.VaccineSpotterFeatures;
import com.covidpinger.models.VaccineSpotterModel;
import com.covidpinger.utility.DistanceUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class VaccineSpotterClient {

    private final RestTemplate restTemplate;

    private final DistanceUtility distanceUtility;

    @Value("${distance.max.miles}")
    private double myMaxMiles;

    @Value("${urls.vaccinespotter}")
    private String vaccineSpotterUrl;

    @Autowired
    public VaccineSpotterClient(RestTemplate restTemplate,
                                DistanceUtility distanceUtility) {
        this.restTemplate = restTemplate;
        this.distanceUtility = distanceUtility;
    }

    public List<VaccineSpotterFeatures> pullOpenAppointments() throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");

        ResponseEntity<VaccineSpotterModel> model = restTemplate.exchange(
                vaccineSpotterUrl, HttpMethod.GET,
                new HttpEntity<>(headers), VaccineSpotterModel.class);

        List<VaccineSpotterFeatures> properties = new ArrayList<>();

        if (model.getBody() != null) {
            model.getBody().features.forEach(location -> {
                if (location.properties.appointments_available) {
                    properties.add(location);
                }
            });
        }
        return properties;
    }

    public List<TweetBuilder> extractAppointmentsForArea(List<VaccineSpotterFeatures> successAppointments) {

        List<TweetBuilder> tweets = new ArrayList<>();

        successAppointments.forEach(appt -> {
            try {

                double distance = distanceUtility.distance(appt.geometry.coordinates[1], appt.geometry.coordinates[0]);
                if (distance <= myMaxMiles && appt.properties.name != null  &&
                        appt.properties.address != null && !appt.properties.name.isEmpty() &&
                        !appt.properties.address.isEmpty()) {
                    StringBuilder message = new StringBuilder();
                    TweetBuilder tweet = new TweetBuilder();
                    tweet.distance = distance;
                    message.append(appt.properties.name).append(" - ").append(appt.properties.address).append(" ")
                            .append(appt.properties.city).append(", ").append(appt.properties.state).append(" ")
                            .append(appt.properties.postal_code).append("\n");
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

package com.example.covidpinger;

import com.google.common.base.Splitter;
import models.TweetBuilder;
import models.VaccineSpotterFeatures;
import models.VaccineSpotterModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import twitter4j.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class MyRunner implements CommandLineRunner {

    private final RestTemplate restTemplate;

    @Value("${location.lattitude}")
    private double myLat;

    @Value("${location.longitude}")
    private double myLon;

    @Value("${distance.max.miles}")
    private double myMaxMiles;

    @Value("${tweets.max.multi}")
    private int maxMultiTweet;

    @Value("${pings.findashot}")
    private boolean pingFindAShot;

    @Value("${pings.vaccinespotter}")
    private boolean pingVaccineSpotter;

    @Value("${pings.seconds.between}")
    private int secondsBetweenPings;

    @Value("${tweets.max.length}")
    private int maxTweetLength;

    @Value("${tweets.reply.to.id}")
    private long replyToTweet;

    @Value("${tweets.clean.old.tweets}")
    private boolean cleanOldTweets;

    @Value("${tweets.clean.old.tweets.count}")
    private int cleanTweetCount;

    @Value("${urls.vaccinespotter}")
    private String vaccineSpotterUrl;

    @Value("${urls.findashot}")
    private String findAShotUrl;

    @Autowired
    public MyRunner(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run(String... args) {
        try {
            while (true) {
                if (cleanOldTweets) {cleanOldTweets();}

                if (pingFindAShot) {tweetFindAShotAppointments();}
                if (pingVaccineSpotter) {tweetVaccineSpotterAppointments();}
                Thread.sleep(secondsBetweenPings * 1000);
            }

        } catch (InterruptedException e) {
            try {
                createTweet("VAX APP CRASHED");
            } catch (TwitterException t) {
                e.printStackTrace();
            }
        }
    }

    private void tweetVaccineSpotterAppointments() {
        try {
            List<VaccineSpotterFeatures> successAppointments = pullOpenAppointmentsFromVaccineSpotter();

            List<TweetBuilder> tweets = extractVaccineSpotterAppointmentsForArea(successAppointments);
            Collections.sort(tweets);

            sendVaccineAppointments(buildMessageBody(tweets));

        } catch (RestClientException e) {
            e.printStackTrace();
        }
    }

    private void tweetFindAShotAppointments() {
        try {
            List<Element> successAppointments = pullOpenAppointmentsFromFindAShot();

            List<TweetBuilder> tweets = extractFindAShotAppointmentsForArea(successAppointments);
            Collections.sort(tweets);

            sendVaccineAppointments(buildMessageBody(tweets));
        } catch (RestClientException e) {
            e.printStackTrace();
        }
    }

    private StringBuilder buildMessageBody(List<TweetBuilder> tweets) {
        StringBuilder message = new StringBuilder();
        tweets.forEach(tweet -> message.append(tweet.message));
        return message;
    }

    private List<VaccineSpotterFeatures> pullOpenAppointmentsFromVaccineSpotter() throws RestClientException {
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

    private List<Element> pullOpenAppointmentsFromFindAShot() throws RestClientException {
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

    private List<TweetBuilder> extractVaccineSpotterAppointmentsForArea(List<VaccineSpotterFeatures> successAppointments) {

        List<TweetBuilder> tweets = new ArrayList<>();

        successAppointments.forEach(appt -> {
            try {

                double distance = distance(appt.geometry.coordinates[1], appt.geometry.coordinates[0]);
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

    private List<TweetBuilder> extractFindAShotAppointmentsForArea(List<Element> successAppointments) {

        List<TweetBuilder> tweets = new ArrayList<>();

        successAppointments.forEach(appt -> {
            String store = appt.select(".title").text();
            String location = appt.select(".subtitle").text();
            try {
                double locationLat = Double.parseDouble(appt.select(".relative-distance").attr("data-lat"));
                double locationLong = Double.parseDouble(appt.select(".relative-distance").attr("data-lon"));
                double distance = distance(locationLat, locationLong);

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

    private void sendVaccineAppointments(StringBuilder vaccineAppointments) {
        if (vaccineAppointments.length() > 0) {
            try {

                createTweet("VAX Appt: " + vaccineAppointments);
            } catch (TwitterException e) {
                e.printStackTrace();
            }

        }
    }

    private void createTweet(String tweet) throws TwitterException {
        Twitter twitter = TwitterFactory.getSingleton();

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "M-dd-yyyy HH:mm:ss");

        String tweetDate = ": " + dateFormat.format(new Date());


        if ((tweet.length() + tweetDate.length()) > maxTweetLength) {
            int tweets = 0;
            for(final String token : Splitter.fixedLength(maxTweetLength - tweetDate.length()).split(tweet)){
                StatusUpdate statusUpdate = new StatusUpdate(token + tweetDate);
                statusUpdate.inReplyToStatusId(this.replyToTweet);


                twitter.updateStatus(statusUpdate);
                tweets ++;
                if (tweets == maxMultiTweet) {
                    break;
                }
            }
        } else {
            StatusUpdate statusUpdate = new StatusUpdate(tweet + tweetDate);
            statusUpdate.inReplyToStatusId(this.replyToTweet);
            twitter.updateStatus(statusUpdate);
        }
    }

    private void cleanOldTweets() {
        try {
            Twitter twitter = TwitterFactory.getSingleton();
            Date hourAgo =  new Date(System.currentTimeMillis() - 3600 * 1000);

            Paging page = new Paging();

            int pageNumber = 1;
            page.setPage(pageNumber);
            page.setCount(cleanTweetCount);

            ResponseList<Status> tweets = twitter.getUserTimeline(page);
            while (tweets.size() > 0) {
                tweets.forEach(it -> {
                    if ((it.getCreatedAt().before(hourAgo) ) && (it.getText().startsWith("VAX Appt: "))) {
                        try {
                           twitter.destroyStatus(it.getId());
                        }
                        catch (TwitterException e) {
                            e.printStackTrace();
                        }
                    }
                 });
                pageNumber ++;
                page.setPage(pageNumber);
                tweets = twitter.getUserTimeline(page);
            }

        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    private double distance(double lat2, double lon2) {

        double theta = myLon - lon2;
        double dist = Math.sin(deg2rad(myLat)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(myLat))
                * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}

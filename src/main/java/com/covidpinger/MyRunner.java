package com.covidpinger;

import com.covidpinger.client.FindAShotClient;
import com.covidpinger.client.TwitterClient;
import com.covidpinger.client.VaccineSpotterClient;
import com.covidpinger.models.TweetBuilder;
import com.covidpinger.models.VaccineSpotterFeatures;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import twitter4j.TwitterException;

import java.util.Collections;
import java.util.List;

@Component
public class MyRunner implements CommandLineRunner {

    private final VaccineSpotterClient vaccineSpotterClient;

    private final FindAShotClient findAShotClient;

    private final TwitterClient twitterClient;

    @Value("${pings.findashot}")
    private boolean pingFindAShot;

    @Value("${pings.vaccinespotter}")
    private boolean pingVaccineSpotter;

    @Value("${pings.seconds.between}")
    private int secondsBetweenPings;


    @Value("${tweets.clean.old.tweets}")
    private boolean cleanOldTweets;



    @Autowired
    public MyRunner(VaccineSpotterClient vaccineSpotterClient,
                    FindAShotClient findAShotClient,
                    TwitterClient twitterClient) {
        this.vaccineSpotterClient = vaccineSpotterClient;
        this.findAShotClient = findAShotClient;
        this.twitterClient = twitterClient;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run(String... args) {
        try {
            while (true) {
                if (cleanOldTweets) {twitterClient.cleanOldTweets();}

                if (pingFindAShot) {tweetFindAShotAppointments();}
                if (pingVaccineSpotter) {tweetVaccineSpotterAppointments();}
                Thread.sleep(secondsBetweenPings * 1000);
            }

        } catch (InterruptedException e) {
            try {
                twitterClient.createTweet("VAX APP CRASHED");
            } catch (TwitterException t) {
                e.printStackTrace();
            }
        }
    }

    private void tweetVaccineSpotterAppointments() {
        try {
            List<VaccineSpotterFeatures> successAppointments = vaccineSpotterClient.pullOpenAppointments();

            List<TweetBuilder> tweets = vaccineSpotterClient.extractAppointmentsForArea(successAppointments);
            Collections.sort(tweets);

            twitterClient.tweetAppointments(tweets);
        } catch (RestClientException e) {
            e.printStackTrace();
        }
    }

    private void tweetFindAShotAppointments() {
        try {
            List<Element> successAppointments = findAShotClient.pullOpenAppointments();

            List<TweetBuilder> tweets = findAShotClient.extractAppointmentsForArea(successAppointments);
            Collections.sort(tweets);

            twitterClient.tweetAppointments(tweets);
        } catch (RestClientException e) {
            e.printStackTrace();
        }
    }

}

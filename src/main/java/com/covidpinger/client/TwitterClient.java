package com.covidpinger.client;

import com.covidpinger.models.TweetBuilder;
import com.google.common.base.Splitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import twitter4j.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class TwitterClient {

    @Value("${tweets.clean.old.tweets.count}")
    private int cleanTweetCount;

    @Value("${tweets.max.multi}")
    private int maxMultiTweet;

    @Value("${tweets.max.length}")
    private int maxTweetLength;

    @Value("${tweets.reply.to.id}")
    private long replyToTweet;

    public void cleanOldTweets() {
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

    public void tweetAppointments(List<TweetBuilder> tweets) {
        StringBuilder vaccineAppointments = buildMessageBody(tweets);
        if (vaccineAppointments.length() > 0) {
            try {
                createTweet("VAX Appt: " + vaccineAppointments);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }
    }

    public void createTweet(String tweet) throws TwitterException {
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

    private StringBuilder buildMessageBody(List<TweetBuilder> tweets) {
        StringBuilder message = new StringBuilder();
        tweets.forEach(tweet -> message.append(tweet.message));
        return message;
    }
}

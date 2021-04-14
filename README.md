# covid-vaccine-tweeter

## Description
Has can use vaccinespotter.org and/or findashot.org to find COVID-19 vaccines within a certain vicinity. It tweets the locations (closest first) as a reply to a Twitter message at configurable intervals.

## Configuration
1. Acquire Twitter Developer API access, and enter your credentials as they prescibe in a twitter4j.properties file under the resources directory.
2. application.properties has the following configurable items:
#### location.lattitude
Lattitude for your starting location
#### location.longitude
Longitude for your starting location
#### distance.max.miles
The max radius around your starting location to search
#### tweets.max.multi
If your tweet goes too long, this script has potential to break messages up in to multiple tweets. This defines the max amount of tweets.
#### tweets.max.length
The max length of each tweet (Twitter maxes out at 280)
#### tweets.reply.to.id
The twitter ID to which this script will reply to
#### pings.findashot
Configures whether or not it will use findashot.org as a source
#### pings.vaccinehunter
Configures whether or not it will use vaccinespotter.org as a source
#### pings.seconds.between
The amount of seconds it waits before pinging your sources again
#### tweets.clean.old.tweets
Configures whether or not it will clean out tweets sent by this script that are older than an hour
#### tweets.clean.old.tweets.count
This is related to the above configuration. Difficult to explain, but recommend just keeping in at 200

## Current running setup
This can run anywhere that supports JAVA - however, I'm currently running this on a Raspberry Pi Zero! As such, you'll notice that the source compatibility in build.gradle is set to JAVA 8. 
For a Pi Zero, you'll probably want to run a light OS - I'm using Raspberry Pi OS Lite. Use the openjdk-8-jre-zero apt package to run JAVA 8 on the Pi Zero.


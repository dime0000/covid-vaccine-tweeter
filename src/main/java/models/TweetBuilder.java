package models;

public class TweetBuilder implements Comparable<TweetBuilder> {
    public double distance;
    public String message;

    @Override
    public int compareTo(TweetBuilder o) {
        if(this.distance < o.distance)
            return -1;
        else if(o.distance < this.distance)
            return 1;
        return 0;
    }
}

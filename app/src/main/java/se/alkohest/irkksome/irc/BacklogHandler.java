package se.alkohest.irkksome.irc;

import java.util.Date;

/**
 * Created by oed on 8/2/14.
 */
public interface BacklogHandler {

    public Date extractDate(String[] parts);

    public String getBacklogRequest(long unixTime);
}

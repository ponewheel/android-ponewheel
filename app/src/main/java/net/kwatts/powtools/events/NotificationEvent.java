package net.kwatts.powtools.events;

/**
 * Created by kwatts on 7/1/16.
 */
public class NotificationEvent {

    public final String title;
    public final String message;

    public NotificationEvent(String title, String message) {
        this.title = title;
        this.message = message;
    }

}

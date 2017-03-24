package net.kwatts.powtools.events;

/**
 * Created by kwatts on 4/28/16.
 */
public class VibrateEvent {
    public final int length;
    public final int count;

    public VibrateEvent(int length, int count) {
        this.length = length;
        this.count = count;
    }
}

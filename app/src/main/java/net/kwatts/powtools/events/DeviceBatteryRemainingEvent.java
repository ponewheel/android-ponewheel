package net.kwatts.powtools.events;

/**
 * Created by kwatts on 5/30/16.
 */
public class DeviceBatteryRemainingEvent {
    public final int percentage;

    public DeviceBatteryRemainingEvent(int percentage) {
        this.percentage = percentage;
    }
}

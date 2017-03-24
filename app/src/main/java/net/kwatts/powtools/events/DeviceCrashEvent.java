package net.kwatts.powtools.events;

/**
 * Created by kwatts on 6/17/16.
 */
public class DeviceCrashEvent {
    public final boolean state;

    public DeviceCrashEvent(boolean state) {
        this.state = state;
    }
}

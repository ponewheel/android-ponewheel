package net.kwatts.powtools.util;

import net.kwatts.powtools.App;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

import timber.log.Timber;


public class BatteryMods {
    private static int owPercent = 0;
    private static double nominalVoltage = 0;
    private static double avgVolts = 0;
    private static double avgCells = 0.0;
    private static double cellCount = 0.0;
    private static boolean changes = true;

    private static int remainingByVoltage() {
        double nominalVolts=nominalVoltage * cellCount;
        double remaining;

        remaining=100.0/(1.0+Math.pow(4, nominalVolts-avgVolts))+1;

        Timber.d( "remainingByVoltage:" + remaining);

        return((int)remaining);
    }

    private static int remainingFromCells() {
        double nominalVolts=nominalVoltage * cellCount;
        double remaining;

        remaining=100.0/(1.0+Math.pow(5, nominalVolts-avgCells))+1;

        Timber.d( "remainingFromCells:" + remaining);

        return((int)remaining);
    }

    private static int remainingForTwoX() {
        double remaining;

        if (owPercent>5) {
            remaining = owPercent/2+50;
        } else {
            remaining = remainingFromCells();
        }

        Timber.d( "remainingForTwoX:" + remaining);

        return((int)remaining);
    }

    public static void setHardware(int hver) {
        if(hver<4000)
            nominalVoltage=3.2;
        else
            nominalVoltage=3.7;
    }

    public static void setRemaining(int level) {
        if(owPercent != level) {
            owPercent=level;
            changes=true;
        }
    }

    public static void setVoltage(double volts) {
        if (avgVolts>0) {
            avgVolts = avgVolts*0.9 + volts*0.1;
        } else {
            avgVolts = volts;
        }

	   if (App.INSTANCE.getSharedPreferences().isRemainVolts()) {
            changes=true;
        }
    }

    public static void setCells(double volts, int count) {
        cellCount = count;

        if (avgCells>0) {
            avgCells = avgCells*0.9 + volts*0.1;
        } else {
            avgCells = volts;
        }

	   if (App.INSTANCE.getSharedPreferences().isRemainCells()) {
            changes=true;
        }
    }

    public static void updateBatteryModsRemaining(MainActivity mainActivity) {
        if (changes) {
            if (App.INSTANCE.getSharedPreferences().isRemainDefault()) {
                mainActivity.updateBatteryRemaining(owPercent);
            } else if (App.INSTANCE.getSharedPreferences().isRemainVolts()) {
                mainActivity.updateBatteryRemaining(remainingByVoltage());
            } else if (App.INSTANCE.getSharedPreferences().isRemainCells()) {
                mainActivity.updateBatteryRemaining(remainingFromCells());
            } else if (App.INSTANCE.getSharedPreferences().isRemainTwoX()) {
                mainActivity.updateBatteryRemaining(remainingForTwoX());
            }

            changes = false;
        }
    }

}


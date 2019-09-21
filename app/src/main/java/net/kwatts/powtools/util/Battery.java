package net.kwatts.powtools.util;

import net.kwatts.powtools.App;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

import timber.log.Timber;


public class Battery {
    // Tunable Variables (use these to tweak the output)
    private static final double LiFePO4_16_OUTPUT  = 52.3;
    private static final double LiFePO4_16_CELLS   = 51.7;
    private static final double NMC_15_OUTPUT      = 55.9;
    private static final double NMC_15_CELLS       = 55.5;
    private static final double MIN_VIABLE_VOLTAGE = 43.0;
    private static final int    FM_LIMIT_PERCENT   = 45;
    private static final int    TX_EXTRA_PERCENT   = 100-FM_LIMIT_PERCENT;
    private static final double VOLTAGE_SPIKE      = 1.2;
    private static final double REMAIN_SPIKE       = 20;
    private static final double DECAY_NEW_STEP     = 0.02;
    private static final double DECAY_NEW_LEAP     = 0.25;
    private static final double DECAY_OLD_STEP     = 1.0-DECAY_NEW_STEP;
    private static final double DECAY_OLD_LEAP     = 1.0-DECAY_NEW_LEAP;
    private static final double VOLT_CURVE_OUTPUT  = 4;
    private static final double VOLT_CURVE_CELLS   = 6;
    private static final int    MOVING_RPMS        = 10;

    // State Variables
    private static int owPercent = 0;
    private static double medianOutput = LiFePO4_16_OUTPUT;
    private static double medianCells = LiFePO4_16_CELLS;
    private static double avgVoltsOut = -1.0;
    private static double avgVoltsCells = -1.0;
    private static double avgRemainOut = 0.0;
    private static double avgRemainCells = 0.0;
    private static double ampAdjust = 0.0;
    private static double idleAdjust = 0.0;

    private static double voltDecay(double old, double cur) {
        if (old < MIN_VIABLE_VOLTAGE) {
            return(cur);
        } else if (Math.abs(old - cur) > VOLTAGE_SPIKE) {
            return(old*DECAY_OLD_LEAP + cur*DECAY_NEW_LEAP);
        } else {
            return(old*DECAY_OLD_STEP + cur*DECAY_NEW_STEP);
        }
    }

    private static double remainDecay(double old, double cur) {
        if ( old < 1 ) {
            return(cur);
        } else if (Math.abs(old - cur) > REMAIN_SPIKE) {
            return(old*DECAY_OLD_LEAP + cur*DECAY_NEW_LEAP);
        } else {
            return(old*DECAY_OLD_STEP + cur*DECAY_NEW_STEP);
        }
    }

    private static double calcRemain(double curve, double median, double volts) {
        return( 99.9 / (1.0+Math.pow(curve, median-volts)) + 1 );
    }

    public static int getRemainingDefault() {
        return(owPercent);
    }

    public static int getRemainingOutput() {
        return((int)avgRemainOut);
    }

    public static int getRemainingCells() {
        return((int)avgRemainCells);
    }

    public static int getRemainingTwoX() {
        int owMin = 3;
        double remaining;

        if (owPercent > owMin) {
            remaining = owPercent*FM_LIMIT_PERCENT/100+TX_EXTRA_PERCENT;
        } else {
            remaining = Math.min(getRemainingCells(), TX_EXTRA_PERCENT+owMin);
        }

        Timber.d( "remainingForTwoX:" + remaining);

        return((int)remaining);
    }

    public static void setHardware(int hver) {
        if (hver<4000) {
            medianOutput = LiFePO4_16_OUTPUT;
            medianCells = LiFePO4_16_CELLS;
        } else {
            medianOutput = NMC_15_OUTPUT;
            medianCells = NMC_15_CELLS;
        }
    }

    public static boolean setAmps(double amps) {
        ampAdjust = amps/9;
        //ampAdjust = 4.0/(1+Math.pow(0.75, amps/3))-2;

        return(false);
    }

    public static boolean setSpeedRpm(int rpms) {
        if (rpms<MOVING_RPMS) {
            idleAdjust = -0.2;
        } else {
            idleAdjust = 0;
        }
        return(false);
    }

    public static boolean setUsedAmpHrs(double amphrs) {
        //TODO: Keep track and report on Trip AmpHrs used
        Timber.d( "setUsedAmpHrs:" + amphrs);
        return(false);
    }

    public static boolean setRegenAmpHrs(double amphrs) {
        //TODO: Keep track and report with regen AmpHrs in account
        Timber.d( "setRegenAmpHrs:" + amphrs);
        return(false);
    }

    public static boolean setRemaining(int level) {
        if (owPercent != level) {
            owPercent = level;
            return(true);
        } else {
            return(false);
        }
    }

    public static boolean setOutput(double volts) {
        double change = avgRemainOut;
        double remaining;

        if (volts < MIN_VIABLE_VOLTAGE) {
            return(false);
        }

        avgVoltsOut = voltDecay(avgVoltsOut, volts+ampAdjust+idleAdjust);

        remaining = calcRemain(VOLT_CURVE_OUTPUT, medianOutput, avgVoltsOut);
        avgRemainOut = remainDecay(avgRemainOut, remaining);

        Timber.d( "avgRemainOut:" + avgRemainOut);

        change -= avgRemainOut;
        return((int)change != 0);
    }

    public static boolean setCells(double volts) {
        double change = avgRemainCells;
        double remaining;

        if (volts < MIN_VIABLE_VOLTAGE) {
            return(false);
        }

        avgVoltsCells = voltDecay(avgVoltsCells, volts+ampAdjust+idleAdjust);

        remaining = calcRemain(VOLT_CURVE_CELLS, medianCells, avgVoltsCells);
        avgRemainCells = remainDecay(avgRemainCells, remaining);

        Timber.d( "avgRemainingCells:" + avgRemainCells);

        change -= avgRemainCells;
        return((int)change != 0);
    }

}


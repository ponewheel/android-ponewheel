package net.kwatts.powtools.util;

import timber.log.Timber;


public class Battery {
    // Tunable Variables (use these to tweak the output)
    private static final double LiFePO4_16_OUTPUT  = 52.3;
    private static final double LiFePO4_16_CELLS   = 51.9;
    private static final double NMC_15_OUTPUT      = 55.9;
    private static final double NMC_15_CELLS       = 55.5;
    private static final double MIN_VIABLE_VOLTAGE = 43.0;
    private static final int    FM_LIMIT_PERCENT   = 44;
    private static final int    TX_EXTRA_PERCENT   = 100-FM_LIMIT_PERCENT;
    private static final int    OW_REMAIN_MIN      = 3;
    private static final int    AMP_REMAIN_MIN     = 3+OW_REMAIN_MIN;
    private static final double AMP_SMOOTH_OUTPUT  = 9;
    private static final double AMP_SMOOTH_CELLS   = 10;
    private static final double DECAY_NEW_STEP     = 0.02;
    private static final double DECAY_OLD_STEP     = 1.0-DECAY_NEW_STEP;
    private static final double VCURVE_OUTPUT_LOW  = 6;
    private static final double VCURVE_OUTPUT_HIGH = 5;
    private static final double VCURVE_CELLS_LOW   = 3;
    private static final double VCURVE_CELLS_HIGH  = 13;
    private static final int    MOVING_RPMS        = 10;

    // State Variables
    private static int    owPercent = 0;
    private static int    owRemaining = 0;
    private static int    cellCount = 16;
    private static double fmLimitPercent = FM_LIMIT_PERCENT;
    private static double txExtraPercent = TX_EXTRA_PERCENT;
    private static double medianOutput = LiFePO4_16_OUTPUT;
    private static double medianCells = LiFePO4_16_CELLS;
    private static double avgVoltsOut = -1.0;
    private static double avgVoltsCells = -1.0;
    private static double avgRemainOut = 0.0;
    private static double avgRemainCells = 0.0;
    private static double ampAdjust = 0.0;
    private static double idleAdjust = 0.0;
    private static double tempCurveRatio = 1.0;

    private static double ampsRemainBase = -1.0;
    private static double ampsRemainStart = -1.0;
    private static double ampsUsedStart = -1.0;
    private static double ampsRegenStart = -1.0;
    private static double ampsUsed = -1.0;
    private static double ampsRegen = -1.0;
    private static double ampsConvert = 0.0;
    private static int    ampsRemaining = 0;


    // The first filter to apply to any voltage read is to try and update
    // it based on the current Amp Hrs draw.  Voltage swings are higher
    // when the batter is full than when it is empty, so this adjustment
    // scales based on the current voltage draw.
    private static double adjustVolts(double volts, double median, double smooth) {
	   double scale;

	   scale = smooth + (median-volts);
	   scale = Math.min(Math.max( 2, scale ), smooth*2);

	   return( volts + ampAdjust / scale + idleAdjust );
    }

    // The second filter to apply to any voltage read is to smooth it
    // out based on previous reads.  This is a decaying average calculation
    // which combines previous values with current values while only having
    // to keep one variable of state.  Use DECAY_NEW_STEP to tune this
    // voltage calculation.
    private static double voltDecay(double old, double cur) {
        if (old < MIN_VIABLE_VOLTAGE) {
            return(cur);
        } else {
            return(old*DECAY_OLD_STEP + cur*DECAY_NEW_STEP);
        }
    }

    // The third filter to apply when turning a voltage into a percent is
    // to smooth it with a second decaying average.  This one is calculated
    // against the battery percentage remaining.  Use DECAY_NEW_STEP to
    // tune this percentage remaining calculation.
    private static double remainDecay(double old, double cur) {
        if ( old < 1 ) {
            return(cur);
        } else {
            return(old*DECAY_OLD_STEP + cur*DECAY_NEW_STEP);
        }
    }

    // To convert a voltage to a battery percent remaining, an equasion
    // is used which can handle the sharp initial drop, slow voltage drop
    // over time, and then a final, sharper drop.
    private static double calcRemain(double curve, double median, double volts) {
        curve*=tempCurveRatio;
        return( 99.9 / (1.0+Math.pow(curve, median-volts)) + 1 );
    }

    // Two-X might be it's name, but it more than doubles the capacity.  This
    // calculation converts what the OneWheel things is a percent into what
    // actually constitutes a Two-X percent.
    private static double convertRatioTwoX(double percent) {
        return( percent*fmLimitPercent/100 );
    }

    public static int getRemainingDefault() {
        return(owPercent);
    }

    public static int getRemainingOW() {
        return(owRemaining);
    }

    public static int getRemainingAmps() {
        return(ampsRemaining);
    }

    public static int getRemainingOutput() {
        return((int)avgRemainOut);
    }

    public static int getRemainingCells() {
        return((int)avgRemainCells);
    }

    // A central goal of this entire package is to make an accurate percent
    // remaining calculation for a Two-X battery modification.  This function
    // puts together all the functions to make the best possible calculation.
    // It is only valid if the OneWheel is fully charged (the charger block
    // light goes green) when charged.  The OneWheel doesn't have to be
    // charged between every use, but when charged, charge it all the way.
    // 1. Use the value provided by the OneWheel as long as it is over 3
    // 2. Keep track of how many amp hours make up each OW percent and use
    //    that number to continue the battery remaining percent draw down.
    // 3. If we are starting to run out of battery, try a couple different
    //    methods and take the lowest, bettery to let people know the OW
    //    is running out of battery early than late.
    // 4. If we don't have enough history to do a better calculation, fall
    //    back on the Cell Voltage calculation, which gets relatively accurate
    //    as it gets closer to 0.
    public static int getRemainingTwoX() {
        double remaining;

        if (owPercent >= OW_REMAIN_MIN) {
            remaining = owRemaining;
        //TODO: be more conservative at the bottom?
        //} else if (ampsRemaining > AMP_REMAIN_MIN) {
        } else if (ampsRemaining > OW_REMAIN_MIN) {
            remaining = ampsRemaining;
        } else if (ampsRemaining > 0) {
            remaining = Math.min(ampsRemaining, (int)avgRemainCells);
        } else {
            remaining = Math.min((int)avgRemainCells, txExtraPercent);
        }

        Timber.v( "TwoX:%.2f, ow:%d, amphrs:%d, output:%.2f, cells:%.2f", remaining, owPercent, ampsRemaining, avgRemainOut, avgRemainCells);

        return((int)remaining);
    }

    public static boolean checkCells(int count) {
        return( count == cellCount );
    }

    // Use the hardware version to set hardware specific values
    public static void setHardware(int hver) {
        if (hver<4000) {
            medianOutput = LiFePO4_16_OUTPUT;
            medianCells = LiFePO4_16_CELLS;
            cellCount = 16;
        } else {
            medianOutput = NMC_15_OUTPUT;
            medianCells = NMC_15_CELLS;
            cellCount = 15;
        }
    }

    public static boolean setAmps(double amps) {
        ampAdjust = amps;

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

    public static boolean setBatteryTemp(int temp) {
        int percent = 0;

        //TODO: do we need this, or can we take it out (so far, take it out)
        //Temp (in Celcius) comes from enclosed battery box, not ambient
        //Some initial guesses on how to adjust based on temperature
        //These are not tested
        //!tempCurveRatio = 1+(temp-30)/100;

        //TODO: do we need this, or can we take it out (so far, take it out)
        //percent=(temp-20)/10;  //Valid as a difference from 45?? No idea!
        //!fmLimitPercent = FM_LIMIT_PERCENT-percent;
        //!txExtraPercent = TX_EXTRA_PERCENT+percent;

        //!Timber.d( "setBatteryTemp:%d, tempCurveRatio:%.2f", temp, tempCurveRatio );
        return(false);
    }

    // The best way to accurately calculate how much percentage battery is left
    // is to know how many Amp Hrs used makes up each percentage.  These
    // calculations do their best to make that number as accurate as possible.
    public static boolean setUsedAmpHrs(double amphrs) {
        int change = ampsRemaining;

        if (amphrs > 0.1) {
            if (amphrs < 0.5 || amphrs < ampsUsed) {
                ampsRemainBase = -1.0;
                ampsRemainStart = -1.0;
                ampsUsedStart = -1.0;
                ampsRegen = 0.0;
            } else if (ampsRemainBase > 0 && ampsConvert > 0) {
                ampsRemaining = (int)Math.floor(ampsRemainBase - convertRatioTwoX((amphrs-ampsRegen)/ampsConvert));
            } else {
                ampsRemaining = 0;
	       }

            ampsUsed = amphrs;

            //Timber.d( "setUsedAmpHrs:%.2f, ampsRegen:%.2f, ampsConvert:%.2f, ampsBase:%.2f, ampsRemain:%d", ampsUsed, ampsRegen, ampsConvert, ampsRemainBase, ampsRemaining );
        }

        change -= ampsRemaining;
        return((int)change != 0);
    }

    public static boolean setRegenAmpHrs(double amphrs) {
        ampsRegen = amphrs;

        if (ampsRegenStart < 0) {
            ampsRegenStart = amphrs;
        }

        return(false);
    }

    // Set everything up to monitor batter based on the OneWheel's belief,
    // which simply is the value for the Default.
    // Also, setup and make everything work for battery mods to continute
    // drawing the percentage down based on an amp hours calculation.
    public static boolean setRemaining(int level) {
        double ampsUsedDiff, travelPct;

        if (owPercent != level) {
            owPercent = level;
            owRemaining = (int)(convertRatioTwoX(owPercent)+txExtraPercent);

            // We want to capture the starting AmpHrs at the turn of a percent
            // to ensure we get a full percentage AmpHrs change.
            if (owPercent >= AMP_REMAIN_MIN && ampsRemainStart < owPercent) {
                ampsRemainBase = convertRatioTwoX(owPercent)+txExtraPercent;
                ampsRemainStart = owPercent;
                ampsUsedStart = ampsUsed;
            } else if (owPercent >= OW_REMAIN_MIN && ampsRemainBase > 0 && ampsRegen < ampsUsed) {
                ampsUsedDiff = (ampsUsed-ampsUsedStart)-(ampsRegen-ampsRegenStart);
                travelPct = (ampsRemainStart-owPercent);

                ampsConvert = ampsUsedDiff / travelPct;
                Timber.d("ampsConvert:%.2f = ampsUsedDiff:%.2f / travelPct:%.2f", ampsConvert, ampsUsedDiff, travelPct);
            }

            return(true);
        } else {
            return(false);
        }
    }

    // The OneWheel reports an accurage and highly variable current voltage
    // calculation, use that to try and calculate the battery remaining.
    public static boolean setOutput(double volts) {
        int change = (int)avgRemainOut;
        double adjusted, volt_curve, remaining;

        if (volts < MIN_VIABLE_VOLTAGE) {
            return(false);
        }

        adjusted = adjustVolts(volts, medianOutput, AMP_SMOOTH_OUTPUT);

        avgVoltsOut = voltDecay(avgVoltsOut, adjusted);

        if ( avgVoltsOut > medianOutput ) {
            volt_curve = VCURVE_OUTPUT_HIGH;
        } else {
            volt_curve = VCURVE_OUTPUT_LOW;
        }
        remaining = calcRemain(volt_curve, medianOutput, avgVoltsOut);
        avgRemainOut = remainDecay(avgRemainOut, remaining);

        //Timber.d( "avgRemainOut:%.2f, curve:%.1f", avgRemainOut, volt_curve);

        change -= (int)avgRemainOut;
        return(change != 0);
    }

    // The OneWheel reports a slightly less variable voltage by Cell.  If you
    // add these up and use them to calculate battery remaining, it gets
    // really accurate as the battery starts to run out.
    public static boolean setCells(double volts) {
        int change = (int)avgRemainCells;
        double adjusted, volt_curve, remaining;

        if (volts < MIN_VIABLE_VOLTAGE) {
            return(false);
        }

        adjusted = adjustVolts(volts, medianCells, AMP_SMOOTH_CELLS);

        avgVoltsCells = voltDecay(avgVoltsCells, adjusted);

        if ( avgVoltsCells > medianOutput ) {
            volt_curve = VCURVE_CELLS_HIGH;
        } else {
            volt_curve = VCURVE_CELLS_LOW;
        }
        remaining = calcRemain(volt_curve, medianCells, avgVoltsCells);
        avgRemainCells = remainDecay(avgRemainCells, remaining);

        //Timber.d( "avgRemainCells:%.2f, avgVoltsCells:%.2f, volts:%.2f, ampAdjust:%.2f, idleAdjust:%.2f, median:%.2f, curve:%.1f",avgRemainCells, avgVoltsCells, volts, ampAdjust, idleAdjust, medianCells, volt_curve);

        change -= (int)avgRemainCells;
        return(change != 0);
    }

}


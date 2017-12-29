package net.kwatts.powtools.model;

import android.support.annotation.VisibleForTesting;

import java.util.BitSet;

import timber.log.Timber;

public class DeviceStatus
{
    public final Boolean bmsCtrlComms;
    public final Boolean brokenCapacitor;
    public final Boolean charging;
    //public final StatusCode code = StatusCode.from(paramArrayOfByte[1]);
    public final Boolean icsuFault;
    public final Boolean icsvFault;
    public final Boolean riderDetectPad1;
    public final Boolean riderDetectPad2;
    public final Boolean riderDetected;

    public DeviceStatus(byte[] paramArrayOfByte)
    {
        if (paramArrayOfByte.length == 0) {
            Timber.d("DeviceStatus: all status values were zero");
            this.riderDetected = false;
            this.riderDetectPad1 = false;
            this.riderDetectPad2 = false;
            this.icsuFault = false;
            this.icsvFault = false;
            this.charging = false;
            this.bmsCtrlComms = false;
            this.brokenCapacitor = false;
            return;
        }
        byte[] arrayOfByte = new byte[1];
        arrayOfByte[0] = paramArrayOfByte[0];
        BitSet localBitSet = BitSet.valueOf(arrayOfByte);
        this.riderDetected = localBitSet.get(0);
        this.riderDetectPad1 = localBitSet.get(1);
        this.riderDetectPad2 = localBitSet.get(2);
        this.icsuFault = localBitSet.get(3);
        this.icsvFault = localBitSet.get(4);
        this.charging = localBitSet.get(5);
        this.bmsCtrlComms = localBitSet.get(6);
        this.brokenCapacitor = localBitSet.get(7);
    }

    public static DeviceStatus from(byte[] paramArrayOfByte)
    {
        return new DeviceStatus(paramArrayOfByte);
    }

    @VisibleForTesting // testing and/or mock
    public static byte[] toByteArray(
            boolean riderDetected,
            boolean riderDetectPad1,
            boolean riderDetectPad2,
            boolean icsuFault,
            boolean icsvFault,
            boolean charging,
            boolean bmsCtrlComms,
            boolean brokenCapacitor ) {

        BitSet bitSet = new BitSet(8);
        bitSet.set(0,riderDetected);
        bitSet.set(1,riderDetectPad1);
        bitSet.set(2,riderDetectPad2);
        bitSet.set(3,icsuFault);
        bitSet.set(4,icsvFault);
        bitSet.set(5,charging);
        bitSet.set(6,bmsCtrlComms);
        bitSet.set(7,brokenCapacitor);

        return bitSet.toByteArray();
    }
}

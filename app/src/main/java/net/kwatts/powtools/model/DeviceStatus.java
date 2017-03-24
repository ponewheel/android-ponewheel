package net.kwatts.powtools.model;

import java.util.BitSet;

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
        byte[] arrayOfByte = new byte[1];
        arrayOfByte[0] = paramArrayOfByte[0];
        BitSet localBitSet = BitSet.valueOf(arrayOfByte);
        this.riderDetected = Boolean.valueOf(localBitSet.get(0));
        this.riderDetectPad1 = Boolean.valueOf(localBitSet.get(1));
        this.riderDetectPad2 = Boolean.valueOf(localBitSet.get(2));
        this.icsuFault = Boolean.valueOf(localBitSet.get(3));
        this.icsvFault = Boolean.valueOf(localBitSet.get(4));
        this.charging = Boolean.valueOf(localBitSet.get(5));
        this.bmsCtrlComms = Boolean.valueOf(localBitSet.get(6));
        this.brokenCapacitor = Boolean.valueOf(localBitSet.get(7));
    }

    public static DeviceStatus from(byte[] paramArrayOfByte)
    {
        return new DeviceStatus(paramArrayOfByte);
    }
}

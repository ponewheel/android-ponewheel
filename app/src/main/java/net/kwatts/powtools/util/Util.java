package net.kwatts.powtools.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.IntRange;

import java.util.LinkedList;
import java.util.List;
/**
 * Created by kwatts on 6/15/16.
 */
public class Util {


    public static String getUsername(Context c) {
        AccountManager manager = AccountManager.get(c);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");

            if (parts.length > 1)
                return parts[0];
        }
        return null;
    }

    /* Helper methods. For dealing with bytes see:
       http://www.roseindia.net/java/master-java/bitwise-bitshift-operators.shtml
       https://calleerlandsson.com/2014/02/06/rubys-bitwise-operators/
     */
    public static short byteToShort(byte[] v) {
        return (short)((v[1] << 8) + (v[0] & 0xff));
    }

    private static final byte[] twoBytes = new byte[2];
    public static byte[] intToShortBytes(@IntRange(from = -32768, to = 32767) int v) {

        twoBytes[0] = (byte) (v >> 8 & 0xff);
        twoBytes[1] = (byte) (v & 0xff);

        return twoBytes;
    }

    // 1 byte/8-bit signed two's complement (-128 to 127)
    // returns an int, 32-bit signed two's complement
    public static int unsignedByte(byte var0) {
        return var0 & 255;
    }

    // short = 2 bytes/16-bit signed two's complement (-32,768 to 32,767)
    public static int unsignedShort(byte[] var0) {
        int var1;
        if(var0.length < 2) {
            var1 = -1;
        } else {
            var1 = (unsignedByte(var0[0]) << 8) + unsignedByte(var0[1]);
        }
        // ByteBuffer.wrap(v_bytes).getShort() also works
        // or
        // ByteBuffer bb = ByteBuffer.allocate(2);
        // bb.order(ByteOrder.LITTLE_ENDIAN);
        // bb.put(var0[0]);
        // bb.put(var0[1]);
        // short shortVal = bb.getShort(0);
        return var1;
    }

    // double is 64 bit and used for decimals
    public static double cel2far(int celsius) {
        return (9.0/5.0)*celsius + 32;
    }
    public static double far2cel(int far) {
        return (5.0/9.0)*(far - 32);
    }
    public static double milesToKilometers(double paramDouble) {
        return paramDouble * 1.609344;

    }
    public static double revolutionsToKilometers(double paramDouble)
    {
        return paramDouble * 35.0D / 39370.099999999999D;
    }

    public static double revolutionsToMiles(double paramDouble)
    {
        return paramDouble * 35.0D / 63360.0D;
    }
    public static double rpmToKilometersPerHour(double paramDouble)
    {
        return 60.0D * (35.0D * paramDouble) / 39370.099999999999D;
    }

    public static double rpmToMilesPerHour(double paramDouble)
    {
        return 60.0D * (35.0D * paramDouble) / 63360.0D;
    }

    public static String bytesToHex(byte c[]) {
        StringBuilder sb = new StringBuilder();
        for (byte b : c) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * If your number X falls between A and B, and you would like Y to fall between C and D, you can apply the following linear transform:
     *
     * Y = (X-A)/(B-A) * (D-C) + C
     *
     * https://stackoverflow.com/a/345204/247325
     */
    public static float linearTransform(float x, float a, float b, float c, float d) {
        return (x-a)/(b-a) * (d-c) + c;
    }
    /**
     * If your number X falls between A and B, and you would like Y to fall between C and D, you can apply the following linear transform:
     *
     * Y = (X-A)/(B-A) * (D-C) + C
     *
     * https://stackoverflow.com/a/345204/247325
     */
    public static double linearTransform(double x, double a, double b, double c, double d) {
        return (x-a)/(b-a) * (d-c) + c;
    }
}

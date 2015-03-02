package net.ctrdn.stuba.want.swrouter.common;

import java.text.DecimalFormat;

public class DataTypeHelpers {

    public static String getReadableByteSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public final static short getUnsignedByteValue(byte b) {
        if (b < 0) {
            return (short) (b & 0xff);
        } else {
            return b;
        }
    }

    public final static int getUnsignedShortValue(short s) {
        if (s < 0) {
            return (s & 0xffff);
        } else {
            return s;
        }
    }

    public final static int getUnsignedShortFromBytes(byte msb, byte lsb) {
        short targetShort = DataTypeHelpers.getUnsignedByteValue(lsb);
        targetShort |= (msb << 8);
        return DataTypeHelpers.getUnsignedShortValue(targetShort);
    }

    public final static byte[] getUnsignedShort(int value) {
        byte[] data = new byte[2];
        data[0] = (byte) ((value & 0xffffffff) >> 8);
        data[1] = (byte) (value & 0xffffffff);
        return data;
    }

    public final static byte getUnsignedByte(short value) {
        byte data = (byte) (value & 0xffff);
        return data;
    }

    public final static String byteArrayToHexString(byte[] a) {
        return DataTypeHelpers.byteArrayToHexString(a, false);
    }

    public final static String byteArrayToHexString(byte[] a, boolean spaces) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b & 0xff));
            if (spaces) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public final static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public final static long RFC1071Checksum(byte[] buf, int length) {
        int i = 0;
        long sum = 0;
        while (length > 0) {
            sum += (buf[i++] & 0xff) << 8;
            if ((--length) == 0) {
                break;
            }
            sum += (buf[i++] & 0xff);
            --length;
        }

        return (~((sum & 0xFFFF) + (sum >> 16))) & 0xFFFF;
    }
}

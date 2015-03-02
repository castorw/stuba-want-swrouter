package net.ctrdn.stuba.want.swrouter.common;

import java.util.Arrays;

public class MACAddress {

    public static final MACAddress BROADCAST = new MACAddress(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
    public static final MACAddress ZERO = new MACAddress(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
    private final byte[] addressBytes;

    public MACAddress(byte[] address) {
        this.addressBytes = address;
    }

    @Override
    public String toString() {
        String o = "";
        for (int i = 0; i < 6; i++) {
            if (!o.isEmpty()) {
                o += ":";
            }
            String x = Integer.toString(this.getAddressBytes()[i] & 0xff, 16);
            if (x.length() < 2) {
                o += "0";
            }
            o += x;
        }
        return o;
    }

    public static MACAddress fromString(String str) {
        String[] split = str.split(":");
        if (split.length != 6) {
            throw new RuntimeException("invalid mac addr string");
        }
        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) (Short.parseShort(split[i], 16) & 0xff);
        }

        return new MACAddress(bytes);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.getAddressBytes());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MACAddress other = (MACAddress) obj;
        if (!Arrays.equals(this.addressBytes, other.addressBytes)) {
            return false;
        }
        return true;
    }

    public byte[] getAddressBytes() {
        return addressBytes;
    }

    public boolean isBroadcast() {
        for (int i = 0; i < 6; i++) {
            if (this.addressBytes[i] != (byte) 0xff) {
                return false;
            }
        }
        return true;
    }
}

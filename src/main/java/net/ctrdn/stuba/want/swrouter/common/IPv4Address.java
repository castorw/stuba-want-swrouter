package net.ctrdn.stuba.want.swrouter.common;

import java.util.Arrays;

public class IPv4Address {

    private final byte[] addressBytes;

    public IPv4Address(byte[] address) {
        this.addressBytes = address;
    }

    public IPv4Address(String address) {
        String[] exp = address.split("\\.");
        if (exp.length == 4) {
            this.addressBytes = new byte[]{(byte) (Short.parseShort(exp[0]) & (byte) 0xff), (byte) (Short.parseShort(exp[1]) & (byte) 0xff), (byte) (Short.parseShort(exp[2]) & (byte) 0xff), (byte) (Short.parseShort(exp[3]) & (byte) 0xff)};
        } else {
            this.addressBytes = null;
        }
    }

    @Override
    public String toString() {
        return (getAddressBytes()[0] & 0xff) + "." + (getAddressBytes()[1] & 0xff) + "." + (getAddressBytes()[2] & 0xff) + "." + (getAddressBytes()[3] & 0xff);
    }

    public static IPv4Address fromString(String str) {
        String[] split = str.split("\\.");
        if (split.length != 4) {
            throw new RuntimeException("invalid ip addr string");
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (Short.parseShort(split[i]) & 0xff);
        }
        return new IPv4Address(bytes);
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
        final IPv4Address other = (IPv4Address) obj;
        return Arrays.equals(this.addressBytes, other.addressBytes);
    }

    public byte[] getAddressBytes() {
        return addressBytes;
    }
}

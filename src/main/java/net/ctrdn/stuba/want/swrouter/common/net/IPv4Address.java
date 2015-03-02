package net.ctrdn.stuba.want.swrouter.common.net;

import java.util.Arrays;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;

public class IPv4Address {

    private final byte[] addressBytes;

    public IPv4Address(byte[] address) {
        this.addressBytes = address;
    }

    public static IPv4Address fromString(String stringRepresentedAddress) throws IPv4MathException {
        String[] split = stringRepresentedAddress.split("\\.");
        if (split.length != 4) {
            throw new IPv4MathException("Invalid format of IPv4 address " + stringRepresentedAddress);
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (Short.parseShort(split[i]) & 0xff);
        }
        return new IPv4Address(bytes);
    }

    public static IPv4Address fromDecimal(int decimalRepresentedAddress) {
        byte[] b = new byte[4];
        b[0] = (byte) ((decimalRepresentedAddress >> 24) & 0xff);
        b[1] = (byte) ((decimalRepresentedAddress >> 16) & 0xff);
        b[2] = (byte) ((decimalRepresentedAddress >> 8) & 0xff);
        b[3] = (byte) ((decimalRepresentedAddress) & 0xff);
        return new IPv4Address(b);
    }

    public byte[] getBytes() {
        return this.addressBytes;
    }

    public int getDecimal() {
        int b = 0;
        b |= (int) (this.addressBytes[0] << 24) & 0xff000000;
        b |= (int) (this.addressBytes[1] << 16) & 0x00ff0000;
        b |= (int) (this.addressBytes[2] << 8) & 0x0000ff00;
        b |= (int) (this.addressBytes[3]) & 0x000000ff;
        return b;
    }

    @Override
    public String toString() {
        return (this.getBytes()[0] & 0xff) + "." + (this.getBytes()[1] & 0xff) + "." + (this.getBytes()[2] & 0xff) + "." + (this.getBytes()[3] & 0xff);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Arrays.hashCode(this.addressBytes);
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
}

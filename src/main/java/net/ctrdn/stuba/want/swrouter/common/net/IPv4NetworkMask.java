package net.ctrdn.stuba.want.swrouter.common.net;

import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;

public class IPv4NetworkMask {

    private final int length;

    public IPv4NetworkMask(int length) throws IPv4MathException {
        if (length > 32 || length < 0) {
            throw new IPv4MathException("Netmask must be in range from 0 to 32");
        }
        this.length = length;
    }

    public int getDecimal() {
        int b = 0;
        for (int i = 0; i < this.length; i++) {
            b |= ((int) (1 & 0xffffffff) << (31 - i));
        }
        return b;
    }

    public byte[] getBytes() {
        byte[] bb = new byte[4];
        int bi = this.getDecimal();
        for (int i = 0; i < 4; i++) {
            bb[i] = (byte) (bi >> (8 * (3 - i)));
        }
        return bb;
    }

    public int getLength() {
        return length;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.length;
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
        final IPv4NetworkMask other = (IPv4NetworkMask) obj;
        return this.length == other.length;
    }
}

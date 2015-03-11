package net.ctrdn.stuba.want.swrouter.common.net;

import java.util.Objects;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;

public class IPv4Prefix {

    public static final IPv4Prefix MULTICAST = IPv4Prefix.createStatic("224.0.0.0", 4);
    private final IPv4Address address;
    private final IPv4NetworkMask networkMask;

    public IPv4Prefix(IPv4Address address, IPv4NetworkMask networkMask) throws IPv4MathException {
        int adDec = address.getDecimal();
        int nmDec = networkMask.getDecimal();
        if ((adDec | nmDec) << networkMask.getLength() != 0) {
            throw new IPv4MathException(address.toString() + "/" + networkMask.getLength() + " is not a valid IPv4 prefix");
        }
        this.address = address;
        this.networkMask = networkMask;
    }

    private static IPv4Prefix createStatic(String addressString, int netmaskLength) {
        try {
            return new IPv4Prefix(IPv4Address.fromString(addressString), new IPv4NetworkMask(netmaskLength));
        } catch (IPv4MathException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean containsAddress(IPv4Address address) {
        int prefixAddressDec = this.getAddress().getDecimal();
        int addressDec = address.getDecimal();
        if (prefixAddressDec == 0 && this.getNetworkMask().getLength() == 0) {
            return true;
        }
        return (prefixAddressDec >> (32 - this.getNetworkMask().getLength()) == (addressDec >> (32 - this.getNetworkMask().getLength())));
    }

    public boolean containsPrefix(IPv4Prefix prefix) {
        if (this.getNetworkMask().getLength() > prefix.getNetworkMask().getLength()) {
            return false;
        }
        int thisPrefixAddressDec = this.getAddress().getDecimal();
        int otherPrefixAddressDec = this.getAddress().getDecimal();
        int thisNetworkMathLength = this.getNetworkMask().getLength();
        return (thisPrefixAddressDec >> (32 - thisNetworkMathLength)) == (otherPrefixAddressDec >> (32 - thisNetworkMathLength));
    }

    @Override
    public String toString() {
        return this.getAddress().toString() + "/" + this.getNetworkMask().getLength();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.getAddress());
        hash = 53 * hash + Objects.hashCode(this.getNetworkMask());
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
        final IPv4Prefix other = (IPv4Prefix) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return Objects.equals(this.getNetworkMask(), other.getNetworkMask());
    }

    public IPv4Address getAddress() {
        return address;
    }

    public IPv4NetworkMask getNetworkMask() {
        return networkMask;
    }
}

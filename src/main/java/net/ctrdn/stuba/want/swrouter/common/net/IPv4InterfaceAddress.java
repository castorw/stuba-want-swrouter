package net.ctrdn.stuba.want.swrouter.common.net;

import java.util.Objects;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;

public class IPv4InterfaceAddress {

    private final IPv4Address address;
    private final IPv4Prefix prefix;

    public IPv4InterfaceAddress(IPv4Address address, IPv4NetworkMask networkMask) throws IPv4MathException {
        this.address = address;
        int prefixAddressDec = address.getDecimal() >> (32 - networkMask.getLength());
        prefixAddressDec = prefixAddressDec << (32 - networkMask.getLength());
        IPv4Address prefixAddress = IPv4Address.fromDecimal(prefixAddressDec);
        this.prefix = new IPv4Prefix(prefixAddress, networkMask);
    }

    public IPv4Address getAddress() {
        return address;
    }

    public IPv4Prefix getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return this.address.toString() + "@" + this.prefix.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.address);
        hash = 79 * hash + Objects.hashCode(this.prefix);
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
        final IPv4InterfaceAddress other = (IPv4InterfaceAddress) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return Objects.equals(this.prefix, other.prefix);
    }
}

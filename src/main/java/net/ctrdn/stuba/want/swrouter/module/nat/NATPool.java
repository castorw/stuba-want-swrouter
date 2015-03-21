package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.exception.NATException;

public class NATPool {

    private final UUID id;
    private final NATModule natModule;
    private final String name;
    private final IPv4Prefix prefix;
    private final List<NATAddress> addressList = new ArrayList<>();

    public NATPool(NATModule natModule, String name, IPv4Prefix prefix) {
        this.natModule = natModule;
        this.name = name;
        this.prefix = prefix;
        this.id = UUID.randomUUID();
    }

    protected void addAddress(IPv4Address address) throws NATException {
        if (this.getPrefix().containsAddress(address)) {
            this.getAddressList().add(this.natModule.getNATAddress(address));
        } else {
            throw new NATException("Address " + address + " does not belong to NAT pool " + this.getName() + " with prefix " + this.getPrefix());
        }
    }

    public String getName() {
        return name;
    }

    public IPv4Prefix getPrefix() {
        return prefix;
    }

    public List<NATAddress> getAddressList() {
        return addressList;
    }

    public UUID getID() {
        return this.id;
    }
}

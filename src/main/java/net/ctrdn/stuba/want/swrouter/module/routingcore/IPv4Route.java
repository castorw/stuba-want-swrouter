package net.ctrdn.stuba.want.swrouter.module.routingcore;

import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.exception.RoutingException;

public interface IPv4Route {

    /**
     * Returns the target IPv4 prefix.
     *
     * @return The IPv4 prefix representing destination network
     */
    public IPv4Prefix getTargetPrefix();

    /**
     * Returns next forwarder address. In case of ECMP, different addresses will
     * be returned for particular separate calls.
     *
     * @return IPv4 address of next forwarding router
     */
    public IPv4RouteGateway getNextGateway();

    public IPv4RouteGateway[] getGateways();

    /**
     * Administrative distance.
     *
     * @return Administrative distance
     */
    public int getAdministrativeDistance();

    /**
     * Returns flags currently available.
     *
     * @return Route flags
     */
    public IPv4RouteFlag[] getFlags();

    public void addFlag(IPv4RouteFlag flag) throws RoutingException;

    public void removeFlag(IPv4RouteFlag flag) throws RoutingException;

    @Override
    public String toString();
}

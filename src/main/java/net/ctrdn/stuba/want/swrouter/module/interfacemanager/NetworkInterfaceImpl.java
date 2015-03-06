package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4InterfaceAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteFlag;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteGateway;
import net.ctrdn.stuba.want.swrouter.module.routingcore.RoutingCoreModule;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkInterfaceImpl implements NetworkInterface {

    private class Receiver implements Runnable {

        private final NetworkInterfaceImpl networkInterface = NetworkInterfaceImpl.this;
        private final Logger logger = NetworkInterfaceImpl.this.logger;
        private boolean running = true;

        @Override
        public void run() {
            PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

                @Override
                public void nextPacket(PcapPacket pcapPacket, String user) {
                    try {
                        ProcessingChain chain = ProcessingChain.FORWARD;
                        Packet packet = new Packet(Receiver.this.networkInterface, pcapPacket);
                        if (packet.getEthernetType() == EthernetType.IPV4) {
                            if (packet.getDestinationIPv4Address().equals(Receiver.this.networkInterface.getIPv4InterfaceAddress().getAddress())) {
                                chain = ProcessingChain.INPUT;
                            }
                        } else if (packet.getDestinationHardwareAddress().equals(Receiver.this.networkInterface.getHardwareAddress()) || packet.getDestinationHardwareAddress().isBroadcast()) {
                            chain = ProcessingChain.INPUT;
                        }
                        packet.setProcessingChain(chain);
                        Receiver.this.networkInterface.routerController.getPacketProcessor().processPacket(packet);
                    } catch (PacketException ex) {
                        Receiver.this.networkInterface.logger.warn("Failed to process incoming packet on interface {}", Receiver.this.networkInterface.getName(), ex);
                    }
                }
            };

            while (this.running) {
                this.networkInterface.pcap.loop(Pcap.LOOP_INFINITE, jpacketHandler, null);
            }
        }
    }

    private final Logger logger = LoggerFactory.getLogger(NetworkInterface.class);
    private final RouterController routerController;
    private final String name;
    private final int mtu;
    private final MACAddress hardwareAddress;
    private IPv4InterfaceAddress ipv4InterfaceAddress;
    private boolean enabled = false;
    private IPv4Route route = null;

    private Pcap pcap;
    private final PcapIf pcapInterface;
    private Receiver receiver;
    private Thread receiverThread;

    protected NetworkInterfaceImpl(String name, int mtu, MACAddress hardwareAddress, PcapIf pcapInterface, RouterController routerController) {
        this.routerController = routerController;
        this.name = name;
        this.mtu = mtu;
        this.hardwareAddress = hardwareAddress;
        this.pcapInterface = pcapInterface;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getMTU() {
        return this.mtu;
    }

    @Override
    public MACAddress getHardwareAddress() {
        return this.hardwareAddress;
    }

    @Override
    public IPv4InterfaceAddress getIPv4InterfaceAddress() {
        return this.ipv4InterfaceAddress;
    }

    @Override
    public void setIPv4InterfaceAddress(IPv4InterfaceAddress interfaceAddress) {
        this.ipv4InterfaceAddress = interfaceAddress;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected void start() {
        if (this.enabled) {
            if (this.pcap == null) {
                StringBuilder errbuf = new StringBuilder();
                this.logger.debug("Starting packet receiver for interface " + this.pcapInterface.getName());
                int snaplen = 64 * 1024;
                int flags = Pcap.MODE_PROMISCUOUS;
                int timeout = 10 * 1000;
                this.pcap = Pcap.openLive(this.pcapInterface.getName(), snaplen, flags, timeout, errbuf);

                if (pcap == null) {
                    this.logger.error("Failed to open device for capture: " + errbuf.toString());
                    return;
                }
            }

            this.receiver = new Receiver();
            this.receiverThread = new Thread(this.receiver);
            this.receiverThread.start();

            this.installConnectedRoute();
        }
    }

    protected void stop() {
        if (this.receiverThread != null) {
            this.receiver.running = false;
            this.receiverThread = null;
            this.receiver = null;
            this.uninstallConnectedRoute();
        }
    }

    @Override
    public void sendPacket(Packet packet) {
        if (this.receiver != null) {
            this.pcap.sendPacket(packet.getPcapPacket());
        } else {
            this.logger.warn("Cannot transmit data over inactive interface");
        }
    }

    private void installConnectedRoute() {
        try {
            this.uninstallConnectedRoute();
            this.route = new IPv4Route() {

                IPv4RouteGateway gateway = new IPv4RouteGateway() {

                    @Override
                    public IPv4Address getGatewayAddress() {
                        return null;
                    }

                    @Override
                    public NetworkInterface getGatewayInterface() {
                        return (NetworkInterface) NetworkInterfaceImpl.this;
                    }

                    @Override
                    public boolean isAvailable() {
                        return NetworkInterfaceImpl.this.isEnabled();
                    }
                };
                private final IPv4RouteFlag connectedFlag = new IPv4RouteFlag("C", "Connected", "Network on directly connected interface");

                @Override
                public IPv4Prefix getTargetPrefix() {
                    return NetworkInterfaceImpl.this.getIPv4InterfaceAddress().getPrefix();
                }

                @Override
                public IPv4RouteGateway getNextGateway() {
                    return this.gateway;
                }

                @Override
                public IPv4RouteGateway[] getGateways() {
                    return new IPv4RouteGateway[]{this.gateway};
                }

                @Override
                public int getAdministrativeDistance() {
                    return 0;
                }

                @Override
                public IPv4RouteFlag[] getFlags() {
                    return new IPv4RouteFlag[]{this.connectedFlag};
                }
            };

            RoutingCoreModule routingCoreModule = this.routerController.getModule(RoutingCoreModule.class);
            routingCoreModule.installRoute(route);
        } catch (NoSuchModuleException ex) {
            this.logger.error("Failed to add connected route - routing core module is not available", ex);
        }
    }

    private void uninstallConnectedRoute() {
        if (this.route != null) {
            try {
                RoutingCoreModule routingCoreModule = this.routerController.getModule(RoutingCoreModule.class);
                routingCoreModule.uninstallRoute(this.route);
            } catch (NoSuchModuleException ex) {
                this.logger.error("Failed to remove connected route - routing core module is not available", ex);
            }
        }
    }
}

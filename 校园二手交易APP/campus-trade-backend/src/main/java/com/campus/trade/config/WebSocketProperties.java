package com.campus.trade.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    private String endpoint = "/ws";
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));
    private List<String> applicationDestinationPrefixes = new ArrayList<>(List.of("/app"));
    private List<String> simpleBrokerPrefixes = new ArrayList<>(List.of("/topic", "/queue"));
    private String userDestinationPrefix = "/user";
    private Heartbeat heartbeat = new Heartbeat();
    private Transport transport = new Transport();
    private ChannelPool inbound = new ChannelPool();
    private ChannelPool outbound = new ChannelPool();

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> getApplicationDestinationPrefixes() {
        return applicationDestinationPrefixes;
    }

    public void setApplicationDestinationPrefixes(List<String> applicationDestinationPrefixes) {
        this.applicationDestinationPrefixes = applicationDestinationPrefixes;
    }

    public List<String> getSimpleBrokerPrefixes() {
        return simpleBrokerPrefixes;
    }

    public void setSimpleBrokerPrefixes(List<String> simpleBrokerPrefixes) {
        this.simpleBrokerPrefixes = simpleBrokerPrefixes;
    }

    public String getUserDestinationPrefix() {
        return userDestinationPrefix;
    }

    public void setUserDestinationPrefix(String userDestinationPrefix) {
        this.userDestinationPrefix = userDestinationPrefix;
    }

    public Heartbeat getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Heartbeat heartbeat) {
        this.heartbeat = heartbeat;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public ChannelPool getInbound() {
        return inbound;
    }

    public void setInbound(ChannelPool inbound) {
        this.inbound = inbound;
    }

    public ChannelPool getOutbound() {
        return outbound;
    }

    public void setOutbound(ChannelPool outbound) {
        this.outbound = outbound;
    }

    public static class Heartbeat {
        private Duration server = Duration.ofSeconds(15);
        private Duration client = Duration.ofSeconds(15);

        public Duration getServer() {
            return server;
        }

        public void setServer(Duration server) {
            this.server = server;
        }

        public Duration getClient() {
            return client;
        }

        public void setClient(Duration client) {
            this.client = client;
        }
    }

    public static class Transport {
        private int messageSizeLimit = 64 * 1024;
        private int sendBufferSizeLimit = 512 * 1024;
        private int sendTimeLimit = 20_000;

        public int getMessageSizeLimit() {
            return messageSizeLimit;
        }

        public void setMessageSizeLimit(int messageSizeLimit) {
            this.messageSizeLimit = messageSizeLimit;
        }

        public int getSendBufferSizeLimit() {
            return sendBufferSizeLimit;
        }

        public void setSendBufferSizeLimit(int sendBufferSizeLimit) {
            this.sendBufferSizeLimit = sendBufferSizeLimit;
        }

        public int getSendTimeLimit() {
            return sendTimeLimit;
        }

        public void setSendTimeLimit(int sendTimeLimit) {
            this.sendTimeLimit = sendTimeLimit;
        }
    }

    public static class ChannelPool {
        private int corePoolSize = 4;
        private int maxPoolSize = 16;
        private int queueCapacity = 1000;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
}

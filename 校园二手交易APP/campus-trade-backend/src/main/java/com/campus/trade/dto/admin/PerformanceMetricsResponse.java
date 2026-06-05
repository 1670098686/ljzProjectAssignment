package com.campus.trade.dto.admin;

import java.util.ArrayList;
import java.util.List;

public class PerformanceMetricsResponse {

    private double httpRequestCount;
    private double httpRequestMaxMillis;
    private double httpRequestMeanMillis;
    private List<SlowEndpoint> slowestEndpoints = new ArrayList<>();

    public double getHttpRequestCount() {
        return httpRequestCount;
    }

    public void setHttpRequestCount(double httpRequestCount) {
        this.httpRequestCount = httpRequestCount;
    }

    public double getHttpRequestMaxMillis() {
        return httpRequestMaxMillis;
    }

    public void setHttpRequestMaxMillis(double httpRequestMaxMillis) {
        this.httpRequestMaxMillis = httpRequestMaxMillis;
    }

    public double getHttpRequestMeanMillis() {
        return httpRequestMeanMillis;
    }

    public void setHttpRequestMeanMillis(double httpRequestMeanMillis) {
        this.httpRequestMeanMillis = httpRequestMeanMillis;
    }

    public List<SlowEndpoint> getSlowestEndpoints() {
        return slowestEndpoints;
    }

    public void setSlowestEndpoints(List<SlowEndpoint> slowestEndpoints) {
        this.slowestEndpoints = slowestEndpoints;
    }

    public static class SlowEndpoint {
        private String uri;
        private double maxMillis;
        private double meanMillis;

        public SlowEndpoint() {
        }

        public SlowEndpoint(String uri, double maxMillis, double meanMillis) {
            this.uri = uri;
            this.maxMillis = maxMillis;
            this.meanMillis = meanMillis;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public double getMaxMillis() {
            return maxMillis;
        }

        public void setMaxMillis(double maxMillis) {
            this.maxMillis = maxMillis;
        }

        public double getMeanMillis() {
            return meanMillis;
        }

        public void setMeanMillis(double meanMillis) {
            this.meanMillis = meanMillis;
        }
    }
}

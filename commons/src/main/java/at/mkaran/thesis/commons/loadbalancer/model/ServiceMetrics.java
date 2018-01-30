package at.mkaran.thesis.commons.loadbalancer.model;

/**
 * POJO for metrics about a service scraped from Prometheus API
 */
public class ServiceMetrics {
    private double cpuUsage;

    public ServiceMetrics() {
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }
}
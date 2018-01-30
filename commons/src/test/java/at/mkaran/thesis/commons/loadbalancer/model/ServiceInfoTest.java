package at.mkaran.thesis.commons.loadbalancer.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the ServiceInfo POJO
 */
public class ServiceInfoTest {
    @Test
    /**
     * Tests if the nodeID is correctly extracted
     */
    public void extractNodeIdShouldReturnID() throws Exception {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNodeExServiceId("node-exporter-892xy-93");
        String nodeId = serviceInfo.extractNodeId();
        assertEquals("892xy-93", nodeId);
    }

    @Test
    /**
     * Tests if in case node-exporter service has a different name than configured, returns null
     */
    public void extractNodeIdShouldReturnNull() throws Exception {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNodeExServiceId("node-exporterekj-892xy-93");
        String nodeId = serviceInfo.extractNodeId();
        assertEquals(null, nodeId);
    }

}
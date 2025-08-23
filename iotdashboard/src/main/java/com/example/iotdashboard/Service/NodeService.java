package com.example.iotdashboard.Service;

import com.example.iotdashboard.dto.NodeDTO;
import com.example.iotdashboard.model.Node;
import com.example.iotdashboard.model.Reading;
import com.example.iotdashboard.repository.NodeRepository;
import com.example.iotdashboard.repository.ReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NodeService {

    private final NodeRepository nodeRepository;
    private final ReadingRepository readingRepository;

    public NodeService(NodeRepository nodeRepository, ReadingRepository readingRepository) {
        this.nodeRepository = nodeRepository;
        this.readingRepository = readingRepository;
    }

    public List<NodeDTO> getAllNodes() {
        List<Node> nodes = nodeRepository.findAll();
        List<NodeDTO> dtos = new ArrayList<>();
        
        for (Node n : nodes) {
            NodeDTO dto = mapToDTO(n);
            // Determine status based on last update time (5-minute threshold)
            if (n.getLastUpdated() != null && 
                n.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
                dto.setStatus("ONLINE");
            } else {
                dto.setStatus("OFFLINE");
            }
            dtos.add(dto);
        }
        return dtos;
    }

    public NodeDTO getNodeById(Long id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        NodeDTO dto = mapToDTO(node);
        
        // Determine status
        if (node.getLastUpdated() != null && 
            node.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
            dto.setStatus("ONLINE");
        } else {
            dto.setStatus("OFFLINE");
        }
        
        return dto;
    }

    public Map<String, Object> getCurrentNodeData(Long id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        
        // Get the latest reading
        List<Reading> latestReadings = readingRepository.findLatestByNodeId(
            id, PageRequest.of(0, 1));
        
        Reading latestReading = latestReadings.isEmpty() ? null : latestReadings.get(0);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", node.getId());
        response.put("name", node.getName());
        response.put("ipAddress", node.getIpAddress());
        response.put("macAddress", node.getMacAddress());
        response.put("firmwareVersion", node.getFirmwareVersion());
        response.put("location", node.getLocation());
        
        // Determine status
        if (node.getLastUpdated() != null && 
            node.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
            response.put("status", "ONLINE");
        } else {
            response.put("status", "OFFLINE");
        }
        
        // Use the latest reading if available, otherwise use node data
        if (latestReading != null) {
            response.put("temperature", latestReading.getTemperature());
            response.put("humidity", latestReading.getHumidity());
            response.put("pressure", latestReading.getPressure());
            response.put("light", latestReading.getLight());
            response.put("lastUpdated", latestReading.getTimestamp());
        } else {
            response.put("temperature", node.getTemperature());
            response.put("humidity", node.getHumidity());
            response.put("pressure", node.getPressure());
            response.put("light", node.getLight());
            response.put("lastUpdated", node.getLastUpdated());
        }
        
        return response;
    }

    private NodeDTO mapToDTO(Node node) {
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setIpAddress(node.getIpAddress());
        dto.setMacAddress(node.getMacAddress());
        dto.setFirmwareVersion(node.getFirmwareVersion());
        dto.setLocation(node.getLocation());
        dto.setTemperature(node.getTemperature());
        dto.setHumidity(node.getHumidity());
        dto.setPressure(node.getPressure());
        dto.setLight(node.getLight());
        dto.setLastUpdated(node.getLastUpdated());
        return dto;
    }

    public Map<String, Integer> getStats() {
        List<NodeDTO> nodes = getAllNodes();
        int total = nodes.size();
        int online = (int) nodes.stream().filter(n -> "ONLINE".equals(n.getStatus())).count();
        int offline = total - online;
        
        Map<String, Integer> stats =  new HashMap<>();
        stats.put("total", total);
        stats.put("online", online);
        stats.put("offline", offline);
        return stats;
    }

    public Node saveNode(Node node) {
        return nodeRepository.save(node);
    }

    public Reading saveReading(Long nodeId, Reading reading) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        reading.setNode(node);
        return readingRepository.save(reading);
    }

    public List<Reading> getHistory(Long nodeId, int limit) {
        return readingRepository.findByNode_IdOrderByTimestampDesc(nodeId, PageRequest.of(0, Math.max(1, limit)));
    }
    
    public Node findOrCreateNode(String macAddress, String ipAddress) {
        return nodeRepository.findByMacAddress(macAddress)
                .orElseGet(() -> {
                    Node newNode = new Node();
                    newNode.setName("ESP32 Node " + macAddress.substring(Math.max(0, macAddress.length() - 4)));
                    newNode.setMacAddress(macAddress);
                    newNode.setIpAddress(ipAddress);
                    newNode.setLastUpdated(LocalDateTime.now());
                    newNode.setStatus("ONLINE");
                    return nodeRepository.save(newNode);
                });
    }

    // New method to handle ESP32 sensor data
    public void processSensorData(int nodeId, Double temperature, Double humidity, 
                                 Double pressure, Double light, String ipAddress, String macAddress) {
        try {
            // Generate consistent MAC and IP if not provided
            String finalMacAddress = macAddress != null ? 
                macAddress : String.format("00:1A:2B:3C:%02X:00", nodeId);
            
            String finalIpAddress = ipAddress != null ? 
                ipAddress : String.format("192.168.1.%d", nodeId + 100);

            // Find or create node
            Node node = findOrCreateNode(finalMacAddress, finalIpAddress);
            
            // Update node information
            if (node.getName() == null) {
                node.setName("Node " + nodeId);
            }
            
            node.setLastUpdated(LocalDateTime.now());
            node.setTemperature(temperature);
            node.setHumidity(humidity);
            node.setPressure(pressure);
            node.setLight(light);
            node.setStatus("ONLINE");
            saveNode(node);

            // Create and save reading record
            Reading reading = new Reading();
            reading.setNode(node);
            reading.setTemperature(temperature);
            reading.setHumidity(humidity);
            reading.setPressure(pressure);
            reading.setLight(light);
            saveReading(node.getId(), reading);
            
        } catch (Exception e) {
            throw new RuntimeException("Error processing sensor data: " + e.getMessage(), e);
        }
    }

    /**
     * NEW: Process ESP-NOW CSV data directly
     * Format: "nodeId,temperature,humidity,pressure,light"
     * Example: "4,29.1,55.8,1011.5,25.7"
     */
    public void processEspNowCsvData(String csvData) {
        try {
            String[] parts = csvData.split("\\s*,\\s*");
            if (parts.length < 5) {
                throw new IllegalArgumentException("Expected 5 fields in CSV data: nodeId,temperature,humidity,pressure,light");
            }

            int nodeId = Integer.parseInt(parts[0]);
            Double temperature = Double.valueOf(parts[1]);
            Double humidity = Double.valueOf(parts[2]);
            Double pressure = Double.valueOf(parts[3]);
            Double light = Double.valueOf(parts[4]);

            // Use the existing processSensorData method
            processSensorData(nodeId, temperature, humidity, pressure, light, null, null);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in CSV data", e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing ESP-NOW CSV data: " + e.getMessage(), e);
        }
    }

    // Method to get node by MAC address
    public Optional<Node> getNodeByMacAddress(String macAddress) {
        return nodeRepository.findByMacAddress(macAddress);
    }

    // Method to update node status based on last update time
    public void updateNodeStatuses() {
        List<Node> nodes = nodeRepository.findAll();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        
        for (Node node : nodes) {
            if (node.getLastUpdated() != null && node.getLastUpdated().isAfter(threshold)) {
                node.setStatus("ONLINE");
            } else {
                node.setStatus("OFFLINE");
            }
            nodeRepository.save(node);
        }
    }

    // Method to get nodes by status
    public List<NodeDTO> getNodesByStatus(String status) {
        List<NodeDTO> allNodes = getAllNodes();
        List<NodeDTO> filteredNodes = new ArrayList<>();
        
        for (NodeDTO node : allNodes) {
            if (status.equalsIgnoreCase(node.getStatus())) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    // Method to get latest readings for all nodes
    public Map<Long, Reading> getLatestReadingsForAllNodes() {
        Map<Long, Reading> latestReadings = new HashMap<>();
        List<Node> nodes = nodeRepository.findAll();
        
        for (Node node : nodes) {
            List<Reading> readings = readingRepository.findLatestByNodeId(
                node.getId(), PageRequest.of(0, 1));
            if (!readings.isEmpty()) {
                latestReadings.put(node.getId(), readings.get(0));
            }
        }
        return latestReadings;
    }
}
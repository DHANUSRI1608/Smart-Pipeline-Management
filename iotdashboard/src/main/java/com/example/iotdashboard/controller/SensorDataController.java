package com.example.iotdashboard.controller;

import com.example.iotdashboard.dto.SensorData;
import com.example.iotdashboard.Service.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
public class SensorDataController {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataController.class);
    
    private final NodeService nodeService;

    public SensorDataController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    /**
     * Quick connectivity test
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Sensor API is working!");
    }

    /**
     * Handles normal sensor data.
     */
    @PostMapping("/data")
    public ResponseEntity<String> receiveSensorData(@RequestBody SensorData data) {
        try {
            logger.info("Received normal sensor data: {}", data);
            processSensorData(data);
            return ResponseEntity.ok("Data received successfully");
        } catch (Exception e) {
            logger.error("Error processing sensor data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing data: " + e.getMessage());
        }
    }

    /**
     * Handles ESP-NOW specific format - supports both JSON and CSV styles.
     */
    @PostMapping("/espnow")
    public ResponseEntity<?> receiveEspNowData(@RequestBody Map<String, Object> payload) {
        try {
            logger.info("Received ESP-NOW payload: {}", payload);
            
            // Check if it's CSV format
            if (payload.containsKey("data") && payload.get("data") instanceof String) {
                String csvData = (String) payload.get("data");
                return processCsvData(csvData);
            } else {
                // Structured JSON format
                return processStructuredJson(payload);
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ESP-NOW data format: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid data format: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing ESP-NOW data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing ESP-NOW data: " + e.getMessage());
        }
    }

    private ResponseEntity<?> processCsvData(String csvData) {
        if (!StringUtils.hasText(csvData)) {
            return ResponseEntity.badRequest().body("Empty CSV data");
        }
        
        String[] parts = csvData.split("\\s*,\\s*");
        if (parts.length < 5) {
            return ResponseEntity.badRequest().body("Expected 5 fields: nodeId,temperature,humidity,pressure,light");
        }

        try {
            nodeService.processEspNowCsvData(csvData);
            return ResponseEntity.ok("ESP-NOW CSV data received successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing CSV data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing CSV data: " + e.getMessage());
        }
    }

    private ResponseEntity<?> processStructuredJson(Map<String, Object> payload) {
        try {
            SensorData data = new SensorData();
            
            // Support both "nodeId" and "node_id"
            Object nodeIdObj = payload.containsKey("nodeId") ? payload.get("nodeId") : payload.get("node_id");
            if (nodeIdObj == null) {
                return ResponseEntity.badRequest().body("Missing nodeId");
            }
            data.setNodeId(((Number) nodeIdObj).intValue());

            // Extract values safely
            data.setTemperature(parseDouble(payload.get("temperature")));
            data.setHumidity(parseDouble(payload.get("humidity")));
            data.setPressure(parseDouble(payload.get("pressure")));
            data.setLight(parseDouble(payload.get("light")));
            
            if (payload.containsKey("ipAddress")) {
                data.setIpAddress((String) payload.get("ipAddress"));
            }
            if (payload.containsKey("macAddress")) {
                data.setMacAddress((String) payload.get("macAddress"));
            }

            logger.info("Processed ESP-NOW structured JSON: {}", data);
            processSensorData(data);
            return ResponseEntity.ok("ESP-NOW JSON data received successfully");
        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid JSON structure: " + e.getMessage());
        }
    }

    private Double parseDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert to double: " + value);
            }
        }
        throw new IllegalArgumentException("Cannot convert to double: " + value);
    }

    private void processSensorData(SensorData data) {
        nodeService.processSensorData(
            data.getNodeId(),
            data.getTemperature(),
            data.getHumidity(),
            data.getPressure(),
            data.getLight(),
            data.getIpAddress(),
            data.getMacAddress()
        );
    }
}

package com.example.iotdashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorData {
    @JsonProperty("id") // This maps JSON "id" field to nodeId
    private int nodeId;
    
    private Double temperature;
    private Double humidity;
    private Long timestamp;
    
    // Optional fields
    private Double pressure;
    private Double light;
    private String ipAddress;
    private String macAddress;

    // Getters and Setters
    public int getNodeId() { return nodeId; }
    public void setNodeId(int nodeId) { this.nodeId = nodeId; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public Double getPressure() { return pressure; }
    public void setPressure(Double pressure) { this.pressure = pressure; }
    public Double getLight() { return light; }
    public void setLight(Double light) { this.light = light; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    @Override
    public String toString() {
        return "SensorData{" +
                "nodeId=" + nodeId +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", timestamp=" + timestamp +
                ", pressure=" + pressure +
                ", light=" + light +
                ", ipAddress='" + ipAddress + '\'' +
                ", macAddress='" + macAddress + '\'' +
                '}';
    }
}
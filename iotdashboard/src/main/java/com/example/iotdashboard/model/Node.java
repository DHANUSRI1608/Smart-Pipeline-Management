package com.example.iotdashboard.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "nodes")
public class Node {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String ipAddress;
    private String macAddress;
    private String firmwareVersion;
    private String location;

    private Double temperature;
    private Double humidity;
    private Double pressure;
    private Double light;

    private String status;

    private LocalDateTime lastUpdated;
}
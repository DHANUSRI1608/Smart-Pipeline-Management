package com.example.iotdashboard.repository;

import com.example.iotdashboard.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NodeRepository extends JpaRepository<Node, Long> {
    Optional<Node> findByMacAddress(String macAddress);
}
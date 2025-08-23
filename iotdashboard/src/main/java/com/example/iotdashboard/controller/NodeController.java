package com.example.iotdashboard.controller;

import com.example.iotdashboard.dto.NodeDTO;
import com.example.iotdashboard.model.Reading;
import com.example.iotdashboard.Service.NodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("/nodes")
    public List<NodeDTO> getAllNodes() {
        return nodeService.getAllNodes();
    }

    @GetMapping("/nodes/{id}")
    public ResponseEntity<NodeDTO> getNode(@PathVariable Long id) {
        try {
            NodeDTO node = nodeService.getNodeById(id);
            return ResponseEntity.ok(node);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/nodes/{id}/readings")
    public List<Reading> getNodeReadings(@PathVariable Long id,
                                         @RequestParam(defaultValue = "30") int limit) {
        return nodeService.getHistory(id, limit);
    }

    @GetMapping("/nodes/{id}/current")
    public ResponseEntity<Map<String, Object>> getCurrentNodeData(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(nodeService.getCurrentNodeData(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public Object getStats() {
        return nodeService.getStats();
    }
}
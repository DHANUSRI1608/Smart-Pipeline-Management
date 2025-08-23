package com.example.iotdashboard.repository;

import com.example.iotdashboard.model.Reading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReadingRepository extends JpaRepository<Reading, Long> {
    List<Reading> findByNode_IdOrderByTimestampDesc(Long nodeId, Pageable pageable);
    
    @Query("SELECT r FROM Reading r WHERE r.node.id = :nodeId ORDER BY r.timestamp DESC")
    List<Reading> findLatestByNodeId(@Param("nodeId") Long nodeId, Pageable pageable);
}
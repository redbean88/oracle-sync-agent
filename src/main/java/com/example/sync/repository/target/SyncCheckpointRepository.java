package com.example.sync.repository.target;

import com.example.sync.entity.target.SyncCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncCheckpointRepository extends JpaRepository<SyncCheckpoint, String> {
}

package com.example.sync.repository.proxy;

import com.example.sync.domain.proxy.SyncCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncCheckpointRepository extends JpaRepository<SyncCheckpoint, String> {
}

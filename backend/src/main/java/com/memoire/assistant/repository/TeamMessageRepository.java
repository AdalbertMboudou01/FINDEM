package com.memoire.assistant.repository;

import com.memoire.assistant.model.TeamMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamMessageRepository extends JpaRepository<TeamMessage, UUID> {
    List<TeamMessage> findByChannel_ChannelIdOrderByCreatedAtAsc(UUID channelId);
    List<TeamMessage> findByChannel_ChannelIdOrderByCreatedAtDesc(UUID channelId, Pageable pageable);
    TeamMessage findTopByChannel_ChannelIdOrderByCreatedAtDesc(UUID channelId);
}

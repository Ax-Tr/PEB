package com.paywithease.commitment.infrastructure;

import com.paywithease.commitment.domain.CommitmentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitmentEventRepository extends JpaRepository<CommitmentEvent, String> {

  List<CommitmentEvent> findByCommitmentIdOrderByOccurredAtDesc(String commitmentId);
}

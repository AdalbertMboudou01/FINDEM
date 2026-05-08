package com.memoire.assistant.repository;

import com.memoire.assistant.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByApplication_ApplicationIdAndCompanyIdOrderByCreatedAtAsc(UUID applicationId, UUID companyId);

    List<Comment> findByApplication_ApplicationIdAndCompanyIdAndVisibilityOrderByCreatedAtAsc(
            UUID applicationId, UUID companyId, Comment.Visibility visibility);

    boolean existsByApplication_ApplicationIdAndAuthorId(UUID applicationId, UUID authorId);

    java.util.Optional<Comment> findByApplication_ApplicationIdAndAuthorId(UUID applicationId, UUID authorId);
}

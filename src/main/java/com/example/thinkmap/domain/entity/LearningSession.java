package com.example.thinkmap.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_session", indexes = {
        @Index(name = "idx_learning_session_updated_at", columnList = "updated_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThoughtNode> nodes = new ArrayList<>();

    @Builder
    public LearningSession(String title) {
        this.title = title;
    }

    /**
     * 노드 추가/변경 시 호출하여 updatedAt을 갱신한다.
     * @UpdateTimestamp는 엔티티 자체 필드가 변경될 때만 자동 발동하므로
     * 연관 노드 변경 후 session을 merge할 때 이 메서드로 dirty 상태를 만든다.
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 세션 제목을 변경한다. 변경 시 updatedAt도 자동 갱신된다.
     */
    public void updateTitle(String title) {
        this.title = title;
    }
}

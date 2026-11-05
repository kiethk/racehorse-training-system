package com.rtms.backend.stable.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One "Complete" action of a Groom. It may cover several routine tasks (the same duty at the same time for
 * several horses), and its note and photos describe all of them.
 */
@Entity
@Table(name = "groom_task_completions")
public class GroomTaskCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "groom_id", nullable = false)
    private Long groomId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    /** Storage keys, in upload order. Never sent to clients: they load each photo from its endpoint. */
    @JsonIgnore
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "groom_task_completion_photos", joinColumns = @JoinColumn(name = "completion_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_key", nullable = false, length = 500)
    private List<String> photoKeys = new ArrayList<>();

    public GroomTaskCompletion() {
    }

    public GroomTaskCompletion(Long groomId, String note, LocalDateTime completedAt) {
        this.groomId = groomId;
        this.note = note;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroomId() {
        return groomId;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public List<String> getPhotoKeys() {
        return photoKeys;
    }

    public void setPhotoKeys(List<String> photoKeys) {
        this.photoKeys = photoKeys;
    }
}

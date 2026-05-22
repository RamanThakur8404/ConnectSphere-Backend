package com.connectsphere.search_service.entity;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
    name = "hashtags",
    uniqueConstraints = @UniqueConstraint(columnNames = "tag"),
    indexes = {
        @Index(name = "idx_hashtag_post_count", columnList = "postCount DESC"),
        @Index(name = "idx_hashtag_last_used",  columnList = "lastUsedAt DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"postHashtags"})
@EqualsAndHashCode(of = "hashtagId")
public class Hashtag {

    // Primary key — auto-generated. 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id")
    private Integer hashtagId;

    // Lowercase hashtag text without the '#' symbol.
    @Column(name = "tag", nullable = false, unique = true, length = 100)
    private String tag;

    // Total number of posts that have used this hashtag.
    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private Integer postCount = 0;

    // Timestamp of the most recent post that used this hashtag. 
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // Bidirectional mapping to PostHashtag join records (lazy-loaded). 
    @OneToMany(mappedBy = "hashtag", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PostHashtag> postHashtags = new ArrayList<>();
}

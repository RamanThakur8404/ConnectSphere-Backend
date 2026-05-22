package com.connectsphere.search_service.serviceimpl;

import com.connectsphere.search_service.constant.ErrorMessages;
import com.connectsphere.search_service.constant.LogMessages;
import com.connectsphere.search_service.dto.*;
import com.connectsphere.search_service.entity.Hashtag;
import com.connectsphere.search_service.entity.PostHashtag;
import com.connectsphere.search_service.exception.*;
import com.connectsphere.search_service.repository.PostHashtagRepository;
import com.connectsphere.search_service.repository.SearchRepository;
import com.connectsphere.search_service.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Implementation of {@link SearchService}.
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SearchServiceImpl implements SearchService {

    private final SearchRepository     searchRepository;
    private final PostHashtagRepository postHashtagRepository;

    @Value("${app.trending.top-n:10}")
    private int trendingTopN;

    @Value("${app.trending.window-hours:24}")
    private int trendingWindowHours;

    private static final Pattern HASHTAG_PATTERN =
            Pattern.compile("#(\\w{1,100})");

    @Override
    public List<String> indexPost(Integer postId, String content, List<String> providedHashtags) {
        log.debug(LogMessages.INDEX_POST_ATTEMPT, postId);

        removePostIndexInternal(postId);

        List<String> tags = extractTags(content, providedHashtags);
        if (tags.isEmpty()) {
            log.debug(LogMessages.INDEX_POST_NO_TAGS, postId);
            return Collections.emptyList();
        }

        for (String tag : tags) {
            log.debug(LogMessages.INDEX_POST_UPSERT_TAG, tag, postId);
            Hashtag hashtag = upsertHashtag(tag);
            createPostHashtagMapping(postId, hashtag);
        }

        log.info(LogMessages.INDEX_POST_SUCCESS, tags.size(), postId, tags);
        return tags;
    }

    @Override
    public void removePostIndex(Integer postId) {
    	 log.info(LogMessages.REMOVE_INDEX_ATTEMPT, postId);
         removePostIndexInternal(postId);
         log.info(LogMessages.REMOVE_INDEX_SUCCESS, postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> searchPosts(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        String normalised = keyword.startsWith("#")
                ? keyword.substring(1).toLowerCase()
                : keyword.toLowerCase();

        log.info(LogMessages.SEARCH_POSTS_ATTEMPT, normalised);

        List<Hashtag> matchingHashtags = searchRepository.searchByTagContaining(normalised);
        if (matchingHashtags.isEmpty()) {
            log.debug(LogMessages.SEARCH_POSTS_EMPTY, normalised);
            return Collections.emptyList();
        }

        Set<Integer> postIds = new LinkedHashSet<>();
        for (Hashtag h : matchingHashtags) {
            postIds.addAll(postHashtagRepository.findPostsByHashtag(h.getTag()));
        }

        log.info(LogMessages.SEARCH_POSTS_SUCCESS, postIds.size(), normalised);
        return new ArrayList<>(postIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> searchUsers(String query) {
        log.info(LogMessages.SEARCH_USERS_ATTEMPT, query);
        // Stub — user search is delegated to auth-service via Elasticsearch in prod
        log.info(LogMessages.SEARCH_USERS_SUCCESS, query, 0);
        return Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HashtagResponseDto> getHashtagsForPost(Integer postId) {
        log.info(LogMessages.HASHTAG_FETCH_ATTEMPT, postId);
        List<HashtagResponseDto> result = postHashtagRepository.findByPostId(postId)
                .stream()
                .map(ph -> toHashtagResponse(ph.getHashtag()))
                .collect(Collectors.toList());
        log.info(LogMessages.HASHTAG_FETCH_SUCCESS, result.size(), postId);
        return result;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<HashtagSummaryDto> getTrendingHashtags() {
        LocalDateTime since = LocalDateTime.now().minusHours(trendingWindowHours);
        log.info(LogMessages.TRENDING_ATTEMPT, trendingTopN, since);

        List<HashtagSummaryDto> result = searchRepository
                .findTrendingHashtags(since, PageRequest.of(0, trendingTopN))
                .stream()
                .map(this::toHashtagSummary)
                .collect(Collectors.toList());

        log.info(LogMessages.TRENDING_SUCCESS, result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getPostsByHashtag(String tag) {
        String normalised = normaliseTag(tag);
        log.info(LogMessages.HASHTAG_POSTS_ATTEMPT, normalised);

        if (searchRepository.findByTag(normalised).isEmpty()) {
            throw new HashtagNotFoundException(ErrorMessages.HASHTAG_NOT_FOUND + normalised);
        }

        List<Integer> postIds = postHashtagRepository.findPostsByHashtag(normalised);
        log.info(LogMessages.HASHTAG_POSTS_SUCCESS, postIds.size(), normalised);
        return postIds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HashtagResponseDto> searchHashtags(String fragment) {
        if (!StringUtils.hasText(fragment)) {
            return Collections.emptyList();
        }
        String normalised = fragment.toLowerCase().replace("#", "");
        log.info(LogMessages.HASHTAG_SEARCH_ATTEMPT, normalised);

        List<HashtagResponseDto> result = searchRepository.searchByTagContaining(normalised)
                .stream()
                .map(this::toHashtagResponse)
                .collect(Collectors.toList());

        log.info(LogMessages.HASHTAG_SEARCH_SUCCESS, result.size(), normalised);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getHashtagCount(String tag) {
        String normalised = normaliseTag(tag);
        log.info(LogMessages.HASHTAG_COUNT_ATTEMPT, normalised);
        Integer count = searchRepository.countPostsByHashtag(normalised);
        int result = count != null ? count : 0;
        log.info(LogMessages.HASHTAG_COUNT_SUCCESS, normalised, result);
        return result;
    }

    // Private helpers

    private List<String> extractTags(String content, List<String> providedHashtags) {
        Set<String> tags = new LinkedHashSet<>();
        if (StringUtils.hasText(content)) {
            Matcher matcher = HASHTAG_PATTERN.matcher(content);
            while (matcher.find()) {
                tags.add(normaliseTagValue(matcher.group(1)));
            }
        }
        if (providedHashtags != null) {
            providedHashtags.stream()
                    .map(this::normaliseTagValue)
                    .filter(StringUtils::hasText)
                    .forEach(tags::add);
        }
        return new ArrayList<>(tags);
    }

    private String normaliseTagValue(String tag) {
        if (!StringUtils.hasText(tag)) {
            return "";
        }
        return tag.trim().toLowerCase().replaceFirst("^#", "").replaceAll("[^\\p{L}\\p{N}_]", "");
    }

    private Hashtag upsertHashtag(String tag) {
        return searchRepository.findByTag(tag)
                .map(existing -> {
                    existing.setPostCount(existing.getPostCount() + 1);
                    existing.setLastUsedAt(LocalDateTime.now());
                    return searchRepository.save(existing);
                })
                .orElseGet(() -> {
                    Hashtag newTag = Hashtag.builder()
                            .tag(tag)
                            .postCount(1)
                            .lastUsedAt(LocalDateTime.now())
                            .build();
                    return searchRepository.save(newTag);
                });
    }

    private void createPostHashtagMapping(Integer postId, Hashtag hashtag) {
        boolean exists = postHashtagRepository
                .existsByPostIdAndHashtagHashtagId(postId, hashtag.getHashtagId());
        if (!exists) {
            PostHashtag mapping = PostHashtag.builder()
                    .postId(postId)
                    .hashtag(hashtag)
                    .createdAt(LocalDateTime.now())
                    .build();
            postHashtagRepository.save(mapping);
        }
    }

    private void removePostIndexInternal(Integer postId) {
        List<PostHashtag> existingMappings = postHashtagRepository.findByPostId(postId);
        for (PostHashtag ph : existingMappings) {
            Hashtag hashtag = ph.getHashtag();
            hashtag.setPostCount(Math.max(0, hashtag.getPostCount() - 1));
            searchRepository.save(hashtag);
        }
        postHashtagRepository.deleteByPostId(postId);
    }

    private String normaliseTag(String tag) {
        if (!StringUtils.hasText(tag)) {
            throw new InvalidSearchQueryException(ErrorMessages.HASHTAG_BLANK);
        }
        return tag.trim().toLowerCase().replaceFirst("^#", "");
    }

    private HashtagResponseDto toHashtagResponse(Hashtag h) {
        return HashtagResponseDto.builder()
                .hashtagId(h.getHashtagId())
                .tag(h.getTag())
                .postCount(h.getPostCount())
                .lastUsedAt(h.getLastUsedAt())
                .build();
    }

    private HashtagSummaryDto toHashtagSummary(Hashtag h) {
        return HashtagSummaryDto.builder()
                .tag(h.getTag())
                .postCount(h.getPostCount())
                .lastUsedAt(h.getLastUsedAt())
                .build();
    }
}

package com.connectsphere.post_service.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HashtagExtractor {

    private static final int MAX_TAGS = 4;
    private static final Pattern EXPLICIT_HASHTAG_PATTERN = Pattern.compile("#([\\p{L}\\p{N}_]{1,100})");

    public List<String> resolveTags(String content, List<String> mediaUrls) {
        Set<String> tags = new LinkedHashSet<>();
        String normalizedContent = content == null ? "" : content.toLowerCase(Locale.ROOT);

        Matcher matcher = EXPLICIT_HASHTAG_PATTERN.matcher(content == null ? "" : content);
        while (matcher.find()) {
            addTag(tags, matcher.group(1));
        }

        addRuleBasedTags(tags, normalizedContent, mediaUrls);

        if (tags.isEmpty()) {
            tags.add("connectsphere");
        }

        return new ArrayList<>(tags).stream().limit(MAX_TAGS).toList();
    }

    private void addRuleBasedTags(Set<String> tags, String content, List<String> mediaUrls) {
        if (containsAny(content, "java", "spring", "backend", "microservice", "api", "database", "mysql")) {
            addTag(tags, "backend");
        }
        if (containsAny(content, "react", "frontend", "ui", "design", "dashboard", "component")) {
            addTag(tags, "frontend");
        }
        if (containsAny(content, "login", "auth", "oauth", "password", "security", "jwt")) {
            addTag(tags, "auth");
        }
        if (containsAny(content, "ai", "gemini", "openai", "analysis", "moderation")) {
            addTag(tags, "ai");
        }
        if (containsAny(content, "payment", "razorpay", "refund", "subscription", "checkout")) {
            addTag(tags, "payments");
        }
        if (containsAny(content, "message", "chat", "notification", "realtime", "websocket")) {
            addTag(tags, "community");
        }
        if ((mediaUrls != null && !mediaUrls.isEmpty()) || containsAny(content, "photo", "image", "video", "media", "cloudinary")) {
            addTag(tags, "media");
        }
        if (containsAny(content, "bug", "fix", "error", "issue", "problem")) {
            addTag(tags, "devlog");
        }
    }

    private boolean containsAny(String source, String... words) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        for (String word : words) {
            if (source.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private void addTag(Set<String> tags, String rawTag) {
        if (!StringUtils.hasText(rawTag) || tags.size() >= MAX_TAGS) {
            return;
        }
        String normalized = rawTag.toLowerCase(Locale.ROOT)
                .replaceFirst("^#", "")
                .replaceAll("[^\\p{L}\\p{N}_]", "");
        if (StringUtils.hasText(normalized)) {
            tags.add(normalized);
        }
    }
}

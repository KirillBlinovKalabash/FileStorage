package com.filestorage.demo.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class TagService {
    private final Set<String> allowedTags = Set.of("document", "image", "video", "backup");

    public boolean isValidTag(String tag) {
        return allowedTags.contains(tag.toLowerCase());
    }

    public Set<String> getAllowedTags() {
        return allowedTags;
    }
}

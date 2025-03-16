package com.filestorage.demo;

import com.filestorage.demo.service.TagService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TagServiceTest {
    private final TagService tagService = new TagService();

    @Test
    void shouldReturnTrue_WhenTagIsValid() {
        assertTrue(tagService.isValidTag("document"));
        assertTrue(tagService.isValidTag("IMAGE"));  // Case insensitive check
        assertTrue(tagService.isValidTag("video"));
    }

    @Test
    void shouldReturnFalse_WhenTagIsInvalid() {
        assertFalse(tagService.isValidTag("music"));
        assertFalse(tagService.isValidTag("randomTag"));
        assertFalse(tagService.isValidTag("doc"));  // Partial match should fail
    }

    @Test
    void shouldReturnAllowedTags() {
        Set<String> expectedTags = Set.of("document", "image", "video", "backup");
        assertEquals(expectedTags, tagService.getAllowedTags());
    }
}

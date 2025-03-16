package com.filestorage.demo.controller;

import com.filestorage.demo.service.TagService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/tags")
@AllArgsConstructor
public class TagsController {

    TagService tagService;

    @GetMapping("")
    public ResponseEntity<Set<String>> getTagsList() {
        return ResponseEntity.ok().body(tagService.getAllowedTags());
    }
}

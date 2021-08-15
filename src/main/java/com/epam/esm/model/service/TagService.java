package com.epam.esm.model.service;

import com.epam.esm.model.entity.Tag;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface TagService {
    Optional<Tag> create(Tag tag);

    Optional<Tag> findTag(Long id);

    List<Tag> findAllTag();

    boolean deleteTag(Long id);

    Page<Tag> findTags(int page, int size);
}

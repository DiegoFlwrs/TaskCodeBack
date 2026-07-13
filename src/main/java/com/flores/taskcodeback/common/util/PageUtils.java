package com.flores.taskcodeback.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageUtils {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(5, 10, 50, 100);
    public static final int DEFAULT_SIZE = 5;

    private PageUtils() {
    }

    public static int normalizeSize(Integer size) {
        if (size == null || !ALLOWED_SIZES.contains(size)) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    public static int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    public static Pageable of(Integer page, Integer size, Sort sort) {
        return PageRequest.of(normalizePage(page), normalizeSize(size), sort);
    }
}

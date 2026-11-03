package com.tradeguard.common;

import org.springframework.data.domain.Pageable;

import java.util.Set;

public final class PageableSortValidator {

    private PageableSortValidator() {
    }

    public static void requireAllowed(Pageable pageable, Set<String> allowedProperties) {
        pageable.getSort().forEach(order -> {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new InvalidRequestException("不允許使用此排序欄位。");
            }
        });
    }
}

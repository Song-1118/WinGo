package com.wingo.model;

import java.util.List;

public record AppItem(String name, String strategy, String primaryId
        , String keyword, List<String> candidateIds, String fallbackUrl) {
}

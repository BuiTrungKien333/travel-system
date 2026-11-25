package com.travel.media.dto;

import java.util.List;

/** Index refers to the submitted files order; one failure does not hide successful uploads. */
public record BatchUploadResponse(List<Item> items, int succeeded, int failed) {
    public record Item(int index, UploadResponse media, String errorCode, Integer errorStatus) {}
}

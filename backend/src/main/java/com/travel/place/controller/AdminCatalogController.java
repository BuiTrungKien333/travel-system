package com.travel.place.controller;

import com.travel.common.response.ApiResponse;
import com.travel.media.dto.UploadResponse;
import com.travel.media.service.MediaService;
import com.travel.place.service.AdminCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final AdminCatalogService service;
    private final MediaService mediaService;

    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadResponse> uploadPlaceImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Place image uploaded", mediaService.uploadPlaceImage(file));
    }

    @GetMapping
    public ApiResponse<AdminCatalogService.Page> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Pattern(regexp = "\\d{2}") String provinceCode,
            @RequestParam(defaultValue = "ALL") @Pattern(regexp = "(?i)ALL|PENDING|APPROVED|ARCHIVED") String status,
            @RequestParam(required = false) @Pattern(regexp = "[A-Za-z0-9_]{2,30}") String tag,
            @RequestParam(required = false) @Size(max = 200) String q) {
        return ApiResponse.success(service.list(page, size, provinceCode, status, tag, q));
    }

    @GetMapping("/provinces")
    public ApiResponse<List<AdminCatalogService.ProvinceOption>> provinces() {
        return ApiResponse.success(service.provinces());
    }

    @PostMapping
    public ApiResponse<AdminCatalogService.Entry> create(@Valid @RequestBody AdminCatalogService.PlaceRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCatalogService.Entry> update(@PathVariable UUID id,
            @RequestParam @Min(1) int version, @Valid @RequestBody AdminCatalogService.PlaceRequest request) {
        return ApiResponse.success(service.update(id, request, version));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> review(@PathVariable UUID id, @Valid @RequestBody AdminCatalogService.Review request) {
        service.review(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<AdminCatalogService.Entry> archive(@PathVariable UUID id,
            @RequestParam @Min(1) int version) {
        return ApiResponse.success(service.archive(id, version));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<AdminCatalogService.Entry> restore(@PathVariable UUID id,
            @RequestParam @Min(1) int version) {
        return ApiResponse.success(service.restore(id, version));
    }
}

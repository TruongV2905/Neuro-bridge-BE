package com.nqt.workspace_service.controller;

import com.nqt.workspace_service.constant.FileType;
import com.nqt.workspace_service.service.interfaces.IFileTypeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/file-types")
@RequiredArgsConstructor
public class FileTypeController {
    private final IFileTypeService fileTypeService;

    @GetMapping
    @Operation(summary = "Lấy danh sách loại file hợp lệ")
    public ResponseEntity<List<String>> getFileTypes() {
        return ResponseEntity.ok(fileTypeService.getAllFileTypes());
    }
}

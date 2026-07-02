package com.nqt.workspace_service.controllers;

import com.nqt.workspace_service.enums.FileType;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/file-types")
public class FileTypeController {

    @GetMapping
    @Operation(summary = "Lấy danh sách loại file hợp lệ")
    public ResponseEntity<List<String>> getFileTypes() {
        List<String> types = Arrays.stream(FileType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }
}

package com.nqt.workspace_service.service.implement;

import com.nqt.workspace_service.constant.FileType;
import com.nqt.workspace_service.service.interfaces.IFileTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileTypeService implements IFileTypeService {
    @Override
    public List<String> getAllFileTypes() {
        return Arrays.stream(FileType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}

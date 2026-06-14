package com.nqt.common_starter.util;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.mapper.ExcelRowMapper;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@NoArgsConstructor
public class ExcelUtil {
    private static final Set<String> EXCEL_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-excel" // .xls
    );
    public static String getCellValue(
            Row row,
            DataFormatter formatter,
            Map<String, Integer> headerMap,
            String headerName
    ) {
        Integer columnIndex = headerMap.get(normalizeHeader(headerName));

        if (columnIndex == null) {
            return "";
        }

        Cell cell = row.getCell(columnIndex);
        return getCellValue(cell, formatter);
    }

    public static String getCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }
    private static Map<String, Integer> getHeaderIndexMap(
            Sheet sheet,
            DataFormatter formatter
    ) {
        Row headerRow = sheet.getRow(0);

        if (headerRow == null) {
            throw new GlobalException("FIle "+ ErrorCode.INVALID_KEY.getMessage());
        }

        Map<String, Integer> headerMap = new HashMap<>();

        for (Cell cell : headerRow) {
            String headerName = getCellValue(cell, formatter);

            if (!headerName.isBlank()) {
                headerMap.put(normalizeHeader(headerName), cell.getColumnIndex());
            }
        }

        return headerMap;
    }

    private static String normalizeHeader(String header) {
        return header == null
                ? ""
                : header.trim().replaceAll("\\s+", "").toLowerCase();
    }

    public static String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public static void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GlobalException(ErrorCode.FILE_EMPTY.getMessage());
        }

        String contentType = file.getContentType();

        if (contentType == null || !EXCEL_CONTENT_TYPES.contains(contentType)) {
            throw new GlobalException("FIle "+ ErrorCode.INVALID_KEY.getMessage());
        }
    }

    public static <T> List<T> readExcel(
            MultipartFile file,
            ExcelRowMapper<T> mapper
    ) {
        validateExcelFile(file);

        List<T> result = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Map<String, Integer> headerMap = getHeaderIndexMap(sheet, formatter);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                T item = mapper.map(row, formatter, headerMap);

                if (item != null) {
                    result.add(item);
                }
            }

            return result;

        } catch (Exception e) {
            throw new GlobalException(ErrorCode.FILE_READ_ERROR.getMessage());
        }
    }

}

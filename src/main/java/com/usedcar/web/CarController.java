package com.usedcar.web;

import com.usedcar.dto.CarQuery;
import com.usedcar.dto.CarRequest;
import com.usedcar.dto.CarResponse;
import com.usedcar.dto.PageResponse;
import com.usedcar.dto.StatusFilter;
import com.usedcar.domain.SaleType;
import com.usedcar.domain.StockType;
import com.usedcar.service.CarExportService;
import com.usedcar.service.CarService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;
    private final CarExportService exportService;

    public CarController(CarService carService, CarExportService exportService) {
        this.carService = carService;
        this.exportService = exportService;
    }

    /** 一覧（検索 / 絞り込み / 並び替え / ページング） */
    @GetMapping
    public PageResponse<CarResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stockType,
        @RequestParam(required = false) String saleType,
        @RequestParam(required = false) String manufacturer,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockDateTo,
        @RequestParam(defaultValue = "stockDate") String sort,
        @RequestParam(defaultValue = "desc") String dir,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return carService.search(toQuery(search, status, stockType, saleType, manufacturer,
            stockDateFrom, stockDateTo, sort, dir, page, size));
    }

    /** CSV 出力（一覧と同じ検索条件） */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stockType,
        @RequestParam(required = false) String saleType,
        @RequestParam(required = false) String manufacturer,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockDateTo,
        @RequestParam(defaultValue = "stockDate") String sort,
        @RequestParam(defaultValue = "desc") String dir
    ) {
        CarQuery query = toQuery(search, status, stockType, saleType, manufacturer,
            stockDateFrom, stockDateTo, sort, dir, 0, 5000);

        // Excel で文字化けしないよう BOM 付き UTF-8
        String csv = "\uFEFF" + exportService.toCsv(query);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);

        String fileName = exportService.fileName(LocalDate.now());
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"inventory.csv\"; filename*=UTF-8''" + encoded)
            .body(body);
    }

    @GetMapping("/{id}")
    public CarResponse get(@PathVariable Long id) {
        return carService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarResponse create(@Valid @RequestBody CarRequest request) {
        return carService.create(request);
    }

    @PutMapping("/{id}")
    public CarResponse update(@PathVariable Long id, @Valid @RequestBody CarRequest request) {
        return carService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        carService.delete(id);
    }

    private CarQuery toQuery(String search, String status, String stockType, String saleType,
                             String manufacturer, LocalDate stockDateFrom, LocalDate stockDateTo,
                             String sort, String dir, int page, int size) {
        return new CarQuery(
            search,
            StatusFilter.parse(status),
            EnumSupport.parse(StockType.class, stockType),
            EnumSupport.parse(SaleType.class, saleType),
            manufacturer,
            stockDateFrom,
            stockDateTo,
            sort,
            dir,
            page,
            size
        );
    }
}

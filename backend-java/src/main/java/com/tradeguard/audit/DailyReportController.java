package com.tradeguard.audit;

import com.tradeguard.config.MarketProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class DailyReportController {

    private final DailyReportService reportService;
    private final MarketProperties market;

    public DailyReportController(DailyReportService reportService, MarketProperties market) {
        this.reportService = reportService;
        this.market = market;
    }

    @GetMapping(value = "/daily.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> dailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date == null
                ? LocalDate.now(market.getZone()) : date;
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tradeguard-" + reportDate + ".xml\"")
                .body(reportService.generate(reportDate));
    }
}

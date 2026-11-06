package com.usedcar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** ダッシュボード（KPI + 粗利分析）レスポンス */
public record DashboardResponse(
    LocalDate generatedAt,
    int months,
    Counts counts,
    InventorySummary inventory,
    SalesSummary realized,
    List<MonthlyPoint> monthly,
    List<StockTypePoint> stockTypes,
    List<SaleTypePoint> saleTypes,
    List<AgingPoint> aging,
    List<ManufacturerPoint> manufacturers,
    List<CarResponse> inspectionAlerts,
    List<CarResponse> longStockAlerts
) {

    public record Counts(
        long total,
        long inStock,
        long contracted,
        long sold,
        long scheduledIncoming,
        long inspectionOverdue,
        long inspectionExpiringSoon
    ) {
    }

    /** 在庫中車両の評価（販売前） */
    public record InventorySummary(
        long units,
        BigDecimal totalCost,
        BigDecimal totalSalePrice,
        BigDecimal potentialGrossProfit,
        BigDecimal averageSalePrice,
        BigDecimal averageGrossProfit,
        double averageStockDays,
        long longStockUnits
    ) {
    }

    /** 実績（契約日基準で計上した粗利） */
    public record SalesSummary(
        long units,
        BigDecimal totalSalePrice,
        BigDecimal totalCost,
        BigDecimal grossProfit,
        BigDecimal averageGrossProfit,
        BigDecimal grossProfitRate,
        BigDecimal thisMonthGrossProfit,
        long thisMonthUnits,
        BigDecimal lastMonthGrossProfit
    ) {
    }

    public record MonthlyPoint(
        String month,
        String label,
        long units,
        BigDecimal salePrice,
        BigDecimal cost,
        BigDecimal grossProfit
    ) {
    }

    public record StockTypePoint(
        String type,
        String label,
        long units,
        BigDecimal totalCost,
        BigDecimal totalSalePrice,
        BigDecimal grossProfit
    ) {
    }

    public record SaleTypePoint(
        String type,
        String label,
        long units,
        BigDecimal salePrice,
        BigDecimal grossProfit
    ) {
    }

    public record AgingPoint(
        String label,
        int fromDays,
        int toDays,
        long units,
        BigDecimal totalCost
    ) {
    }

    public record ManufacturerPoint(
        String name,
        long units,
        BigDecimal totalCost
    ) {
    }
}

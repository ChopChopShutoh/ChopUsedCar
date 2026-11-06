package com.usedcar.dto;

import com.usedcar.domain.Car;
import com.usedcar.domain.SaleType;
import com.usedcar.domain.StockType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/** 在庫 1 件のレスポンス（派生値を含む） */
public record CarResponse(
    Long id,
    Long version,

    // 車両の基本情報
    String manufacturer,
    String model,
    String chassisNumber,
    String vehicleType,
    YearMonth firstRegistration,
    LocalDate inspectionExpiry,
    Integer mileage,

    // 入庫情報
    LocalDate stockDate,
    LocalDate scheduledStockDate,
    StockType stockType,
    String stockTypeLabel,
    String supplierName,
    String buyerStaff,

    // 販売情報
    LocalDate contractDate,
    LocalDate transferDate,
    LocalDate deliveryDate,
    SaleType saleType,
    String saleTypeLabel,
    String buyerName,
    String sellerStaff,

    // 価格情報
    BigDecimal baseCost,
    BigDecimal recycleFee,
    BigDecimal transportFee,
    BigDecimal repairFee,
    BigDecimal totalCost,
    BigDecimal salePrice,
    BigDecimal grossProfit,
    BigDecimal grossProfitRate,

    // 派生 / 状態
    String status,
    String statusLabel,
    boolean scheduledIncoming,
    Long stockDays,
    Long daysUntilInspection,
    boolean realized,

    String memo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static CarResponse of(Car c) {
        return new CarResponse(
            c.getId(),
            c.getVersion(),

            c.getManufacturer(),
            c.getModel(),
            c.getChassisNumber(),
            c.getVehicleType(),
            c.getFirstRegistration(),
            c.getInspectionExpiry(),
            c.getMileage(),

            c.getStockDate(),
            c.getScheduledStockDate(),
            c.getStockType(),
            c.getStockType() == null ? null : c.getStockType().label(),
            c.getSupplierName(),
            c.getBuyerStaff(),

            c.getContractDate(),
            c.getTransferDate(),
            c.getDeliveryDate(),
            c.getSaleType(),
            c.getSaleType() == null ? null : c.getSaleType().label(),
            c.getBuyerName(),
            c.getSellerStaff(),

            c.getBaseCost(),
            c.getRecycleFee(),
            c.getTransportFee(),
            c.getRepairFee(),
            c.getTotalCost(),
            c.getSalePrice(),
            c.getGrossProfit(),
            c.grossProfitRate(),

            c.status().name(),
            c.status().label(),
            c.scheduledIncoming(),
            c.stockDays(),
            c.daysUntilInspection(),
            c.realized(),

            c.getMemo(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}

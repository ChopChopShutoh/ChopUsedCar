package com.usedcar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 中古車在庫 1 台分の記録。
 *
 * 派生値（原価合計 totalCost / 粗利 grossProfit）は保存時に {@link #recalculateDerived()} で再計算する。
 * 原価 = 車両本体(baseCost) + リサイクル預託金(recycleFee) + 輸送費(transportFee) + 加修費(repairFee)
 * 粗利 = 販売価格(salePrice) - 原価   ※ 販売価格が未設定なら粗利も未計算(null)
 */
@Entity
@Table(
    name = "cars",
    uniqueConstraints = @UniqueConstraint(name = "uk_cars_chassis_number", columnNames = "chassis_number"),
    indexes = {
        @Index(name = "idx_cars_stock_date", columnList = "stock_date"),
        @Index(name = "idx_cars_scheduled_stock_date", columnList = "scheduled_stock_date"),
        @Index(name = "idx_cars_contract_date", columnList = "contract_date"),
        @Index(name = "idx_cars_delivery_date", columnList = "delivery_date"),
        @Index(name = "idx_cars_inspection_expiry", columnList = "inspection_expiry")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // ===== 車両の基本情報 =====
    @Column(name = "manufacturer", nullable = false, length = 80)
    private String manufacturer;

    @Column(name = "model", nullable = false, length = 120)
    private String model;

    @Column(name = "chassis_number", nullable = false, length = 40)
    private String chassisNumber;

    @Column(name = "vehicle_type", length = 40)
    private String vehicleType;

    @Column(name = "first_registration", nullable = false, length = 7)
    private YearMonth firstRegistration;

    @Column(name = "inspection_expiry")
    private LocalDate inspectionExpiry;

    @Column(name = "mileage", nullable = false)
    private Integer mileage;

    // ===== 入庫情報 =====
    @Column(name = "stock_date")
    private LocalDate stockDate;

    @Column(name = "scheduled_stock_date")
    private LocalDate scheduledStockDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_type", length = 20)
    private StockType stockType;

    @Column(name = "supplier_name", length = 120)
    private String supplierName;

    @Column(name = "buyer_staff", length = 60)
    private String buyerStaff;

    // ===== 販売情報 =====
    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "transfer_date")
    private LocalDate transferDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", length = 20)
    private SaleType saleType;

    @Column(name = "buyer_name", length = 120)
    private String buyerName;

    @Column(name = "seller_staff", length = 60)
    private String sellerStaff;

    // ===== 価格情報 =====
    @Column(name = "base_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseCost;

    @Column(name = "recycle_fee", precision = 14, scale = 2)
    private BigDecimal recycleFee;

    @Column(name = "transport_fee", precision = 14, scale = 2)
    private BigDecimal transportFee;

    @Column(name = "repair_fee", precision = 14, scale = 2)
    private BigDecimal repairFee;

    @Column(name = "sale_price", precision = 14, scale = 2)
    private BigDecimal salePrice;

    /** 派生値: 原価合計（保存時に再計算） */
    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;

    /** 派生値: 粗利 = 販売価格 - 原価合計（保存時に再計算） */
    @Column(name = "gross_profit", precision = 14, scale = 2)
    private BigDecimal grossProfit;

    @Column(name = "memo", length = 500)
    private String memo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== 派生ロジック =====

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 原価合計・粗利を再計算する。保存前 / 更新前に必ず呼ばれる。 */
    public void recalculateDerived() {
        BigDecimal cost = nz(baseCost)
            .add(nz(recycleFee))
            .add(nz(transportFee))
            .add(nz(repairFee))
            .setScale(2, RoundingMode.HALF_UP);
        this.totalCost = cost;
        this.grossProfit = (salePrice == null)
            ? null
            : salePrice.setScale(2, RoundingMode.HALF_UP).subtract(cost);
    }

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    void onBeforeSave() {
        recalculateDerived();
    }

    /** 在庫ステータス（契約日 / 納車日から導出） */
    @Transient
    public CarStatus status() {
        if (deliveryDate != null) {
            return CarStatus.SOLD;
        }
        return contractDate != null ? CarStatus.CONTRACTED : CarStatus.IN_STOCK;
    }

    /** 粗利が実績計上されているか（契約日を基準に計上） */
    @Transient
    public boolean realized() {
        return contractDate != null;
    }

    /** 入庫予定のみ（まだ入庫していない） */
    @Transient
    public boolean scheduledIncoming() {
        return stockDate == null && scheduledStockDate != null;
    }

    /** 在庫日数（入庫日基準 / 入庫予定のみなら予定日基準のマイナス日数） */
    @Transient
    public Long stockDays() {
        LocalDate today = LocalDate.now();
        if (stockDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(stockDate, today);
        }
        if (scheduledStockDate != null) {
            return -java.time.temporal.ChronoUnit.DAYS.between(today, scheduledStockDate);
        }
        return null;
    }

    /** 車検満了日までの残日数（マイナスなら車検切れ） */
    @Transient
    public Long daysUntilInspection() {
        if (inspectionExpiry == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), inspectionExpiry);
    }

    /** 粗利率（販売価格に対する粗利の割合 / 100） */
    @Transient
    public BigDecimal grossProfitRate() {
        if (salePrice == null || salePrice.signum() == 0 || grossProfit == null) {
            return null;
        }
        return grossProfit.multiply(BigDecimal.valueOf(100))
            .divide(salePrice, 1, RoundingMode.HALF_UP);
    }

    /** 車種表示（メーカー + 車種） */
    @Transient
    public String displayName() {
        return manufacturer + " " + model;
    }

    // ===== getter / setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getChassisNumber() {
        return chassisNumber;
    }

    public void setChassisNumber(String chassisNumber) {
        this.chassisNumber = chassisNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public YearMonth getFirstRegistration() {
        return firstRegistration;
    }

    public void setFirstRegistration(YearMonth firstRegistration) {
        this.firstRegistration = firstRegistration;
    }

    public LocalDate getInspectionExpiry() {
        return inspectionExpiry;
    }

    public void setInspectionExpiry(LocalDate inspectionExpiry) {
        this.inspectionExpiry = inspectionExpiry;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public LocalDate getStockDate() {
        return stockDate;
    }

    public void setStockDate(LocalDate stockDate) {
        this.stockDate = stockDate;
    }

    public LocalDate getScheduledStockDate() {
        return scheduledStockDate;
    }

    public void setScheduledStockDate(LocalDate scheduledStockDate) {
        this.scheduledStockDate = scheduledStockDate;
    }

    public StockType getStockType() {
        return stockType;
    }

    public void setStockType(StockType stockType) {
        this.stockType = stockType;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getBuyerStaff() {
        return buyerStaff;
    }

    public void setBuyerStaff(String buyerStaff) {
        this.buyerStaff = buyerStaff;
    }

    public LocalDate getContractDate() {
        return contractDate;
    }

    public void setContractDate(LocalDate contractDate) {
        this.contractDate = contractDate;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public SaleType getSaleType() {
        return saleType;
    }

    public void setSaleType(SaleType saleType) {
        this.saleType = saleType;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getSellerStaff() {
        return sellerStaff;
    }

    public void setSellerStaff(String sellerStaff) {
        this.sellerStaff = sellerStaff;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public BigDecimal getRecycleFee() {
        return recycleFee;
    }

    public void setRecycleFee(BigDecimal recycleFee) {
        this.recycleFee = recycleFee;
    }

    public BigDecimal getTransportFee() {
        return transportFee;
    }

    public void setTransportFee(BigDecimal transportFee) {
        this.transportFee = transportFee;
    }

    public BigDecimal getRepairFee() {
        return repairFee;
    }

    public void setRepairFee(BigDecimal repairFee) {
        this.repairFee = repairFee;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

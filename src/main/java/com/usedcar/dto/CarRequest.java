package com.usedcar.dto;

import com.usedcar.domain.SaleType;
import com.usedcar.domain.StockType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** 在庫登録 / 更新のリクエスト */
public record CarRequest(

    // 車両の基本情報
    @NotBlank(message = "メーカー名は必須です")
    @Size(max = 80, message = "メーカー名は80文字以内で入力してください")
    String manufacturer,

    @NotBlank(message = "車種は必須です")
    @Size(max = 120, message = "車種は120文字以内で入力してください")
    String model,

    @NotBlank(message = "車台番号は必須です")
    @Pattern(regexp = "^[0-9A-Za-z-/]{3,40}$", message = "車台番号は3〜40文字の半角英数字(- /)で入力してください")
    String chassisNumber,

    @Size(max = 40, message = "型式は40文字以内で入力してください")
    String vehicleType,

    @NotNull(message = "初度登録年月は必須です")
    YearMonth firstRegistration,

    LocalDate inspectionExpiry,

    @NotNull(message = "走行距離(km)は必須です")
    @PositiveOrZero(message = "走行距離は0km以上で入力してください")
    Integer mileage,

    // 入庫情報
    LocalDate stockDate,
    LocalDate scheduledStockDate,
    StockType stockType,

    @Size(max = 120, message = "仕入先名称は120文字以内で入力してください")
    String supplierName,

    @Size(max = 60, message = "仕入担当者は60文字以内で入力してください")
    String buyerStaff,

    // 販売情報
    LocalDate contractDate,
    LocalDate transferDate,
    LocalDate deliveryDate,
    SaleType saleType,

    @Size(max = 120, message = "販売先名称は120文字以内で入力してください")
    String buyerName,

    @Size(max = 60, message = "販売担当者は60文字以内で入力してください")
    String sellerStaff,

    // 価格情報
    @NotNull(message = "車両本体(原価)は必須です")
    @PositiveOrZero(message = "車両本体(原価)は0円以上で入力してください")
    BigDecimal baseCost,

    @PositiveOrZero(message = "リサイクル預託金は0円以上で入力してください")
    BigDecimal recycleFee,

    @PositiveOrZero(message = "輸送費は0円以上で入力してください")
    BigDecimal transportFee,

    @PositiveOrZero(message = "加修費は0円以上で入力してください")
    BigDecimal repairFee,

    @PositiveOrZero(message = "販売価格は0円以上で入力してください")
    BigDecimal salePrice,

    @Size(max = 500, message = "備考は500文字以内で入力してください")
    String memo
) {

    @AssertTrue(message = "入庫日または入庫予定日のいずれかを入力してください")
    boolean isStockDateProvided() {
        return stockDate != null || scheduledStockDate != null;
    }

    @AssertTrue(message = "納車日は契約日以降の日付にしてください")
    boolean isDeliveryAfterContract() {
        if (contractDate == null || deliveryDate == null) {
            return true;
        }
        return !deliveryDate.isBefore(contractDate);
    }

    @AssertTrue(message = "名義変更日は契約日以降の日付にしてください")
    boolean isTransferAfterContract() {
        if (contractDate == null || transferDate == null) {
            return true;
        }
        return !transferDate.isBefore(contractDate);
    }

    @AssertTrue(message = "納車日は名義変更日以降の日付にしてください")
    boolean isDeliveryAfterTransfer() {
        if (transferDate == null || deliveryDate == null) {
            return true;
        }
        return !deliveryDate.isBefore(transferDate);
    }

    @AssertTrue(message = "販売登録（契約日・販売区分）には販売価格が必要です")
    boolean isSalePriceRequiredForSale() {
        boolean saleRegistered = contractDate != null || deliveryDate != null || saleType != null;
        return !saleRegistered || salePrice != null;
    }

    @AssertTrue(message = "入庫予定日は入庫日より後の日付にはできません")
    boolean isScheduleNotAfterStock() {
        if (stockDate == null || scheduledStockDate == null) {
            return true;
        }
        return !scheduledStockDate.isAfter(stockDate);
    }
}

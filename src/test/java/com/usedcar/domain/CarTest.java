package com.usedcar.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class CarTest {

    private Car sample() {
        Car car = new Car();
        car.setManufacturer("トヨタ");
        car.setModel("プリウス GX");
        car.setChassisNumber("8834101");
        car.setVehicleType("DAA-ZVW50A");
        car.setFirstRegistration(YearMonth.of(2018, 3));
        car.setInspectionExpiry(LocalDate.of(2027, 3, 15));
        car.setMileage(62_400);
        car.setBaseCost(new BigDecimal("1050000"));
        car.setRecycleFee(new BigDecimal("13000"));
        car.setTransportFee(new BigDecimal("18000"));
        car.setRepairFee(new BigDecimal("45000"));
        car.setSalePrice(new BigDecimal("1580000"));
        return car;
    }

    @Test
    @DisplayName("原価合計 = 車両本体 + リサイクル預託金 + 輸送費 + 加修費")
    void totalCostIsSumOfCostItems() {
        Car car = sample();
        car.recalculateDerived();

        assertThat(car.getTotalCost()).isEqualByComparingTo("1126000");
    }

    @Test
    @DisplayName("粗利 = 販売価格 - 原価合計")
    void grossProfitIsSalePriceMinusCost() {
        Car car = sample();
        car.recalculateDerived();

        assertThat(car.getGrossProfit()).isEqualByComparingTo("454000");
        assertThat(car.grossProfitRate()).isEqualByComparingTo("28.7");
    }

    @Test
    @DisplayName("販売価格が未設定なら粗利は null（潜在粗利なし）")
    void grossProfitIsNullWithoutSalePrice() {
        Car car = sample();
        car.setSalePrice(null);
        car.recalculateDerived();

        assertThat(car.getGrossProfit()).isNull();
        assertThat(car.grossProfitRate()).isNull();
        assertThat(car.getTotalCost()).isEqualByComparingTo("1126000");
    }

    @Test
    @DisplayName("赤字案件はマイナス粗利になる")
    void negativeGrossProfitIsPossible() {
        Car car = sample();
        car.setSalePrice(new BigDecimal("900000"));
        car.recalculateDerived();

        assertThat(car.getGrossProfit()).isEqualByComparingTo("-226000");
        assertThat(car.grossProfitRate()).isEqualByComparingTo("-25.1");
    }

    @Test
    @DisplayName("契約日 / 納車日からステータスを導出する")
    void statusDerivedFromDates() {
        Car car = sample();

        assertThat(car.status()).isEqualTo(CarStatus.IN_STOCK);

        car.setContractDate(LocalDate.now().minusDays(10));
        assertThat(car.status()).isEqualTo(CarStatus.CONTRACTED);
        assertThat(car.realized()).isTrue();

        car.setDeliveryDate(LocalDate.now());
        assertThat(car.status()).isEqualTo(CarStatus.SOLD);
    }

    @Test
    @DisplayName("入庫予定のみの案件は scheduledIncoming")
    void scheduledIncoming() {
        Car car = sample();
        car.setScheduledStockDate(LocalDate.now().plusDays(7));

        assertThat(car.scheduledIncoming()).isTrue();
        assertThat(car.stockDays()).isNegative();
    }
}

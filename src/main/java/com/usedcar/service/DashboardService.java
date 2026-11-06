package com.usedcar.service;

import com.usedcar.domain.Car;
import com.usedcar.domain.CarStatus;
import com.usedcar.domain.SaleType;
import com.usedcar.domain.StockType;
import com.usedcar.dto.CarResponse;
import com.usedcar.dto.DashboardResponse;
import com.usedcar.repository.CarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ダッシュボード用の集計。
 * 粗利は「契約日」を基準に実績計上する（契約日が無いものは在庫中の潜在粗利として扱う）。
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int MAX_MONTHS = 36;
    private static final int ALERT_LIMIT = 8;
    private static final int OPEN_ENDED = 9999;

    private final CarRepository repository;
    private final int inspectionAlertDays;
    private final int longStockDays;

    public DashboardService(CarRepository repository,
                            @Value("${app.inspection-alert-days:30}") int inspectionAlertDays,
                            @Value("${app.long-stock-days:90}") int longStockDays) {
        this.repository = repository;
        this.inspectionAlertDays = inspectionAlertDays;
        this.longStockDays = longStockDays;
    }

    public DashboardResponse dashboard(int monthsRequested) {
        int months = Math.min(Math.max(1, monthsRequested), MAX_MONTHS);
        LocalDate today = LocalDate.now();

        List<Car> all = repository.findAll();
        List<Car> inventory = all.stream()
            .filter(c -> c.status() == CarStatus.IN_STOCK)
            .toList();
        List<Car> contracted = all.stream()
            .filter(c -> c.status() == CarStatus.CONTRACTED)
            .toList();
        List<Car> sold = all.stream()
            .filter(c -> c.status() == CarStatus.SOLD)
            .toList();
        List<Car> scheduled = all.stream().filter(Car::scheduledIncoming).toList();

        // 粗利実績（契約日があるもの）
        List<Car> realized = all.stream().filter(Car::realized).toList();

        DashboardResponse.Counts counts = new DashboardResponse.Counts(
            all.size(),
            inventory.size(),
            contracted.size(),
            sold.size(),
            scheduled.size(),
            inventory.stream().filter(c -> days(c) != null && days(c) < 0).count(),
            inventory.stream().filter(c -> days(c) != null && days(c) >= 0 && days(c) <= inspectionAlertDays).count()
        );

        DashboardResponse.InventorySummary inventorySummary = summarizeInventory(inventory);
        DashboardResponse.SalesSummary salesSummary = summarizeSales(realized, today);

        List<DashboardResponse.MonthlyPoint> monthly = monthlyTrend(realized, today, months);
        List<DashboardResponse.StockTypePoint> stockTypes = stockTypeBreakdown(inventory);
        List<DashboardResponse.SaleTypePoint> saleTypes = saleTypeBreakdown(realized);
        List<DashboardResponse.AgingPoint> aging = agingBreakdown(inventory);
        List<DashboardResponse.ManufacturerPoint> manufacturers = manufacturerBreakdown(inventory);

        Comparator<Long> ascending = Comparator.nullsLast(Comparator.naturalOrder());
        Comparator<Long> descending = Comparator.nullsLast(Comparator.reverseOrder());

        List<CarResponse> inspectionAlerts = inventory.stream()
            .filter(c -> days(c) != null && days(c) <= inspectionAlertDays)
            .sorted(Comparator.comparing(Car::daysUntilInspection, ascending))
            .limit(ALERT_LIMIT)
            .map(CarResponse::of)
            .toList();

        List<CarResponse> longStockAlerts = inventory.stream()
            .filter(c -> c.stockDays() != null && c.stockDays() > longStockDays)
            .sorted(Comparator.comparing(Car::stockDays, descending))
            .limit(ALERT_LIMIT)
            .map(CarResponse::of)
            .toList();

        return new DashboardResponse(
            today, months, counts, inventorySummary, salesSummary,
            monthly, stockTypes, saleTypes, aging, manufacturers,
            inspectionAlerts, longStockAlerts
        );
    }

    // ===== 集計ヘルパー =====

    private static Long days(Car c) {
        return c.daysUntilInspection();
    }

    private static BigDecimal sumCost(List<Car> cars) {
        return cars.stream().map(c -> nz(c.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumSalePrice(List<Car> cars) {
        return cars.stream().map(c -> nz(c.getSalePrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumGrossProfit(List<Car> cars) {
        return cars.stream().map(c -> nz(c.getGrossProfit())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal average(BigDecimal total, long count) {
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(BigDecimal grossProfit, BigDecimal salePrice) {
        if (salePrice == null || salePrice.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return grossProfit.multiply(BigDecimal.valueOf(100)).divide(salePrice, 1, RoundingMode.HALF_UP);
    }

    private DashboardResponse.InventorySummary summarizeInventory(List<Car> inventory) {
        BigDecimal cost = sumCost(inventory);
        BigDecimal salePrice = sumSalePrice(inventory);
        BigDecimal potential = sumGrossProfit(inventory);

        double avgDays = inventory.stream()
            .map(Car::stockDays)
            .filter(java.util.Objects::nonNull)
            .mapToLong(Long::longValue)
            .average()
            .orElse(0d);

        long longStock = inventory.stream()
            .filter(c -> c.stockDays() != null && c.stockDays() > longStockDays)
            .count();

        return new DashboardResponse.InventorySummary(
            inventory.size(),
            cost,
            salePrice,
            potential,
            average(salePrice, inventory.size()),
            average(potential, inventory.size()),
            Math.round(avgDays * 10d) / 10d,
            longStock
        );
    }

    private DashboardResponse.SalesSummary summarizeSales(List<Car> realized, LocalDate today) {
        BigDecimal salePrice = sumSalePrice(realized);
        BigDecimal cost = sumCost(realized);
        BigDecimal profit = sumGrossProfit(realized);

        YearMonth current = YearMonth.from(today);
        YearMonth previous = current.minusMonths(1);

        List<Car> thisMonth = realized.stream()
            .filter(c -> c.getContractDate() != null && YearMonth.from(c.getContractDate()).equals(current))
            .toList();
        List<Car> lastMonth = realized.stream()
            .filter(c -> c.getContractDate() != null && YearMonth.from(c.getContractDate()).equals(previous))
            .toList();

        return new DashboardResponse.SalesSummary(
            realized.size(),
            salePrice,
            cost,
            profit,
            average(profit, realized.size()),
            rate(profit, salePrice),
            sumGrossProfit(thisMonth),
            thisMonth.size(),
            sumGrossProfit(lastMonth)
        );
    }

    private List<DashboardResponse.MonthlyPoint> monthlyTrend(List<Car> realized, LocalDate today, int months) {
        YearMonth current = YearMonth.from(today);
        YearMonth start = current.minusMonths(months - 1L);

        Map<YearMonth, List<Car>> grouped = realized.stream()
            .filter(c -> c.getContractDate() != null)
            .collect(Collectors.groupingBy(c -> YearMonth.from(c.getContractDate())));

        List<DashboardResponse.MonthlyPoint> points = new ArrayList<>();
        for (YearMonth month = start; !month.isAfter(current); month = month.plusMonths(1)) {
            List<Car> bucket = grouped.getOrDefault(month, List.of());
            BigDecimal salePrice = sumSalePrice(bucket);
            BigDecimal cost = sumCost(bucket);
            BigDecimal profit = sumGrossProfit(bucket);
            points.add(new DashboardResponse.MonthlyPoint(
                month.toString(),
                month.getMonthValue() + "月",
                bucket.size(),
                salePrice,
                cost,
                profit
            ));
        }
        return points;
    }

    private List<DashboardResponse.StockTypePoint> stockTypeBreakdown(List<Car> inventory) {
        Map<StockType, List<Car>> grouped = new EnumMap<>(StockType.class);
        for (Car c : inventory) {
            StockType type = c.getStockType() == null ? StockType.BUY_OUT : c.getStockType();
            grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(c);
        }

        List<DashboardResponse.StockTypePoint> points = new ArrayList<>();
        for (StockType type : StockType.values()) {
            List<Car> bucket = grouped.getOrDefault(type, List.of());
            BigDecimal salePrice = sumSalePrice(bucket);
            points.add(new DashboardResponse.StockTypePoint(
                type.name(),
                type.label(),
                bucket.size(),
                sumCost(bucket),
                salePrice,
                sumGrossProfit(bucket)
            ));
        }
        return points;
    }

    private List<DashboardResponse.SaleTypePoint> saleTypeBreakdown(List<Car> realized) {
        Map<SaleType, List<Car>> grouped = new EnumMap<>(SaleType.class);
        for (Car c : realized) {
            SaleType type = c.getSaleType() == null ? SaleType.RETAIL : c.getSaleType();
            grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(c);
        }

        List<DashboardResponse.SaleTypePoint> points = new ArrayList<>();
        for (SaleType type : SaleType.values()) {
            List<Car> bucket = grouped.getOrDefault(type, List.of());
            points.add(new DashboardResponse.SaleTypePoint(
                type.name(),
                type.label(),
                bucket.size(),
                sumSalePrice(bucket),
                sumGrossProfit(bucket)
            ));
        }
        return points;
    }

    private List<DashboardResponse.AgingPoint> agingBreakdown(List<Car> inventory) {
        int[][] buckets = {
            {0, 14}, {15, 30}, {31, 60}, {61, 90}, {91, OPEN_ENDED}
        };
        List<DashboardResponse.AgingPoint> points = new ArrayList<>();
        for (int[] bucket : buckets) {
            String label = bucket[1] == OPEN_ENDED
                ? bucket[0] + "日〜"
                : bucket[0] + "〜" + bucket[1] + "日";
            List<Car> matched = inventory.stream()
                .filter(c -> c.getStockDate() != null && c.stockDays() != null)
                .filter(c -> c.stockDays() >= bucket[0] && c.stockDays() <= bucket[1])
                .toList();
            points.add(new DashboardResponse.AgingPoint(
                label, bucket[0], bucket[1], matched.size(), sumCost(matched)));
        }
        return points;
    }

    private List<DashboardResponse.ManufacturerPoint> manufacturerBreakdown(List<Car> inventory) {
        Map<String, List<Car>> grouped = new LinkedHashMap<>();
        for (Car c : inventory) {
            grouped.computeIfAbsent(c.getManufacturer(), k -> new ArrayList<>()).add(c);
        }
        Comparator<List<Car>> bySize = (a, b) -> Integer.compare(a.size(), b.size());
        return grouped.entrySet().stream()
            .sorted(Map.Entry.<String, List<Car>>comparingByValue(bySize).reversed())
            .limit(8)
            .map(e -> new DashboardResponse.ManufacturerPoint(e.getKey(), e.getValue().size(), sumCost(e.getValue())))
            .toList();
    }
}

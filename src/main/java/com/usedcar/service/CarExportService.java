package com.usedcar.service;

import com.usedcar.dto.CarQuery;
import com.usedcar.dto.CarResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** 一覧の検索条件と同じ範囲で CSV 出力を作成する */
@Service
public class CarExportService {

    private static final String[] HEADER = {
        "車台番号", "メーカー", "車種", "型式", "初度登録年月", "車検満了日", "走行距離(km)", "状態", "在庫日数",
        "入庫予定日", "入庫日", "入庫区分", "仕入先", "仕入担当者",
        "契約日", "名義変更日", "納車日", "販売区分", "販売先", "販売担当者",
        "車両本体", "リサイクル預託金", "輸送費", "加修費", "原価合計", "販売価格", "粗利", "粗利率(%)",
        "備考", "登録日時", "更新日時"
    };

    private final CarService carService;

    public CarExportService(CarService carService) {
        this.carService = carService;
    }

    public String toCsv(CarQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADER)).append("\r\n");
        for (CarResponse c : carService.findAll(query)) {
            row(sb, c);
        }
        return sb.toString();
    }

    public String fileName(LocalDate today) {
        return "中古車在庫_" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
    }

    private void row(StringBuilder sb, CarResponse c) {
        List<String> cells = List.of(
            s(c.chassisNumber()),
            s(c.manufacturer()),
            s(c.model()),
            s(c.vehicleType()),
            c.firstRegistration() == null ? "" : c.firstRegistration().toString(),
            s(c.inspectionExpiry()),
            c.mileage() == null ? "" : String.valueOf(c.mileage()),
            s(c.statusLabel()),
            c.stockDays() == null ? "" : String.valueOf(c.stockDays()),
            s(c.scheduledStockDate()),
            s(c.stockDate()),
            s(c.stockTypeLabel()),
            s(c.supplierName()),
            s(c.buyerStaff()),
            s(c.contractDate()),
            s(c.transferDate()),
            s(c.deliveryDate()),
            s(c.saleTypeLabel()),
            s(c.buyerName()),
            s(c.sellerStaff()),
            money(c.baseCost()),
            money(c.recycleFee()),
            money(c.transportFee()),
            money(c.repairFee()),
            money(c.totalCost()),
            money(c.salePrice()),
            money(c.grossProfit()),
            c.grossProfitRate() == null ? "" : c.grossProfitRate().toPlainString(),
            s(c.memo()),
            c.createdAt() == null ? "" : c.createdAt().toLocalDate().toString(),
            c.updatedAt() == null ? "" : c.updatedAt().toLocalDate().toString()
        );

        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append("\r\n");
    }

    private static String s(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String money(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String escape(String value) {
        if (value.indexOf('"') >= 0 || value.indexOf(',') >= 0
            || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}

package com.usedcar.config;

import com.usedcar.domain.Car;
import com.usedcar.domain.SaleType;
import com.usedcar.domain.StockType;
import com.usedcar.repository.CarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * デモ用サンプルデータ。DB が空のときに 1 回だけ投入する。
 * 日付はすべて「今日」を基準にした相対値なので、いつ起動しても現実的な在庫状況になる。
 *
 * 無効化: application.yml -> app.demo-data.enabled: false
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final CarRepository repository;

    public DemoDataSeeder(CarRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<Car> cars = samples(today);
        repository.saveAll(cars);
        log.info("デモデータを {} 件投入しました (app.demo-data.enabled=false で無効化できます)", cars.size());
    }

    // ============================================================
    // サンプルデータ
    // ============================================================

    private List<Car> samples(LocalDate today) {
        List<Car> list = new ArrayList<>();

        // ---- 納車済（実績粗利） ----
        list.add(new D(today, "トヨタ", "プリウス GX", "8834101", "DAA-ZVW50A", 96, 480, 62_400)
            .stock(210, StockType.TRADE_IN, "株式会社横浜オート", "山田 太郎")
            .sale(190, 183, 176, SaleType.RETAIL, "山口 直子", "佐藤 健一")
            .price(1_050_000, 13_000, 18_000, 45_000, 1_580_000)
            .memo("ワンオーナー・純正ナビ。販売成約。")
            .car());

        list.add(new D(today, "ホンダ", "フィット e:HEV HOME", "5520733", "6BA-RW1", 60, 620, 34_800)
            .stock(165, StockType.BUY_OUT, "大阪中古車仕入センター", "小林 美咲")
            .sale(150, 144, 140, SaleType.RETAIL, "田中 義弘", "佐藤 健一")
            .price(890_000, 11_000, 15_000, 20_000, 1_190_000)
            .memo("自社買取。整備込み。")
            .car());

        list.add(new D(today, "トヨタ", "ルーミー T", "7741209", "DBA-NS10A", 72, 300, 41_200)
            .stock(300, StockType.AUCTION, "USS 東京本部", "小林 美咲")
            .sale(280, 274, 270, SaleType.WHOLESALE, "中部モーター商会", "高橋 誠")
            .price(520_000, 10_000, 12_000, 8_000, 760_000)
            .memo("業販（遠隔落札->転売）。")
            .car());

        list.add(new D(today, "マツダ", "ロードスター RS", "3390112", "DBA-NB8C", 150, 120, 88_600)
            .stock(260, StockType.BUY_OUT, "個人売買 (Twitter)", "高橋 誠")
            .sale(240, 235, 230, SaleType.RETAIL, "中村 亮", "山田 太郎")
            .price(680_000, 11_000, 9_000, 120_000, 1_020_000)
            .memo("加修費高額（クラッチ交換）。それでも黒字。")
            .car());

        list.add(new D(today, "スズキ", "スペリア SWIFT Sport", "6612087", "DBA-ZC72S", 48, 560, 27_500)
            .stock(140, StockType.AUCTION, "TAA 中部オートオークション", "山田 太郎")
            .sale(120, 115, 112, SaleType.RETAIL, "松本 葵", "高橋 誠")
            .price(730_000, 11_000, 14_000, 15_000, 990_000)
            .car());

        list.add(new D(today, "ニッサン", "ノート e-POWER A", "2280344", "6AA-HE12", 42, 700, 25_900)
            .stock(115, StockType.TRADE_IN, "customer trade-in", "佐藤 健一")
            .sale(95, 90, 88, SaleType.RETAIL, "井上 拓海", "佐藤 健一")
            .price(940_000, 12_000, 16_000, 25_000, 1_240_000)
            .memo("下取り->小売。")
            .car());

        list.add(new D(today, "BMW", "118i M Sport", "4417021", "3BA-1XB51", 60, 420, 51_300)
            .stock(200, StockType.AUCTION, "JU 東京ベイ", "高橋 誠")
            .sale(170, 165, 160, SaleType.RETAIL, "有限会社サン自動車", "山田 太郎")
            .price(1_180_000, 14_000, 22_000, 180_000, 1_690_000)
            .memo("輸入車。板金・車検整備込み。")
            .car());

        list.add(new D(today, "トヨタ", "シエンタ X", "9902113", "DBA-NSP170G", 84, 260, 73_800)
            .stock(320, StockType.BUY_OUT, "兵庫オートモール", "小林 美咲")
            .sale(300, 296, 292, SaleType.WHOLESALE, "九州カーディール", "高橋 誠")
            .price(560_000, 10_000, 30_000, 12_000, 795_000)
            .car());

        list.add(new D(today, "スバル", "インプレッサ 1.6i", "1150288", "DBA-GK336", 120, 60, 104_200)
            .stock(380, StockType.AUCTION, "HAA 西日本", "山田 太郎")
            .sale(350, 345, 342, SaleType.AUCTION, "オークション出品 (IH)", "高橋 誠")
            .price(390_000, 11_000, 0, 6_000, 505_000)
            .memo("自社出品。落札手数料は加修費に按分。")
            .car());

        list.add(new D(today, "ダイハツ", "タフト G", "7781044", "6DA-LA900S", 30, 690, 19_400)
            .stock(95, StockType.TRADE_IN, "展示顧客", "佐藤 健一")
            .sale(70, 66, 63, SaleType.RETAIL, "吉田 遥", "佐藤 健一")
            .price(820_000, 10_000, 12_000, 10_000, 1_090_000)
            .car());

        list.add(new D(today, "ホンダ", "N-BOX Custom G", "3304551", "6BA-JH3", 54, 380, 39_700)
            .stock(150, StockType.BUY_OUT, "店舗 買取", "小林 美咲")
            .sale(130, 126, 124, SaleType.SCRAP, "廃車引取 (リユース)", "高橋 誠")
            .price(120_000, 0, 0, 3_000, 138_000)
            .memo("事故車。廃車販売（粗利わずか）。")
            .car());

        list.add(new D(today, "トヨタ", "ヴォクシー ZS", "6650092", "DBA-ZRR80G", 78, 330, 82_500)
            .stock(245, StockType.AUCTION, "USS 中部", "高橋 誠")
            .sale(215, 210, 208, SaleType.RETAIL, "渡辺 佳奈", "山田 太郎")
            .price(1_120_000, 13_000, 20_000, 60_000, 1_490_000)
            .car());

        list.add(new D(today, "アウディ", "A3 Sportback 35 TFSI", "2216740", "3BA-8YCDFB", 45, 540, 36_100)
            .stock(130, StockType.AUCTION, "BMW/輸入車専門 AU", "山田 太郎")
            .sale(105, 100, 98, SaleType.RETAIL, "海外輸出業者 (業販)", "高橋 誠")
            .price(1_240_000, 14_000, 40_000, 90_000, 1_310_000)
            .memo("輸出業販。輸送費重め。")
            .car());

        // ---- 契約済（納車待ち） ----
        list.add(new D(today, "トヨタ", "ヤリス Hybrid G", "1180333", "6BA-MXPB10", 24, 830, 12_600)
            .stock(48, StockType.TRADE_IN, "展示顧客", "佐藤 健一")
            .sale(9, 4, null, SaleType.RETAIL, "木村 颯太", "佐藤 健一")
            .price(1_290_000, 13_000, 12_000, 30_000, 1_780_000)
            .memo("名義変更完了・納車 11 月上旬予定。")
            .car());

        list.add(new D(today, "マツダ", "CX-30 Touring", "4402198", "LDY-DMFSKYACTIVG", 36, 760, 21_800)
            .stock(62, StockType.BUY_OUT, "四国オートオークション", "小林 美咲")
            .sale(16, null, null, SaleType.RETAIL, "森 千夏", "山田 太郎")
            .price(1_420_000, 14_000, 18_000, 25_000, 1_860_000)
            .memo("契約済・ローン審査中。")
            .car());

        // ---- 在庫中 ----
        list.add(new D(today, "ホンダ", "フリード e:HEV Air", "9930251", "6AA-GT3", 40, 640, 31_200)
            .stock(28, StockType.AUCTION, "USS 東京本部", "小林 美咲")
            .price(1_310_000, 13_000, 19_000, 35_000, 1_740_000)
            .memo("展示中。Web 反響あり。")
            .car());

        list.add(new D(today, "トヨタ", "アルファード S", "2201887", "3DA-AGH30W", 34, 800, 29_900)
            .stock(74, StockType.AUCTION, "JU 東京ベイ", "高橋 誠")
            .price(3_120_000, 15_000, 26_000, 80_000, 3_980_000)
            .memo("人気車両。短期成約見込み。")
            .car());

        list.add(new D(today, "ニッサン", "セレナ S-HYBRID", "7713045", "6AA-VC37", 66, 520, 96_400)
            .stock(118, StockType.TRADE_IN, "買い替え顧客", "佐藤 健一")
            .price(1_050_000, 12_000, 15_000, 110_000, 1_150_000)
            .memo("加修費が大きく粗利が薄い。販売価格見直し候補。")
            .car());

        list.add(new D(today, "スズキ", "ワゴンR SMILE HT", "5580166", "5AA-MH95S", 58, 18, 47_300)
            .stock(160, StockType.BUY_OUT, "店舗 買取", "小林 美咲")
            .price(420_000, 10_000, 8_000, 45_000, 498_000)
            .memo("車検 18 日残。車検整備前提の販売要。")
            .car());

        list.add(new D(today, "MINI", "クロスオーバー Cooper", "3341902", "3DA-MN15B", 90, -12, 78_900)
            .stock(205, StockType.AUCTION, "輸入車 AU", "山田 太郎")
            .price(760_000, 12_000, 20_000, 210_000, 830_000)
            .memo("長期在庫・車検切れ。加修費次第で赤字リスク。")
            .car());

        list.add(new D(today, "三菱", "eK X 空間 T", "8890433", "6AA-B31A", 20, 860, 9_800)
            .stock(34, StockType.AUCTION, "TAA 中部", "小林 美咲")
            .price(780_000, 11_000, 14_000, 9_000, 990_000)
            .memo("在庫 34 日。成約率向上施策対象。")
            .car());

        list.add(new D(today, "ボルボ", "XC40 Plus B4", "1102476", "3LA-BV1R42", 28, 770, 24_300)
            .stock(88, StockType.BUY_OUT, "正規代理店 下取り", "高橋 誠")
            .price(2_350_000, 15_000, 28_000, 60_000, 2_540_000)
            .memo("赤字リスクあり（販売価格 - 原価が薄い）。")
            .car());

        // ---- 入庫予定（未入庫） ----
        list.add(new D(today, "トヨタ", "クラウン X10", "6674011", "3AA-AZSH31", 18, 880, 15_400)
            .scheduled(5, StockType.AUCTION, "USS 東京本部", "高橋 誠")
            .price(2_480_000, 15_000, 24_000, 0, 3_190_000)
            .memo("落札済み・陸送中。")
            .car());

        list.add(new D(today, "レクサス", "NX300 Version L", "4498720", "3BA-AZ20", 30, 840, 33_700)
            .scheduled(12, StockType.TRADE_IN, "LEXUS  dealer", "佐藤 健一")
            .price(3_580_000, 16_000, 0, 40_000, 4_380_000)
            .memo("下取り確定・入庫予定 12 日後。")
            .car());

        return list;
    }

    // ============================================================
    // サンプル作成用ビルダー
    // ============================================================

    private static final class D {
        private final Car car;
        private final LocalDate today;

        D(LocalDate today, String manufacturer, String model, String chassis,
          String vehicleType, int registrationMonthsAgo, Integer inspectionDaysFromToday, int mileage) {
            this.today = today;
            this.car = new Car();
            car.setManufacturer(manufacturer);
            car.setModel(model);
            car.setChassisNumber(chassis);
            car.setVehicleType(vehicleType);
            car.setFirstRegistration(YearMonth.from(today).minusMonths(registrationMonthsAgo));
            car.setInspectionExpiry(inspectionDaysFromToday == null ? null : today.plusDays(inspectionDaysFromToday));
            car.setMileage(mileage);
        }

        D stock(int daysAgo, StockType type, String supplier, String staff) {
            car.setStockDate(today.minusDays(daysAgo));
            car.setStockType(type);
            car.setSupplierName(supplier);
            car.setBuyerStaff(staff);
            return this;
        }

        D scheduled(int daysAhead, StockType type, String supplier, String staff) {
            car.setScheduledStockDate(today.plusDays(daysAhead));
            car.setStockType(type);
            car.setSupplierName(supplier);
            car.setBuyerStaff(staff);
            return this;
        }

        D sale(int contractDaysAgo, Integer transferDaysAgo, Integer deliveryDaysAgo,
               SaleType type, String buyer, String staff) {
            car.setContractDate(today.minusDays(contractDaysAgo));
            car.setTransferDate(transferDaysAgo == null ? null : today.minusDays(transferDaysAgo));
            car.setDeliveryDate(deliveryDaysAgo == null ? null : today.minusDays(deliveryDaysAgo));
            car.setSaleType(type);
            car.setBuyerName(buyer);
            car.setSellerStaff(staff);
            return this;
        }

        D price(long base, long recycle, long transport, long repair, long salePrice) {
            car.setBaseCost(BigDecimal.valueOf(base));
            car.setRecycleFee(BigDecimal.valueOf(recycle));
            car.setTransportFee(BigDecimal.valueOf(transport));
            car.setRepairFee(BigDecimal.valueOf(repair));
            car.setSalePrice(BigDecimal.valueOf(salePrice));
            return this;
        }

        D memo(String memo) {
            car.setMemo(memo);
            return this;
        }

        Car car() {
            car.recalculateDerived();
            return car;
        }
    }
}

package com.usedcar.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/cars の CRUD・バリデーション・重複車台番号の 409 を検証する統合テスト。
 * src/test/resources/application.yml でデモデータは無効なので、存在するのはテストが作成したデータだけ。
 * 車台番号はテストごとに一意にする（H2 はテストコンテキスト内で共有され、ロールバックしない）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CarControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    /** 有効な登録ペイロード（車台番号だけ差し替える） */
    private static Map<String, Object> car(String chassis) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("manufacturer", "トヨタ");
        c.put("model", "プリウス S");
        c.put("chassisNumber", chassis);
        c.put("vehicleType", "DAA-ZY30");
        c.put("firstRegistration", "2019-06");
        c.put("inspectionExpiry", LocalDate.now().plusYears(1).toString());
        c.put("mileage", 48_000);
        c.put("stockDate", LocalDate.now().minusDays(10).toString());
        c.put("stockType", "BUY_OUT");
        c.put("supplierName", "テスト仕入先");
        c.put("buyerStaff", "山田 太郎");
        c.put("baseCost", 900_000);
        c.put("recycleFee", 12_000);
        c.put("transportFee", 15_000);
        c.put("repairFee", 30_000);
        c.put("salePrice", 1_450_000);
        return c;
    }

    private String body(Map<String, Object> payload) throws Exception {
        return json.writeValueAsString(payload);
    }

    /** 登録して ID を返す */
    private long createCar(String chassis) throws Exception {
        String response = mvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(car(chassis))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test
    void createDerivesTotalCostAndGrossProfit() throws Exception {
        Map<String, Object> payload = car("TEST-DERIVE-001");

        String response = mvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.chassisNumber").value("TEST-DERIVE-001"))
            .andExpect(jsonPath("$.status").value("IN_STOCK"))
            .andExpect(jsonPath("$.stockDays").value(10))
            .andReturn().getResponse().getContentAsString();

        // BigDecimal はスケール込みでシリアライズされるので compareTo で検証する
        JsonNode created = json.readTree(response);
        assertThat(new BigDecimal(created.get("totalCost").asText()))
            .isEqualByComparingTo("957000.00");   // 900,000 + 12,000 + 15,000 + 30,000
        assertThat(new BigDecimal(created.get("grossProfit").asText()))
            .isEqualByComparingTo("493000.00");   // 1,450,000 - 957,000
    }

    @Test
    void blankChassisNumberIsRejectedWithFieldErrors() throws Exception {
        Map<String, Object> payload = car("");
        payload.put("chassisNumber", "  ");

        mvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(payload)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.fieldErrors.chassisNumber").isNotEmpty());
    }

    @Test
    void deliveryBeforeContractIsRejectedByClassLevelConstraint() throws Exception {
        Map<String, Object> payload = car("TEST-DATE-001");
        payload.put("contractDate", LocalDate.now().minusDays(30).toString());
        payload.put("transferDate", LocalDate.now().minusDays(20).toString());
        payload.put("deliveryDate", LocalDate.now().minusDays(40).toString()); // 契約日より前 -> 不正
        payload.put("saleType", "RETAIL");
        payload.put("buyerName", "テスト購入者");

        mvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(payload)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("入力内容に不正な項目があります"))
            .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void duplicateChassisNumberReturnsConflict() throws Exception {
        createCar("TEST-DUP-001");

        mvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(car("TEST-DUP-001"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.fieldErrors.chassisNumber").isNotEmpty());
    }

    @Test
    void listAppliesStatusFilterAndPaging() throws Exception {
        createCar("TEST-LIST-001");
        createCar("TEST-LIST-002");

        String response = mvc.perform(get("/api/cars")
                .param("status", "IN_STOCK")
                .param("sort", "stockDate")
                .param("dir", "desc")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode page = json.readTree(response);
        assertThat(page.get("content")).hasSize(1);
        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(2L);
        assertThat(page.get("totalPages").asLong()).isGreaterThanOrEqualTo(2L);
        assertThat(page.get("first").asBoolean()).isTrue();
        assertThat(page.get("content").get(0).get("status").asText()).isEqualTo("IN_STOCK");
    }

    @Test
    void unknownIdReturnsNotFound() throws Exception {
        mvc.perform(get("/api/cars/999999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void updateThenDeleteLifecycle() throws Exception {
        long id = createCar("TEST-CRUD-001");

        Map<String, Object> updated = car("TEST-CRUD-001");
        updated.put("model", "プリウス G");
        updated.put("mileage", 51_000);

        mvc.perform(put("/api/cars/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(updated)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.model").value("プリウス G"))
            .andExpect(jsonPath("$.mileage").value(51_000));

        mvc.perform(delete("/api/cars/" + id))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/cars/" + id))
            .andExpect(status().isNotFound());
    }
}

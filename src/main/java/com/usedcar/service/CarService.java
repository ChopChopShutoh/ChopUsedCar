package com.usedcar.service;

import com.usedcar.domain.Car;
import com.usedcar.domain.SaleType;
import com.usedcar.domain.StockType;
import com.usedcar.dto.CarQuery;
import com.usedcar.dto.CarRequest;
import com.usedcar.dto.CarResponse;
import com.usedcar.dto.PageResponse;
import com.usedcar.dto.StatusFilter;
import com.usedcar.repository.CarRepository;
import com.usedcar.repository.CarSpecifications;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class CarService {

    private static final Map<String, String> SORTABLE_FIELDS;
    private static final int MAX_PAGE_SIZE = 200;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("manufacturer", "manufacturer");
        m.put("model", "model");
        m.put("chassisNumber", "chassisNumber");
        m.put("firstRegistration", "firstRegistration");
        m.put("inspectionExpiry", "inspectionExpiry");
        m.put("mileage", "mileage");
        m.put("stockDate", "stockDate");
        m.put("scheduledStockDate", "scheduledStockDate");
        m.put("contractDate", "contractDate");
        m.put("deliveryDate", "deliveryDate");
        m.put("baseCost", "baseCost");
        m.put("totalCost", "totalCost");
        m.put("salePrice", "salePrice");
        m.put("grossProfit", "grossProfit");
        m.put("createdAt", "createdAt");
        m.put("updatedAt", "updatedAt");
        // 在庫日数は stockDate の逆順で表現できる
        m.put("stockDays", "stockDate");
        SORTABLE_FIELDS = Map.copyOf(m);
    }

    private final CarRepository repository;
    private final EntityManager entityManager;

    public CarService(CarRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    // ===== CRUD =====

    @Transactional(readOnly = true)
    public PageResponse<CarResponse> search(CarQuery query) {
        Page<Car> page = repository.findAll(buildSpecification(query), toPageable(query));
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public List<CarResponse> findAll(CarQuery query) {
        return repository.findAll(buildSpecification(query), toPageable(query)).stream()
            .map(CarResponse::of)
            .toList();
    }

    @Transactional(readOnly = true)
    public CarResponse get(Long id) {
        return CarResponse.of(load(id));
    }

    public CarResponse create(CarRequest request) {
        Car car = new Car();
        apply(car, request);
        assertChassisNumberUnique(request.chassisNumber(), null);
        return CarResponse.of(persist(car));
    }

    public CarResponse update(Long id, CarRequest request) {
        Car car = load(id);
        assertChassisNumberUnique(request.chassisNumber(), id);
        apply(car, request);
        return CarResponse.of(persist(car));
    }

    public void delete(Long id) {
        Car car = load(id);
        repository.delete(car);
    }

    // ===== 内部処理 =====

    private Car load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("指定の在庫データが存在しません (id=" + id + ")"));
    }

    private Car persist(Car car) {
        try {
            Car saved = repository.saveAndFlush(car);
            entityManager.flush();
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateChassisNumberException(car.getChassisNumber());
        }
    }

    private void assertChassisNumberUnique(String chassisNumber, Long excludingId) {
        String normalized = normalizeChassis(chassisNumber);
        repository.findByChassisNumberIgnoreCase(normalized).ifPresent(existing -> {
            if (excludingId == null || !existing.getId().equals(excludingId)) {
                throw new DuplicateChassisNumberException(normalized);
            }
        });
    }

    private void apply(Car car, CarRequest r) {
        car.setManufacturer(trim(r.manufacturer()));
        car.setModel(trim(r.model()));
        car.setChassisNumber(normalizeChassis(r.chassisNumber()));
        car.setVehicleType(blankToNull(r.vehicleType()));
        car.setFirstRegistration(r.firstRegistration());
        car.setInspectionExpiry(r.inspectionExpiry());
        car.setMileage(r.mileage());

        car.setStockDate(r.stockDate());
        car.setScheduledStockDate(r.scheduledStockDate());
        car.setStockType(r.stockType());
        car.setSupplierName(blankToNull(r.supplierName()));
        car.setBuyerStaff(blankToNull(r.buyerStaff()));

        car.setContractDate(r.contractDate());
        car.setTransferDate(r.transferDate());
        car.setDeliveryDate(r.deliveryDate());
        car.setSaleType(r.saleType());
        car.setBuyerName(blankToNull(r.buyerName()));
        car.setSellerStaff(blankToNull(r.sellerStaff()));

        car.setBaseCost(r.baseCost());
        car.setRecycleFee(r.recycleFee());
        car.setTransportFee(r.transportFee());
        car.setRepairFee(r.repairFee());
        car.setSalePrice(r.salePrice());

        car.setMemo(blankToNull(r.memo()));

        car.recalculateDerived();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeChassis(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    // ===== 検索条件 =====

    private Specification<Car> buildSpecification(CarQuery q) {
        Specification<Car> spec = q.status().toSpecification();

        if (q.search() != null && !q.search().isBlank()) {
            spec = spec.and(CarSpecifications.keyword(q.search()));
        }
        if (q.manufacturer() != null && !q.manufacturer().isBlank()) {
            spec = spec.and(CarSpecifications.manufacturerEquals(q.manufacturer().trim()));
        }
        if (q.stockType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("stockType"), q.stockType()));
        }
        if (q.saleType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("saleType"), q.saleType()));
        }
        if (q.stockDateFrom() != null || q.stockDateTo() != null) {
            LocalDate from = q.stockDateFrom();
            LocalDate to = q.stockDateTo();
            if (from != null && to != null && from.isAfter(to)) {
                LocalDate swapped = from;
                from = to;
                to = swapped;
            }
            spec = spec.and(CarSpecifications.stockDateBetween(from, to));
        }
        return spec;
    }

    private PageRequest toPageable(CarQuery q) {
        int page = Math.max(0, q.page());
        int size = Math.min(Math.max(1, q.size()), MAX_PAGE_SIZE);

        String field = q.sortField() == null || q.sortField().isBlank() ? "stockDate" : q.sortField();
        String resolved = SORTABLE_FIELDS.get(field);
        if (resolved == null) {
            resolved = "stockDate";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(q.sortDirection())
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        // 在庫日数で並べ替える場合は stockDate を逆順にする（日数が長い = 入庫日が古い）
        if ("stockDays".equals(field)) {
            direction = direction == Sort.Direction.DESC ? Sort.Direction.ASC : Sort.Direction.DESC;
        }

        // JPA Criteria では NULLS LAST が未対応のため、並び順だけ指定する（H2 の既定の NULL 扱いに任せる）
        Sort sort = Sort.by(direction, resolved);
        return PageRequest.of(page, size, sort);
    }

    private PageResponse<CarResponse> toPageResponse(Page<Car> page) {
        return new PageResponse<>(
            page.getContent().stream().map(CarResponse::of).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    // ===== メタ情報（フォームのサジェスト用） =====

    @Transactional(readOnly = true)
    public Map<String, Object> meta() {
        java.util.Set<String> staff = new java.util.LinkedHashSet<>();
        staff.addAll(repository.findAllBuyerStaff());
        staff.addAll(repository.findAllSellerStaff());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("statuses", java.util.Arrays.stream(StatusFilter.values())
            .map(s -> Map.of("value", s.name(), "label", s.label())).toList());
        meta.put("stockTypes", java.util.Arrays.stream(StockType.values())
            .map(t -> Map.of("value", t.name(), "label", t.label())).toList());
        meta.put("saleTypes", java.util.Arrays.stream(SaleType.values())
            .map(t -> Map.of("value", t.name(), "label", t.label())).toList());
        meta.put("manufacturers", repository.findAllManufacturers());
        meta.put("suppliers", repository.findAllSuppliers());
        meta.put("buyers", repository.findAllBuyers());
        meta.put("staff", List.copyOf(staff));
        return meta;
    }
}

package com.usedcar.repository;

import com.usedcar.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

    Optional<Car> findByChassisNumberIgnoreCase(String chassisNumber);

    boolean existsByChassisNumberIgnoreCase(String chassisNumber);

    @Query("select distinct c.manufacturer from Car c order by c.manufacturer")
    List<String> findAllManufacturers();

    @Query("select distinct c.supplierName from Car c where c.supplierName is not null order by c.supplierName")
    List<String> findAllSuppliers();

    @Query("select distinct c.buyerName from Car c where c.buyerName is not null order by c.buyerName")
    List<String> findAllBuyers();

    @Query("select distinct c.buyerStaff from Car c where c.buyerStaff is not null order by c.buyerStaff")
    List<String> findAllBuyerStaff();

    @Query("select distinct c.sellerStaff from Car c where c.sellerStaff is not null order by c.sellerStaff")
    List<String> findAllSellerStaff();
}

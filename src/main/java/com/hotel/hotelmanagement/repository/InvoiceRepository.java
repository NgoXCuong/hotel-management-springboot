package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    Optional<Invoice> findByBookingId(Long bookingId);

    boolean existsByInvoiceCode(String invoiceCode);

    long countByStatus(Invoice.InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = 'ISSUED'")
    BigDecimal sumTotalRevenue();
}

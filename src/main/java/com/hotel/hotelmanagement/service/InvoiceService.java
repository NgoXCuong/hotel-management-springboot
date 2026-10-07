package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.entity.Invoice;
import com.hotel.hotelmanagement.entity.Payment;
import com.hotel.hotelmanagement.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceService {

    List<Invoice> findAll();

    Optional<Invoice> findById(Long id);

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    Optional<Invoice> findByBookingId(Long bookingId);

    String generateInvoiceCode();

    Invoice createOrGetDraftInvoice(Long bookingId, BigDecimal vatRate, User issuedBy);

    Invoice processPaymentAndIssue(Long invoiceId, Payment.PaymentMethod paymentMethod, BigDecimal paymentAmount, String transactionCode, String note, User cashier);

    Invoice cancelInvoice(Long id, String reason);

    List<Invoice> searchInvoices(String keyword, Invoice.InvoiceStatus status, LocalDate fromDate, LocalDate toDate);

    org.springframework.data.domain.Page<Invoice> searchInvoices(String keyword, Invoice.InvoiceStatus status, LocalDate fromDate, LocalDate toDate, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Invoice> findAll(org.springframework.data.domain.Pageable pageable);

    long countTotal();

    long countIssued();

    long countDraft();

    long countCancelled();

    BigDecimal calculateTotalRevenue();
}

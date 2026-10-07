package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.dto.RevenueReportDTO;
import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.Invoice;
import com.hotel.hotelmanagement.entity.RoomType;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.InvoiceRepository;
import com.hotel.hotelmanagement.repository.RoomTypeRepository;
import com.hotel.hotelmanagement.service.RevenueService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class RevenueServiceImpl implements RevenueService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RevenueServiceImpl(
            InvoiceRepository invoiceRepository,
            BookingRepository bookingRepository,
            RoomTypeRepository roomTypeRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Override
    public RevenueReportDTO getRevenueReport(String period, LocalDate fromDate, LocalDate toDate, Long roomTypeId, Booking.BookingSource source) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;

        if ("today".equalsIgnoreCase(period)) {
            start = today;
            end = today;
        } else if ("week".equalsIgnoreCase(period)) {
            start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        } else if ("year".equalsIgnoreCase(period)) {
            start = today.with(TemporalAdjusters.firstDayOfYear());
            end = today.with(TemporalAdjusters.lastDayOfYear());
        } else if ("custom".equalsIgnoreCase(period) && fromDate != null && toDate != null) {
            start = fromDate;
            end = toDate;
        } else {
            // Mặc định là 'month'
            period = "month";
            start = today.with(TemporalAdjusters.firstDayOfMonth());
            end = today.with(TemporalAdjusters.lastDayOfMonth());
        }

        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        List<Invoice> allInvoices = invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Booking> allBookings = bookingRepository.findAll();

        // Lọc Hóa đơn theo khoảng ngày và điều kiện
        List<Invoice> filteredInvoices = allInvoices.stream()
                .filter(inv -> {
                    LocalDateTime dt = inv.getIssuedAt() != null ? inv.getIssuedAt() : inv.getCreatedAt();
                    if (dt == null) return false;
                    boolean inRange = !dt.isBefore(startDt) && !dt.isAfter(endDt);
                    if (!inRange) return false;

                    if (source != null && inv.getBooking() != null && inv.getBooking().getSource() != source) {
                        return false;
                    }
                    if (roomTypeId != null && inv.getBooking() != null && inv.getBooking().getBookingRooms() != null) {
                        boolean matchRoomType = inv.getBooking().getBookingRooms().stream()
                                .anyMatch(br -> br.getRoom() != null && br.getRoom().getRoomType() != null && br.getRoom().getRoomType().getId().equals(roomTypeId));
                        if (!matchRoomType) return false;
                    }
                    return true;
                })
                .toList();

        // 1. Tính toán 4 chỉ số tài chính tổng hợp
        BigDecimal totalRevenue = filteredInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Nếu totalRevenue từ invoices = 0, tính từ các booking trong kỳ
        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
            totalRevenue = allBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED && b.getCreatedAt() != null && !b.getCreatedAt().isBefore(startDt) && !b.getCreatedAt().isAfter(endDt))
                    .map(Booking::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalRoomRevenue = filteredInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED && inv.getBooking() != null)
                .map(inv -> inv.getBooking().getRoomTotalAmount())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRoomRevenue.compareTo(BigDecimal.ZERO) == 0) {
            totalRoomRevenue = allBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED && b.getCreatedAt() != null && !b.getCreatedAt().isBefore(startDt) && !b.getCreatedAt().isAfter(endDt))
                    .map(Booking::getRoomTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalServiceRevenue = filteredInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED && inv.getBooking() != null)
                .map(inv -> inv.getBooking().getServiceTotalAmount())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = filteredInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .map(Invoice::getDiscount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTax = filteredInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .map(Invoice::getTax)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Biểu đồ cột kép so sánh 12 tháng (Tiền phòng vs Tiền dịch vụ trong năm hiện tại)
        int currentYear = today.getYear();
        List<String> monthlyLabels = new ArrayList<>();
        List<BigDecimal> monthlyRoomRevenue = new ArrayList<>();
        List<BigDecimal> monthlyServiceRevenue = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            monthlyLabels.add("T" + m);
            final int monthVal = m;

            BigDecimal mRoom = allBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED && b.getCreatedAt() != null && b.getCreatedAt().getYear() == currentYear && b.getCreatedAt().getMonthValue() == monthVal)
                    .map(Booking::getRoomTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal mService = allBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED && b.getCreatedAt() != null && b.getCreatedAt().getYear() == currentYear && b.getCreatedAt().getMonthValue() == monthVal)
                    .map(Booking::getServiceTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyRoomRevenue.add(mRoom);
            monthlyServiceRevenue.add(mService);
        }

        // 3. Biểu đồ tròn cơ cấu doanh thu theo Loại phòng
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        List<String> roomTypeLabels = new ArrayList<>();
        List<BigDecimal> roomTypeValues = new ArrayList<>();

        for (RoomType rt : roomTypes) {
            roomTypeLabels.add(rt.getName());

            BigDecimal rtRev = allBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED &&
                            b.getBookingRooms() != null &&
                            b.getBookingRooms().stream().anyMatch(br -> br.getRoom() != null && br.getRoom().getRoomType() != null && br.getRoom().getRoomType().getId().equals(rt.getId())))
                    .map(Booking::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            roomTypeValues.add(rtRev);
        }

        return RevenueReportDTO.builder()
                .period(period)
                .fromDate(start)
                .toDate(end)
                .totalRevenue(totalRevenue)
                .totalRoomRevenue(totalRoomRevenue)
                .totalServiceRevenue(totalServiceRevenue)
                .totalDiscount(totalDiscount)
                .totalTax(totalTax)
                .monthlyLabels(monthlyLabels)
                .monthlyRoomRevenue(monthlyRoomRevenue)
                .monthlyServiceRevenue(monthlyServiceRevenue)
                .roomTypeLabels(roomTypeLabels)
                .roomTypeValues(roomTypeValues)
                .invoices(filteredInvoices)
                .build();
    }

    @Override
    public byte[] exportRevenueReportToCsv(String period, LocalDate fromDate, LocalDate toDate, Long roomTypeId, Booking.BookingSource source) {
        RevenueReportDTO report = getRevenueReport(period, fromDate, toDate, roomTypeId, source);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Microsoft Excel compatibility
        sb.append('\ufeff');

        sb.append("BÁO CÁO DOANH THU KHÁCH SẠN GRAND HOTEL\n");
        sb.append("Thời gian thống kê:;").append(report.getFromDate()).append(" -> ").append(report.getToDate()).append("\n");
        sb.append("Tổng doanh thu thực nhận (VNĐ):;").append(report.getTotalRevenue()).append("\n");
        sb.append("Tổng tiền phòng (VNĐ):;").append(report.getTotalRoomRevenue()).append("\n");
        sb.append("Tổng tiền dịch vụ (VNĐ):;").append(report.getTotalServiceRevenue()).append("\n");
        sb.append("Tổng chiết khấu voucher (VNĐ):;").append(report.getTotalDiscount()).append("\n");
        sb.append("\n");

        sb.append("Mã Hóa đơn;Mã Booking;Khách hàng;Số điện thoại;Ngày phát hành;Tổng tiền (VNĐ);Chiết khấu (VNĐ);Thuế VAT (VNĐ);Trạng thái;Thu ngân\n");

        for (Invoice inv : report.getInvoices()) {
            sb.append(inv.getInvoiceCode()).append(";");
            sb.append(inv.getBooking() != null ? inv.getBooking().getBookingCode() : "").append(";");
            sb.append(inv.getBooking() != null && inv.getBooking().getCustomer() != null ? inv.getBooking().getCustomer().getFullName() : "Khách vãng lai").append(";");
            sb.append(inv.getBooking() != null && inv.getBooking().getCustomer() != null ? inv.getBooking().getCustomer().getPhone() : "").append(";");
            sb.append(inv.getIssuedAt() != null ? inv.getIssuedAt().format(dtf) : (inv.getCreatedAt() != null ? inv.getCreatedAt().format(dtf) : "")).append(";");
            sb.append(inv.getTotalAmount()).append(";");
            sb.append(inv.getDiscount()).append(";");
            sb.append(inv.getTax()).append(";");
            sb.append(inv.getStatus().name()).append(";");
            sb.append(inv.getIssuedBy() != null ? inv.getIssuedBy().getFullName() : "Hệ thống").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}

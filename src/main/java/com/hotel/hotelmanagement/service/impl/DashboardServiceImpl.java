package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.dto.DashboardSummaryDTO;
import com.hotel.hotelmanagement.dto.RoomTypeRevenueDTO;
import com.hotel.hotelmanagement.entity.*;
import com.hotel.hotelmanagement.repository.*;
import com.hotel.hotelmanagement.service.DashboardService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RoomTypeRepository roomTypeRepository;

    public DashboardServiceImpl(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            RoomTypeRepository roomTypeRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<Booking> allBookings = bookingRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        // 1. Doanh thu hôm nay vs Hôm qua
        BigDecimal todayRevenue = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.PAID && p.getPaidAt() != null && p.getPaidAt().toLocalDate().isEqual(today))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Nếu payments hôm nay = 0, tạm tính từ các booking phát sinh hôm nay
        if (todayRevenue.compareTo(BigDecimal.ZERO) == 0) {
            todayRevenue = allBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED && b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isEqual(today))
                    .map(Booking::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal yesterdayRevenue = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.PAID && p.getPaidAt() != null && p.getPaidAt().toLocalDate().isEqual(yesterday))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double growthPercent = 0.0;
        boolean growthPositive = true;
        if (yesterdayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = todayRevenue.subtract(yesterdayRevenue);
            growthPercent = diff.multiply(BigDecimal.valueOf(100))
                    .divide(yesterdayRevenue, 1, RoundingMode.HALF_UP)
                    .doubleValue();
            growthPositive = growthPercent >= 0;
        } else if (todayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthPercent = 100.0;
            growthPositive = true;
        }

        // 2. Công suất phòng (Occupancy Rate)
        long totalRooms = allRooms.size();
        long occupiedRooms = allRooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.OCCUPIED).count();
        long availableRooms = allRooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.AVAILABLE).count();
        long cleaningRooms = allRooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.CLEANING).count();
        long maintenanceRooms = allRooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.MAINTENANCE).count();

        int occupancyRate = totalRooms > 0 ? (int) Math.round((occupiedRooms * 100.0) / totalRooms) : 0;

        // 3. Lượt check-in hôm nay
        long todayCheckIns = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CHECKED_IN ||
                        (b.getActualCheckIn() != null && b.getActualCheckIn().toLocalDate().isEqual(today)) ||
                        (b.getCheckIn() != null && b.getCheckIn().toLocalDate().isEqual(today) && b.getStatus() != Booking.BookingStatus.CANCELLED))
                .count();

        // 4. Số đơn PENDING cần duyệt
        long pendingBookings = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.PENDING)
                .count();

        // 5. Biểu đồ doanh thu (mặc định 7 ngày gần nhất)
        Map<String, Object> chartMap = getRevenueChartData("7");
        @SuppressWarnings("unchecked")
        List<String> chartLabels = (List<String>) chartMap.get("labels");
        @SuppressWarnings("unchecked")
        List<BigDecimal> chartData = (List<BigDecimal>) chartMap.get("data");

        // 6. Phân bổ nguồn đặt phòng
        long walkInCount = allBookings.stream().filter(b -> b.getSource() == Booking.BookingSource.WALK_IN).count();
        long phoneCount = allBookings.stream().filter(b -> b.getSource() == Booking.BookingSource.PHONE).count();
        long onlineCount = allBookings.stream().filter(b -> b.getSource() == Booking.BookingSource.ONLINE).count();
        long otaCount = allBookings.stream().filter(b -> b.getSource() == Booking.BookingSource.OTA).count();

        // 7. Top 5 Loại phòng theo doanh thu
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        BigDecimal grandTotalRev = allBookings.stream()
                .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                .map(Booking::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RoomTypeRevenueDTO> topRoomTypes = new ArrayList<>();
        for (RoomType rt : roomTypes) {
            List<Booking> rtBookings = allBookings.stream()
                    .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED &&
                            b.getBookingRooms() != null &&
                            b.getBookingRooms().stream().anyMatch(br -> br.getRoom() != null && br.getRoom().getRoomType() != null && br.getRoom().getRoomType().getId().equals(rt.getId())))
                    .toList();

            BigDecimal rtRev = rtBookings.stream()
                    .map(Booking::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int pct = (grandTotalRev.compareTo(BigDecimal.ZERO) > 0)
                    ? rtRev.multiply(BigDecimal.valueOf(100)).divide(grandTotalRev, 0, RoundingMode.HALF_UP).intValue()
                    : 0;

            topRoomTypes.add(RoomTypeRevenueDTO.builder()
                    .roomTypeId(rt.getId())
                    .roomTypeName(rt.getName())
                    .totalBookings((long) rtBookings.size())
                    .totalRevenue(rtRev)
                    .percentage(pct)
                    .build());
        }
        topRoomTypes.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        if (topRoomTypes.size() > 5) {
            topRoomTypes = topRoomTypes.subList(0, 5);
        }

        // 8. 5 Đơn đặt phòng mới nhất
        List<Booking> recentBookings = allBookings.stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        return DashboardSummaryDTO.builder()
                .todayRevenue(todayRevenue)
                .revenueGrowthPercent(Math.abs(growthPercent))
                .growthPositive(growthPositive)
                .occupancyRate(occupancyRate)
                .occupiedRooms(occupiedRooms)
                .totalRooms(totalRooms)
                .todayCheckIns(todayCheckIns)
                .pendingBookings(pendingBookings)
                .revenueChartLabels(chartLabels)
                .revenueChartData(chartData)
                .availableRooms(availableRooms)
                .cleaningRooms(cleaningRooms)
                .maintenanceRooms(maintenanceRooms)
                .walkInCount(walkInCount)
                .phoneCount(phoneCount)
                .onlineCount(onlineCount)
                .otaCount(otaCount)
                .topRoomTypes(topRoomTypes)
                .recentBookings(recentBookings)
                .build();
    }

    @Override
    public Map<String, Object> getRevenueChartData(String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;
        String periodLabel;

        if ("14".equals(period) || "14days".equalsIgnoreCase(period)) {
            startDate = today.minusDays(13);
            periodLabel = "14 ngày qua";
        } else if ("30".equals(period) || "30days".equalsIgnoreCase(period)) {
            startDate = today.minusDays(29);
            periodLabel = "30 ngày qua";
        } else if ("this_month".equalsIgnoreCase(period)) {
            startDate = today.withDayOfMonth(1);
            periodLabel = "Tháng này (" + today.getMonthValue() + "/" + today.getYear() + ")";
        } else if ("last_month".equalsIgnoreCase(period)) {
            java.time.YearMonth lastMonth = java.time.YearMonth.from(today).minusMonths(1);
            startDate = lastMonth.atDay(1);
            endDate = lastMonth.atEndOfMonth();
            periodLabel = "Tháng trước (" + lastMonth.getMonthValue() + "/" + lastMonth.getYear() + ")";
        } else { // mặc định: 7 ngày
            startDate = today.minusDays(6);
            periodLabel = "7 ngày qua";
        }

        List<Payment> allPayments = paymentRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();

        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate curDate = startDate; !curDate.isAfter(endDate); curDate = curDate.plusDays(1)) {
            final LocalDate date = curDate;
            labels.add(date.format(dtf));

            BigDecimal dayRev = allPayments.stream()
                    .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.PAID && p.getPaidAt() != null && p.getPaidAt().toLocalDate().isEqual(date))
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (dayRev.compareTo(BigDecimal.ZERO) == 0) {
                dayRev = allBookings.stream()
                        .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED && b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isEqual(date))
                        .map(Booking::getTotalAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            data.add(dayRev);
            totalRevenue = totalRevenue.add(dayRev);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        result.put("totalRevenue", totalRevenue);
        result.put("periodLabel", periodLabel);
        result.put("period", period);

        return result;
    }
}

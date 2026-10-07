package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.Promotion;
import com.hotel.hotelmanagement.repository.PromotionRepository;
import com.hotel.hotelmanagement.service.PromotionService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public List<Promotion> findAll() {
        return promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public Optional<Promotion> findById(Long id) {
        return promotionRepository.findById(id);
    }

    @Override
    public Optional<Promotion> findByCode(String code) {
        if (!StringUtils.hasText(code)) return Optional.empty();
        return promotionRepository.findByCode(code.trim().toUpperCase());
    }

    @Override
    @Transactional
    public Promotion save(Promotion promotion) {
        // Chuẩn hóa mã code thành chữ in hoa, không khoảng trắng
        if (StringUtils.hasText(promotion.getCode())) {
            promotion.setCode(promotion.getCode().trim().toUpperCase().replaceAll("\\s+", ""));
        }

        if (promotion.getName() != null) {
            promotion.setName(promotion.getName().trim());
        }
        if (promotion.getDescription() != null) {
            promotion.setDescription(promotion.getDescription().trim());
            if (promotion.getDescription().isBlank()) {
                promotion.setDescription(null);
            }
        }

        if (promotion.getMinOrderAmount() == null) {
            promotion.setMinOrderAmount(BigDecimal.ZERO);
        }
        if (promotion.getUsedCount() == null) {
            promotion.setUsedCount(0);
        }
        if (promotion.getActive() == null) {
            promotion.setActive(true);
        }

        // Nếu là giảm giá cố định -> xóa mức giảm tối đa
        if (promotion.getDiscountType() == Promotion.DiscountType.FIXED_AMOUNT) {
            promotion.setMaxDiscountAmount(null);
        }

        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        promotionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        promotionRepository.findById(id).ifPresent(promotion -> {
            promotion.setActive(!promotion.getActive());
            promotionRepository.save(promotion);
        });
    }

    @Override
    public boolean existsByCode(String code) {
        if (!StringUtils.hasText(code)) return false;
        return promotionRepository.existsByCode(code.trim().toUpperCase());
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, Long id) {
        if (!StringUtils.hasText(code) || id == null) return false;
        return promotionRepository.existsByCodeAndIdNot(code.trim().toUpperCase(), id);
    }

    @Override
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal orderAmount) {
        if (promotion == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện đơn hàng tối thiểu
        if (promotion.getMinOrderAmount() != null && orderAmount.compareTo(promotion.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        if (promotion.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
            // Giảm theo %: orderAmount * (discountValue / 100)
            BigDecimal percent = promotion.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = orderAmount.multiply(percent).setScale(2, RoundingMode.HALF_UP);

            // Giới hạn mức giảm tối đa nếu có thiết lập
            if (promotion.getMaxDiscountAmount() != null && promotion.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(promotion.getMaxDiscountAmount());
            }
        } else if (promotion.getDiscountType() == Promotion.DiscountType.FIXED_AMOUNT) {
            // Giảm số tiền cố định
            discount = promotion.getDiscountValue();
        }

        // Không bao giờ giảm vượt quá giá trị đơn hàng
        return discount.min(orderAmount);
    }

    @Override
    public boolean checkValidity(String code, BigDecimal orderAmount) {
        Optional<Promotion> opt = findByCode(code);
        if (opt.isEmpty()) return false;

        Promotion promotion = opt.get();
        if (!promotion.isValidNow()) return false;

        if (orderAmount != null && promotion.getMinOrderAmount() != null) {
            return orderAmount.compareTo(promotion.getMinOrderAmount()) >= 0;
        }

        return true;
    }

    private Specification<Promotion> createPromotionSpec(String keyword, Promotion.DiscountType discountType, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // 1. Tìm kiếm theo mã Voucher hoặc tên chương trình
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch));
            }

            // 2. Lọc theo Loại giảm giá
            if (discountType != null) {
                predicates.add(cb.equal(root.get("discountType"), discountType));
            }

            // 3. Lọc theo Trạng thái
            if (StringUtils.hasText(status)) {
                String upperStatus = status.trim().toUpperCase();
                switch (upperStatus) {
                    case "ONGOING":
                    case "ACTIVE":
                        predicates.add(cb.isTrue(root.get("active")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), now));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), now));
                        break;
                    case "EXPIRED":
                        predicates.add(cb.lessThan(root.get("endDate"), now));
                        break;
                    case "UPCOMING":
                        predicates.add(cb.greaterThan(root.get("startDate"), now));
                        break;
                    case "DISABLED":
                    case "LOCKED":
                        predicates.add(cb.isFalse(root.get("active")));
                        break;
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<Promotion> searchPromotions(String keyword, Promotion.DiscountType discountType, String status) {
        return promotionRepository.findAll(createPromotionSpec(keyword, discountType, status), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public org.springframework.data.domain.Page<Promotion> searchPromotions(String keyword, Promotion.DiscountType discountType, String status, org.springframework.data.domain.Pageable pageable) {
        return promotionRepository.findAll(createPromotionSpec(keyword, discountType, status), pageable);
    }

    @Override
    public org.springframework.data.domain.Page<Promotion> findAll(org.springframework.data.domain.Pageable pageable) {
        return promotionRepository.findAll(pageable);
    }

    @Override
    public long countTotal() {
        return promotionRepository.count();
    }

    @Override
    public long countRunning() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findAll().stream()
                .filter(Promotion::isValidNow)
                .count();
    }

    @Override
    public long countExpired() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findAll().stream()
                .filter(Promotion::isExpired)
                .count();
    }
}

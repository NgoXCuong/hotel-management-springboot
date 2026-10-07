package com.hotel.hotelmanagement.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Tự động loại bỏ khoảng trắng thừa từ tham số chuỗi,
 * và chuyển đổi chuỗi rỗng (" \) thành null để tránh lỗi TypeMismatchException / HTTP 400
 * khi Spring MVC phân tích các tham số dạng LocalDate, Integer, BigDecimal...
 */
@ControllerAdvice
public class WebDataBinderAdvice {

 @InitBinder
 public void initBinder(WebDataBinder binder) {
 binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
 }
}
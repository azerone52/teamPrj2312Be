package com.example.prj3be.controller;

import com.example.prj3be.config.PaymentConfig;
import com.example.prj3be.domain.Member;
import com.example.prj3be.dto.PaymentDto;
import com.example.prj3be.dto.PaymentRequestDto;
import com.example.prj3be.dto.PaymentResDto;
import com.example.prj3be.exception.CustomLogicException;
import com.example.prj3be.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentConfig paymentConfig;

    public PaymentController(PaymentService paymentService, PaymentConfig paymentConfig) {
        this.paymentService = paymentService;
        this.paymentConfig = paymentConfig;
    }
    @PostMapping("/toss")
    public ResponseEntity requestTossPayment(@RequestBody @Valid PaymentDto paymentReqDto) throws CustomLogicException {
        PaymentResDto paymentResDto = paymentService.requestTossPayment(paymentReqDto.toEntity(), paymentReqDto.getEmail()).toPaymentResDto();
        paymentResDto.setSuccessUrl(paymentReqDto.getSuccessUrl() == null ? paymentConfig.getSuccessUrl() : paymentReqDto.getSuccessUrl());
        paymentResDto.setFailUrl(paymentReqDto.getFailUrl() == null ? paymentConfig.getFailUrl() : paymentReqDto.getFailUrl());
        return ResponseEntity.ok(paymentResDto);
    }
    @PostMapping("/toss/success")
    public ResponseEntity tossPaymentSuccess(@RequestBody PaymentRequestDto dto) throws JSONException {
        String paymentKey = dto.getPaymentKey();
        String orderId = dto.getPaymentUid();
        Long amount = dto.getAmount();
        return ResponseEntity.ok().body(paymentService.tossPaymentSuccess(paymentKey,orderId,amount));
    }
    // 결제 취소 로직
    @PostMapping("/toss/cancel")
    public ResponseEntity tossPaymentCancel(Member member, @RequestParam String paymentKey, @RequestParam String cancelReason) {
        return ResponseEntity.ok().body(paymentService.canclePayment(member.getEmail(),paymentKey,cancelReason));
    }

}
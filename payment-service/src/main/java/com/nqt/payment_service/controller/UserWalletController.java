//package com.nqt.payment_service.controller;
//
//import com.nqt.common_starter.dto.response.APIResponse;
//import com.nqt.payment_service.dto.response.UserWalletResponse;
//import com.nqt.payment_service.service.UserWalletService;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//@RequestMapping("/wallet")
//public class UserWalletController {
//    UserWalletService userWalletService;
//
//    @PostMapping
//    public ResponseEntity<APIResponse<UserWalletResponse>> create(@RequestBody String userCode) {
//        return ResponseEntity.status(201).body(APIResponse.response(
//                201,
//                "Create wallet successfully",
//                userWalletService.create(userCode)
//        ));
//    }
//    @GetMapping("/mine")
//    public ResponseEntity<APIResponse<UserWalletResponse>> getUserWalletOfMine() {
//        return ResponseEntity.ok(APIResponse.response(
//                200,
//                "Get my wallet successfully",
//                userWalletService.getUserWalletOfMine()
//        ));
//    }
//    @PatchMapping("/{userCode}/deposit")
//    public ResponseEntity<APIResponse<UserWalletResponse>> deposit(
//            @PathVariable String userCode,
//            @RequestBody String amount
//    ) {
//        return ResponseEntity.ok(APIResponse.response(
//                200,
//                "Deposit successfully",
//                userWalletService.deposit(userCode, amount)
//        ));
//    }
//
//    @PatchMapping("/{userCode}/withdraw")
//    public ResponseEntity<APIResponse<UserWalletResponse>> withdraw(
//            @PathVariable String userCode,
//            @RequestBody String amount
//    ) {
//        return ResponseEntity.ok(APIResponse.response(
//                200,
//                "Withdraw successfully",
//                userWalletService.withdraw(userCode, amount)
//        ));
//    }
//}

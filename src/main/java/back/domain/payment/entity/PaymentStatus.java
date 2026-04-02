package back.domain.payment.entity;

public enum PaymentStatus {
    READY, //결제 요청
    PAID,  // 결제 성공
    FAILED,  // 결제 실패
    CANCELED  //결제 취소
}

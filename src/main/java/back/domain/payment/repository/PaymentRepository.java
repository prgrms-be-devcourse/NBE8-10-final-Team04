package back.domain.payment.repository;

import back.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    @Query("SELECT p FROM Payment p WHERE p.member.id = :memberId " +
            "AND p.status = 'PAID' " +
            "AND p.createdAt >= CURRENT_DATE")
    Optional<Payment> findTodayPaymentByMemberId(@Param("memberId") Long memberId);
}

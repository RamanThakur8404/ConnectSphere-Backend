package com.connectsphere.auth.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.auth.config.RabbitMQConfig;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.event.PaymentSuccessEvent;
import com.connectsphere.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_SUCCESS)
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent for userId: {}, paymentType: {}", event.getUserId(), event.getPaymentType());

        if (event.getUserId() == null || event.getPaymentType() == null) {
            log.warn("Invalid PaymentSuccessEvent received: {}", event);
            return;
        }

        if ("PREMIUM".equalsIgnoreCase(event.getPaymentType()) || "SUBSCRIPTION".equalsIgnoreCase(event.getPaymentType())) {
            userRepository.findById(event.getUserId()).ifPresentOrElse(user -> {
                user.setPremium(true);
                userRepository.save(user);
                log.info("User {} is now a Premium subscriber.", user.getUserId());
            }, () -> {
                log.error("User with ID {} not found while processing PaymentSuccessEvent.", event.getUserId());
            });
        }
    }
}

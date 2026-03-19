package com.premiersport.order.service;

import com.premiersport.order.entity.CartEntity;
import com.premiersport.order.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@premier-sport.com}")
    private String fromAddress;

    @Async
    public void sendOrderConfirmation(OrderEntity order) {
        String customerEmail = order.getCustomerEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("No customer email on order {} — confirmation email skipped", order.getOrderNumber());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(customerEmail);
            helper.setSubject("Order Confirmation – " + order.getOrderNumber());
            // Fix #14: use two-arg setText to set both plain-text and HTML correctly
            helper.setText(buildPlainText(order), buildHtml(order));

            mailSender.send(message);
            log.info("Order confirmation email sent to {} for order {}", customerEmail, order.getOrderNumber());
        } catch (MessagingException e) {
            log.error("Failed to send order confirmation email for {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private String buildPlainText(OrderEntity order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thank you for your order!\n\n");
        sb.append("Order Number: ").append(order.getOrderNumber()).append("\n\n");
        sb.append("Items:\n");
        for (CartEntity.CartItem item : order.getItems()) {
            sb.append("  - ").append(item.getProductName())
              .append(" (Size: ").append(item.getSize()).append(")")
              .append(" x").append(item.getQuantity())
              .append("  £").append(String.format("%.2f", item.getUnitPrice() * item.getQuantity()))
              .append("\n");
        }
        sb.append("\nTotal (incl. delivery): £").append(String.format("%.2f", order.getTotal())).append("\n");
        sb.append("\nEstimated delivery: 3-5 business days\n");
        return sb.toString();
    }

    private String buildHtml(OrderEntity order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style='font-family:sans-serif;color:#111;'>");
        sb.append("<h2>Thank you for your order!</h2>");
        sb.append("<p><strong>Order Number:</strong> ").append(order.getOrderNumber()).append("</p>");
        sb.append("<table border='0' cellpadding='8' style='border-collapse:collapse;width:100%;max-width:600px;'>");
        sb.append("<thead><tr style='background:#f3f4f6;'>")
          .append("<th align='left'>Product</th><th align='left'>Size</th>")
          .append("<th align='right'>Qty</th><th align='right'>Price</th></tr></thead>");
        sb.append("<tbody>");
        for (CartEntity.CartItem item : order.getItems()) {
            sb.append("<tr>")
              .append("<td>").append(item.getProductName()).append("</td>")
              .append("<td>").append(item.getSize()).append("</td>")
              .append("<td align='right'>").append(item.getQuantity()).append("</td>")
              .append("<td align='right'>£")
              .append(String.format("%.2f", item.getUnitPrice() * item.getQuantity()))
              .append("</td></tr>");
        }
        sb.append("</tbody>");
        sb.append("<tfoot><tr><td colspan='3' align='right'><strong>Total (incl. delivery)</strong></td>")
          .append("<td align='right'><strong>£")
          .append(String.format("%.2f", order.getTotal()))
          .append("</strong></td></tr></tfoot>");
        sb.append("</table>");
        sb.append("<p style='color:#6b7280;font-size:0.875rem;'>Estimated delivery: 3-5 business days</p>");
        sb.append("</body></html>");
        return sb.toString();
    }
}

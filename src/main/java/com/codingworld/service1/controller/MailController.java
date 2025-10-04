package com.codingworld.service1.controller;

import com.codingworld.dto.BulkEmailRequest;
import com.codingworld.dto.BulkEmailResponse;
import com.codingworld.dto.RecipientResult;
import com.codingworld.service1.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mail")
public class MailController {

    private final EmailService emailService;

    // Inject spring.mail.username from configuration
    @Value("${spring.mail.username}")
    private String from;

    @Value("${spring.mail.display}")
    private String display;

    public MailController(EmailService emailService) {
        this.emailService = emailService;
    }

    // POST /mail/send-bulk  (application/json)
    @PostMapping(
            value = "/send-bulk",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BulkEmailResponse sendBulk(@RequestBody BulkEmailRequest req) {
        if (req.to == null || req.to.isEmpty()) {
            throw new IllegalArgumentException("'to' must contain at least one recipient");
        }

        String subject = (req.subject == null) ? "(no subject)" : req.subject;
        String body = (req.body == null) ? "" : req.body;

        List<RecipientResult> results = emailService.sendIndividually(
                from, display, req.placeHolders, req.to, subject, body, req.html, req.attachments
        );

        int sent = (int) results.stream().filter(r -> r.success).count();
        return new BulkEmailResponse(req.to.size(), sent, results.size() - sent, results);
    }
}

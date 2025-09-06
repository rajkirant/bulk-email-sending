package com.codingworld.service1.controller;

import com.codingworld.dto.BulkEmailRequest;
import com.codingworld.dto.BulkEmailResponse;
import com.codingworld.dto.RecipientResult;
import com.codingworld.service1.EmailService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mail")
public class MailController {

    private final EmailService emailService;

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
        String body    = (req.body == null)    ? ""             : req.body;

        // MUST match your spring.mail.username (your working Gmail address)
        String from = "rajkiran047@gmail.com";

        List<RecipientResult> results = emailService.sendIndividually(
                from, req.to, subject, body, req.html, req.attachments
        );
        int sent = (int) results.stream().filter(r -> r.success).count();
        return new BulkEmailResponse(req.to.size(), sent, results.size() - sent, results);
    }
}

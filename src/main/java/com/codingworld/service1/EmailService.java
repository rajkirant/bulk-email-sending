package com.codingworld.service1;

import com.codingworld.dto.RecipientResult;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Send ONE email to ONE recipient
    public void sendOne(String from, String firstName, String to, String subject, String body, boolean html, List<String> attachmentPaths)
            throws MessagingException {

        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        body = body.replace("[First Name]", firstName);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, html);

        if (attachmentPaths != null) {
            for (String path : attachmentPaths) {
                if (path == null || path.isBlank()) continue;
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    helper.addAttachment(f.getName(), new FileSystemResource(f));
                }
                // else: skip missing file; or throw if you prefer strict behavior
            }
        }

        mailSender.send(mime);
    }

    public List<RecipientResult> sendIndividually(String from, List<String> recipients, String subject, String body,
                                                  boolean html, List<String> attachmentPaths) {
        List<RecipientResult> results = new ArrayList<>();
        if (recipients == null) return results;

        for (String r : recipients) {
            if (r == null) continue;
            String to = r.trim();
            if (to.isEmpty()) continue;

            try {
            	System.out.println(guessFirstName(to));
                sendOne(from,guessFirstName(to), to, subject, body, html, attachmentPaths);
                results.add(RecipientResult.ok(to));
            } catch (Exception ex) {
                results.add(RecipientResult.fail(to, ex.getMessage()));
            }
        }
        return results;
    }
    public static String guessFirstName(String email) {
        if (email == null) return null;

        // grab the local part before @
        Matcher m = Pattern.compile("^\\s*([^@\\s]+)@").matcher(email);
        if (!m.find()) return null;
        String local = m.group(1);

        // drop plus tags (john.doe+tag -> john.doe)
        int plus = local.indexOf('+');
        if (plus >= 0) local = local.substring(0, plus);

        // split on common separators and take the first usable token
        String[] parts = local.split("[._-]+");
        if (parts.length == 0) return null;

        String first = null;
        for (String part : parts) {
            String cleaned = part.replaceAll("\\d+", ""); // remove digits
            if (cleaned.length() > 1) { // skip single-letter like "m"
                first = cleaned;
                break;
            }
        }

        // fallback: if all parts were 1-char or empty, take the first non-empty anyway
        if (first == null) {
            for (String part : parts) {
                String cleaned = part.replaceAll("\\d+", "");
                if (!cleaned.isEmpty()) {
                    first = cleaned;
                    break;
                }
            }
        }

        if (first == null || first.isEmpty()) return null;

        // Capitalize nicely
        return first.substring(0,1).toUpperCase() + first.substring(1).toLowerCase();
    }
}

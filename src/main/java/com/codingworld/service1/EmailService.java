package com.codingworld.service1;

import com.codingworld.dto.RecipientResult;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
	private ResourceLoader resourceLoader;
    public EmailService(JavaMailSender mailSender, ResourceLoader resourceLoader) {
        this.mailSender = mailSender;
		this.resourceLoader = resourceLoader;
    }

	private Resource resolveAttachment(String nameOrPath) {
		if (!StringUtils.hasText(nameOrPath)) return null;
		String trimmed = nameOrPath.trim();

		// If caller already uses classpath:... respect it verbatim
		if (trimmed.startsWith("classpath:")) {
			Resource r = resourceLoader.getResource(trimmed);
			return (r.exists() ? r : null);
		}

		// 1) Try classpath root (src/main/resources/**)
		Resource r = resourceLoader.getResource("classpath:/" + trimmed.replaceFirst("^/*", ""));
		if (r.exists()) return r;

		// 2) Optional: fall back to filesystem
		File f = new File(trimmed);
		if (f.exists() && f.isFile()) return new FileSystemResource(f);

		return null; // not found
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
			for (String a : attachmentPaths) {
				Resource res = resolveAttachment(a);
				if (res != null) {
					String attachName = (res.getFilename() != null ? res.getFilename() : a);
					helper.addAttachment(attachName, res); // Resource implements InputStreamSource
				} else {
					// log.warn("Attachment not found: {}", a);
				}
			}
		}

		mailSender.send(mime);
    }

			    public List<RecipientResult> sendIndividually(String from, List<String> recipients, String subject, String body,
			            boolean html, List<String> attachmentPaths) {
			List<RecipientResult> results = new ArrayList<>();
			if (recipients == null) return results;
			
			// 1) Where we keep the public-facing text file (dev-time location)
			Path logPath = Paths.get("src/main/resources/sent-emails.txt");
			
			// Ensure parent dir exists
			try {
			Files.createDirectories(logPath.getParent());
			} catch (IOException e) {
			e.printStackTrace(); // or use your logger
			}
			
			// 2) Load existing emails (case-insensitive de-dupe)
			Set<String> existingLower = new HashSet<>();
			if (Files.exists(logPath)) {
			    try (Stream<String> lines = Files.lines(logPath, StandardCharsets.UTF_8)) {
			        for (String s : (Iterable<String>) lines::iterator) {
			            if (s != null && !s.trim().isEmpty()) {
			                existingLower.add(s.trim().toLowerCase(Locale.ROOT));
			            }
			        }
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
			}
			
			// 3) During this run, collect *new* unique emails to append (preserve original case for file)
			//    Use LinkedHashMap to keep order and allow case-insensitive de-dupe
			Map<String, String> toAppend = new LinkedHashMap<>();

					for (String r : recipients) {
						if (r == null) continue;
						String raw = r.trim();
						if (raw.isEmpty()) continue;

						String to;
						String firstName;

						// Split on whitespace
						String[] parts = raw.split("\\s+");
						String lastPart = parts[parts.length - 1];

						if (lastPart.contains("@")) {
							// ✅ last token looks like an email
							to = lastPart.trim();

							if (parts.length > 1) {
								// Everything except last token is the "full name"
								firstName = String.join(" ", Arrays.copyOf(parts, parts.length - 1)).trim();
							} else {
								// No name provided, fallback
								firstName = guessFirstName(to);
							}
						} else {
							// ✅ input is only an email
							to = raw;
							firstName = guessFirstName(to);
						}

						String key = to.toLowerCase(Locale.ROOT);

						// 🔑 Skip if already sent
						if (existingLower.contains(key)) {
							results.add(RecipientResult.skip(to, "Already sent earlier"));
							continue;
						}

						try {
							System.out.println("FullName: " + firstName + ", Email: " + to);

							sendOne(from, firstName, to, subject, body, html, attachmentPaths);
							results.add(RecipientResult.ok(to));

							// only add if not already queued this round
							if (!toAppend.containsKey(key)) {
								toAppend.put(key, to); // store only email (without name)
							}
						} catch (Exception ex) {
							results.add(RecipientResult.fail(to, ex.getMessage()));
						}
					}



					if (!toAppend.isEmpty()) {
			try (BufferedWriter w = Files.newBufferedWriter(
			logPath,
			StandardCharsets.UTF_8,
			StandardOpenOption.CREATE,
			StandardOpenOption.APPEND)) {
			
			for (String originalCaseEmail : toAppend.values()) {
			w.write(originalCaseEmail);
			w.newLine();
			}
			} catch (IOException e) {
			e.printStackTrace();
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
            if (cleaned.length() > 2) { // skip double-letters like "mn"
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

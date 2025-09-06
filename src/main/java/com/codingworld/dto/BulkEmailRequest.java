package com.codingworld.dto;

import java.util.List;

public class BulkEmailRequest {
    // required
    public List<String> to;
    public String subject;
    public String body;

    // optional
    public boolean html = false;
    public List<String> attachments;
}

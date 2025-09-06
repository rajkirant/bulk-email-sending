package com.codingworld.dto;

import java.util.List;

public class BulkEmailResponse {
    public int requested;
    public int sent;
    public int failed;
    public List<RecipientResult> results;

    public BulkEmailResponse() {}

    public BulkEmailResponse(int requested, int sent, int failed, List<RecipientResult> results) {
        this.requested = requested;
        this.sent = sent;
        this.failed = failed;
        this.results = results;
    }
}

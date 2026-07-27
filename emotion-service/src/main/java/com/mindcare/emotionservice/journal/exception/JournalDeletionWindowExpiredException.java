package com.mindcare.emotionservice.journal.exception;

import com.mindcare.emotionservice.shared.exception.BusinessException;

public class JournalDeletionWindowExpiredException extends BusinessException {

    public JournalDeletionWindowExpiredException() {
        super(
                "JOURNAL_DELETION_WINDOW_EXPIRED",
                "Emotion journal can only be deleted within 15 minutes after creation"
        );
    }
}

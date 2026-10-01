package ru.ai.sin.logic.chat;

/**
 * Коды системных сообщений в чате (хранятся в {@code chat_messages.system_event}).
 */
public final class ChatSystemEvent {

    public static final String REQUEST_SENT = "REQUEST_SENT";
    public static final String STUDENT_ACCEPTED = "STUDENT_ACCEPTED";
    public static final String STUDENT_REJECTED = "STUDENT_REJECTED";
    public static final String ADMIN_JOINED = "ADMIN_JOINED";
    public static final String VACANCY_APPLICATION_ACCEPTED = "VACANCY_APPLICATION_ACCEPTED";
    public static final String TU_CONFIRMED = "TU_CONFIRMED";
    public static final String TU_REJECTED = "TU_REJECTED";
    public static final String TU_STUDENT_CONFIRMED = "TU_STUDENT_CONFIRMED";
    public static final String TU_RECRUITER_CONFIRMED = "TU_RECRUITER_CONFIRMED";

    private ChatSystemEvent() {}
}

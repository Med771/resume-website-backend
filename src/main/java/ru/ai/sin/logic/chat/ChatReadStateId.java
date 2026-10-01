package ru.ai.sin.logic.chat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatReadStateId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID chatId;
    private UUID userId;
}

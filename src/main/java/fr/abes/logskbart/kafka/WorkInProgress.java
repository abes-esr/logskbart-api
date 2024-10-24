package fr.abes.logskbart.kafka;

import fr.abes.logskbart.dto.LogKbartDto;
import fr.abes.logskbart.entity.LogKbart;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Slf4j
public class WorkInProgress {
    private List<LogKbart> messages;

    private Timestamp timestamp;

    public WorkInProgress() {
        this.messages = Collections.synchronizedList(new ArrayList<>());
    }

    public void addMessage(LogKbart message) {
        this.messages.add(message);
    }

}

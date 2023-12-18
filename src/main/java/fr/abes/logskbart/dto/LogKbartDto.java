package fr.abes.logskbart.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LogKbartDto {
    private InstantDto instant;
    private String thread;
    private String level;
    private String loggerName;
    private String message;
    private boolean endOfBatch;
    private String loggerFqcn;
    private Integer threadId;
    private Integer threadPriority;

    @Getter @Setter
    private static class InstantDto {
        private Long epochSecond;
        private Long nanoOfSecond;
    }

    @Override
    public String toString() {
        return "Level : " + this.level + " / message : " + this.message;
    }
}

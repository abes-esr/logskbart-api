package fr.abes.logskbart.entity;

import fr.abes.logskbart.utils.Level;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "LOGKBART")
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class LogKbart implements Serializable {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PACKAGE_NAME")
    private String packageName;

    @Column(name = "TIMESTAMP")
    private Date timestamp;

    @Column(name = "THREAD")
    private String thread;

    @Column(name = "LEVEL")
    @Enumerated(EnumType.STRING)
    private Level level;

    @Column(name = "LOGGER_NAME")
    private String loggerName;

    @Column(name = "message", length = 2048)
    private String message;

    @Column(name = "END_OF_BATCH")
    private boolean endOfBatch;

    @Column(name = "LOGGER_FQCN")
    private String loggerFqcn;

    @Column(name = "THREAD_ID")
    private Integer threadId;

    @Column(name = "THREAD_PRIORITY")
    private Integer threadPriority;

    @Column(name = "NB_LINE", nullable = false)
    private Integer nbLine;

    @Column(name = "NB_RUN", nullable = false)
    private Integer nbRun = 0;

    @Override
    public String toString() {
        return "LogKbart{" +
                "packageName='" + packageName + '\'' +
                ", timestamp=" + timestamp +
                ", thread='" + thread + '\'' +
                ", level=" + level +
                ", message='" + message + '\'' +
                ", loggerFqcn='" + loggerFqcn + '\'' +
                ", nbLine='" + nbLine + '\'' +
                ", nbRun='" + nbRun + '\'' +

                '}';
    }

    public void log(){
        log.debug( this.level +" : " + this);
    }
}

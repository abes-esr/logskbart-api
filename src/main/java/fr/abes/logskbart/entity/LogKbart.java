package fr.abes.logskbart.entity;

import fr.abes.logskbart.utils.Level;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.io.Serializable;
import java.util.Date;

@Document(indexName = "logkbart")
@Data
@Slf4j
public class LogKbart implements Serializable {
    @Id
    @Field(name = "ID")
    private String id;

    @Field(name = "PACKAGE_NAME")
    private String packageName;

    @Field(name = "TIMESTAMP")
    private Date timestamp;

    @Field(name = "THREAD")
    private String thread;

    @Field(name = "LEVEL")
    private String level;

    @Field(name = "LOGGER_NAME")
    private String loggerName;

    @Field(name = "MESSAGE")
    private String message;

    @Field(name = "END_OF_BATCH")
    private boolean endOfBatch;

    @Field(name = "LOGGER_FQCN")
    private String loggerFqcn;

    @Field(name = "THREAD_ID")
    private Integer threadId;

    @Field(name = "THREAD_PRIORITY")
    private Integer threadPriority;

    @Field(name = "NB_LINE")
    private Integer nbLine;

    @Field(name = "NB_RUN")
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

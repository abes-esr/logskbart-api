package fr.abes.logskbart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailDto {
    private String app;
    private String[] to;
    private String[] cc;
    private String[] cci;
    private String subject;
    private String text;
}

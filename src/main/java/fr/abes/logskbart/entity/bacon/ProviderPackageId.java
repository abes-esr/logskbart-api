package fr.abes.logskbart.entity.bacon;

import jakarta.persistence.Column;

import java.io.Serializable;
import java.util.Date;


public class ProviderPackageId implements Serializable {
    @Column(name = "PACKAGE")
    private String packageName;
    @Column(name = "DATE_P")
    private Date dateP;
    @Column(name = "PROVIDER_IDT_PROVIDER")
    private Integer providerIdtProvider;
}

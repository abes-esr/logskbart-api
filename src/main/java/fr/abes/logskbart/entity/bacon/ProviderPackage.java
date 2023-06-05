package fr.abes.logskbart.entity.bacon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "PROVIDER_PACKAGE")
@IdClass(ProviderPackageId.class)
@Getter @Setter
public class ProviderPackage implements Serializable {
    @Id
    @Column(name = "PACKAGE")
    private String packageName;
    @Id
    @Column(name = "DATE_P")
    private Date dateP;
    @Id
    @Column(name = "PROVIDER_IDT_PROVIDER")
    private Integer providerIdtProvider;

}

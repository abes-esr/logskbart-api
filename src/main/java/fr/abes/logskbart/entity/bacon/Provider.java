package fr.abes.logskbart.entity.bacon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "PROVIDER")
@Getter
@Setter
public class Provider implements Serializable {
    @Id
    @Column(name = "IDT_PROVIDER")
    private Integer idtProvider;
    @Column(name = "PROVIDER")
    private String provider;
    @Column(name = "NAME")
    private String name;
    @Column(name = "NOM_CONTACT")
    private String nomContact;
    @Column(name = "PRENOM_CONTACT")
    private String prenomContact;
    @Column(name = "MAIL_CONTACT")
    private String mailContact;
    @Column(name = "DISPLAY_NAME")
    private String displayName;
}

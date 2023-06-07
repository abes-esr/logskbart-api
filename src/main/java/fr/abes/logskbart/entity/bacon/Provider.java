package fr.abes.logskbart.entity.bacon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Entity
@Table(name = "PROVIDER")
@Getter
@Setter
@NoArgsConstructor
public class Provider implements Serializable {
    @Id
    @Column(name = "IDT_PROVIDER")
    private Integer idtProvider;
    @Column(name = "PROVIDER")
    private String provider;
    @Column(name = "NOM_CONTACT")
    private String nomContact;
    @Column(name = "PRENOM_CONTACT")
    private String prenomContact;
    @Column(name = "MAIL_CONTACT")
    private String mailContact;
    @Column(name = "DISPLAY_NAME")
    private String displayName;


    public Provider(String provider) {
        this.provider = provider;
        //on ne connait pas le display name à l'avance, on l'initialise au provider pour éviter une erreur not null dans la table
        this.displayName = provider;
    }


}

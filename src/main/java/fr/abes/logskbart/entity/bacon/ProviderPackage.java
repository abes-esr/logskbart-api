package fr.abes.logskbart.entity.bacon;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "PROVIDER_PACKAGE")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProviderPackage implements Serializable {
    @EmbeddedId
    private ProviderPackageId providerPackageId;
}

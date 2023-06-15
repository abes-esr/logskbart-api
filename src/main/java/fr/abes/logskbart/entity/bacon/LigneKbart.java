package fr.abes.logskbart.entity.bacon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "LIGNE_KBART_CONVERGENCE")
@Setter @Getter
public class LigneKbart implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDT_LIGNE_KBART")
    private Integer id;
    @Column(name = "PUBLICATION_TITLE")
    private String publicationTitle;
    @Column(name = "PRINT_IDENTIFIER")
    private String printIdentifier;
    @Column(name = "ONLINE_IDENTIFIER")
    private String onlineIdentifer;
    @Column(name = "DATE_FIRST_ISSUE_ONLINE")
    private Date dateFirstIssueOnline;
    @Column(name = "NUM_FIRST_VOL_ONLINE")
    private String numFirstVolOnline;
    @Column(name = "NUM_FIRST_ISSUE_ONLINE")
    private String numFirstIssueOnline;
    @Column(name = "DATE_LAST_ISSUE_ONLINE")
    private Date dateLastIssueOnline;
    @Column(name = "NUM_LAST_VOL_ONLINE")
    private String numLastVolOnline;
    @Column(name = "NUM_LAST_ISSUE_ONLINE")
    private String numlastIssueOnline;
    @Column(name = "TITLE_URL")
    private String titleUrl;
    @Column(name = "FIRST_AUTHOR")
    private String firstAuthor;
    @Column(name = "TITLE_ID")
    private String titleId;
    @Column(name = "EMBARGO_INFO")
    private String embargoInfo;
    @Column(name = "COVERAGE_DEPTH")
    private String coverageDepth;
    @Column(name = "NOTES")
    private String notes;
    @Column(name = "PUBLISHER_NAME")
    private String publisherName;
    @Column(name = "PUBLICATION_TYPE")
    private String publicationType;
    @Column(name = "DATE_MONOGRAPH_PUBLISHED_PRINT")
    private Date dateMonographPublishedPrint;
    @Column(name = "DATE_MONOGRAPH_PUBLISHED_ONLIN")
    private Date dateMonographPublishedOnline;
    @Column(name = "MONOGRAPH_VOLUME")
    private String monographVolume;
    @Column(name = "MONOGRAPH_EDITION")
    private String monographEdition;
    @Column(name = "FIRST_EDITOR")
    private String firstEditor;
    @Column(name = "PARENT_PUBLICATION_TITLE_ID")
    private String parentPublicationTitleId;
    @Column(name = "PRECEDING_PUBLICATION_TITLE_ID")
    private String precedeingPublicationTitleId;
    @Column(name = "ACCESS_TYPE")
    private String accessType;
    @ManyToOne(targetEntity = ProviderPackage.class, cascade = CascadeType.REMOVE, optional = false)
    @JoinColumns({
        @JoinColumn(name = "PROVIDER_PACKAGE_PACKAGE", referencedColumnName = "PACKAGE"),
        @JoinColumn(name = "PROVIDER_PACKAGE_DATE_P", referencedColumnName = "DATE_P"),
        @JoinColumn(name = "PROVIDER_PACKAGE_IDT_PROVIDER", referencedColumnName = "PROVIDER_IDT_PROVIDER"
    )})
    private ProviderPackage providerPackage;
    @Column(name = "BEST_PPN")
    private String bestPpn;
}

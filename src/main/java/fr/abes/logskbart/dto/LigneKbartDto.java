package fr.abes.logskbart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LigneKbartDto {
    @CsvBindByName(column = "publication_title")
    @JsonProperty("publication_title")
    @CsvBindByPosition(position = 0)
    private String publicationTitle;
    @CsvBindByName(column = "print_identifier")
    @JsonProperty("print_identifier")
    @CsvBindByPosition(position = 1)
    private String printIdentifier;
    @CsvBindByName(column = "online_identifier")
    @JsonProperty("online_identifier")
    @CsvBindByPosition(position = 2)
    private String onlineIdentifier;
    @CsvBindByName(column = "date_first_issue_online")
    @JsonProperty("date_first_issue_online")
    @CsvBindByPosition(position = 3)
    private String dateFirstIssueOnline;
    @CsvBindByName(column = "num_first_vol_online")
    @JsonProperty("num_first_vol_online")
    @CsvBindByPosition(position = 4)
    private Integer numFirstVolOnline;
    @CsvBindByName(column = "num_first_issue_online")
    @JsonProperty("num_first_issue_online")
    @CsvBindByPosition(position = 5)
    private Integer numFirstIssueOnline;
    @CsvBindByName(column = "date_last_issue_online")
    @JsonProperty("date_last_issue_online")
    @CsvBindByPosition(position = 6)
    private String dateLastIssueOnline;
    @CsvBindByName(column = "num_last_vol_online")
    @JsonProperty("num_last_vol_online")
    @CsvBindByPosition(position = 7)
    private Integer numLastVolOnline;
    @CsvBindByName(column = "num_last_issue_online")
    @JsonProperty("num_last_issue_online")
    @CsvBindByPosition(position = 8)
    private Integer numLastIssueOnline;
    @CsvBindByName(column = "title_url")
    @JsonProperty("title_url")
    @CsvBindByPosition(position = 9)
    private String titleUrl;
    @CsvBindByName(column = "first_author")
    @JsonProperty("first_author")
    @CsvBindByPosition(position = 10)
    private String firstAuthor;
    @CsvBindByName(column = "title_id")
    @JsonProperty("title_id")
    @CsvBindByPosition(position = 11)
    private String titleId;
    @CsvBindByName(column = "embargo_info")
    @JsonProperty("embargo_info")
    @CsvBindByPosition(position = 12)
    private String embargoInfo;
    @CsvBindByName(column = "coverage_depth")
    @JsonProperty("coverage_depth")
    @CsvBindByPosition(position = 13)
    private String coverageDepth;
    @CsvBindByName(column = "notes")
    @JsonProperty("notes")
    @CsvBindByPosition(position = 14)
    private String notes;
    @CsvBindByName(column = "publisher_name")
    @JsonProperty("publisher_name")
    @CsvBindByPosition(position = 15)
    private String publisherName;
    @CsvBindByName(column = "publication_type")
    @JsonProperty("publication_type")
    @CsvBindByPosition(position = 16)
    private String publicationType;
    @CsvBindByName(column = "date_monograph_published_print")
    @JsonProperty("date_monograph_published_print")
    @CsvBindByPosition(position = 17)
    private String dateMonographPublishedPrint;
    @CsvBindByName(column = "date_monograph_published_online")
    @JsonProperty("date_monograph_published_online")
    @CsvBindByPosition(position = 18)
    private String dateMonographPublishedOnline;
    @CsvBindByName(column = "monograph_volume")
    @JsonProperty("monograph_volume")
    @CsvBindByPosition(position = 19)
    private Integer monographVolume;
    @CsvBindByName(column = "monograph_edition")
    @JsonProperty("monograph_edition")
    @CsvBindByPosition(position = 20)
    private String monographEdition;
    @CsvBindByName(column = "first_editor")
    @JsonProperty("first_editor")
    @CsvBindByPosition(position = 21)
    private String firstEditor;
    @CsvBindByName(column = "parent_publication_title_id")
    @JsonProperty("parent_publication_title_id")
    @CsvBindByPosition(position = 22)
    private String parentPublicationTitleId;
    @CsvBindByName(column = "preceding_publication_title_id")
    @JsonProperty("preceding_publication_title_id")
    @CsvBindByPosition(position = 23)
    private String precedingPublicationTitleId;
    @CsvBindByName(column = "access_type")
    @JsonProperty("access_type")
    @CsvBindByPosition(position = 24)
    private String accessType;
    @CsvBindByName(column = "bestPpn")
    @JsonProperty("bestPpn")
    @CsvBindByPosition(position = 25)
    private String bestPpn;

    @Override
    public int hashCode() {
        return this.publicationTitle.hashCode() * this.onlineIdentifier.hashCode() * this.printIdentifier.hashCode();
    }

    @Override
    public String toString() {
        return "publication title : " + this.publicationTitle + " / publication_type : " + this.publicationType +
                (this.onlineIdentifier.isEmpty() ? "" : " / online_identifier : " + this.onlineIdentifier) +
                (this.printIdentifier.isEmpty() ? "" : " / print_identifier : " + this.printIdentifier);
    }

}

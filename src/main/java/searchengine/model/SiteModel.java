package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Entity
@Getter
@Setter
@Table(name = "site")
public class SiteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "status_time")
    private Date statusTime;
    @Column(name = "last_error")
    private String lastError;

    private String url;

    private String name;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "siteId", cascade = CascadeType.ALL)
    private List<PageModel> pageModelList = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "siteModelId", cascade = CascadeType.ALL)
    private List<LemmaModel> lemmaModelList = new ArrayList<>();

    public SiteModel(Status status,
                     Date statusTime,
                     String lastError,
                     String url,
                     String name,
                     List<PageModel> pageModelList,
                     List<LemmaModel> lemmaModelList) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
        this.pageModelList = pageModelList;
        this.lemmaModelList = lemmaModelList;
    }

    public SiteModel() {
    }

}

package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.response.Response;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.index.Parser;
import searchengine.services.lemma.LemmaIndex;
import searchengine.services.site.SiteIndexing;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import static searchengine.model.Status.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList config;
    private ExecutorService executorService;
    private final LemmaIndex lemmaIndex;

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Parser parser;

    public Response startIndexing() {
        if (isIndexingOn()) {
            log.debug("Indexing is already running");
            new Response(false, "Индексация уже запущена").getError();
        } else {
            List<Site> siteList = config.getSites();
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (Site site : siteList) {
                String url = site.getUrl();
                SiteModel siteModel = new SiteModel();
                siteModel.setName(site.getName());
                log.info("Indexing web site ".concat(site.getName()));
                executorService.submit(new SiteIndexing(pageRepository,
                        siteRepository,
                        url,
                        config, indexRepository,
                        lemmaRepository,
                        lemmaIndex, parser));
            }
            executorService.shutdown();

        }
        return new Response(true);
    }


    private boolean isIndexingOn() {
        Iterable<SiteModel> siteList = siteRepository.findAll();
        for (SiteModel site : siteList) {
            if (site.getStatus() == INDEXING) {
                return true;
            }
        }
        return false;
    }

    public Response stopIndexing() {
        if (!isIndexingOn()) {
            log.info("Site indexing is already running!");
            return new Response(false, "Индексация не запущена");
        } else {
            log.info("Index stopping.");
            executorService.shutdownNow();
            Iterable<SiteModel> siteModels = siteRepository.findAll();
            for (SiteModel siteModel : siteModels) {
                if (siteModel.getStatus() == INDEXING) {
                    siteModel.setStatus(FAILED);
                    siteModel.setLastError("Индексация остановлена пользователем");
                }
                siteRepository.save(siteModel);
            }
            return new Response(true);


        }

    }


    public boolean indexPage(String page) {
        if (urlEquals(page)) {
            log.info("Переиндексация страницы : " + page);
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            executorService.submit(new SiteIndexing(pageRepository, siteRepository, page, config,
                    indexRepository, lemmaRepository, lemmaIndex, parser));
            return true;
        } else {
            return false;
        }
    }

    private boolean urlEquals(String url) {
        List<Site> urlList = config.getSites();
        return urlList.stream().anyMatch(site -> site.getUrl().equals(url));
    }


}


package searchengine.services.site;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexDTO;
import searchengine.dto.statistics.LemmaDTO;
import searchengine.dto.statistics.PageDTO;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.index.Parser;
import searchengine.services.lemma.LemmaIndex;
import searchengine.services.page.PageIndexing;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexing  implements  Runnable {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final String url;
    private final SitesList sitesList;
    private final IndexRepository indexRepository;
    private  final LemmaRepository lemmaRepository;

    private final LemmaIndex lemmaIndex;

    private final Parser parser;


    public void run() {
        if (siteRepository.findByUrl(url) != null) {
            log.info("start site date delete from ".concat(url));
            SiteModel siteModel = siteRepository.findByUrl(url);
            siteModel.setStatus(Status.INDEXED);
            siteModel.setName(getSiteName());
            siteModel.setStatusTime(new Date());
            siteRepository.save(siteModel);
            siteRepository.delete(siteModel);
        }
        log.info("Site indexing start ".concat(url).concat(" ").concat(getSiteName()));
        SiteModel siteModel = new SiteModel();
        siteModel.setUrl(url);
        siteModel.setName(getSiteName());
        siteModel.setStatus(Status.INDEXING);
        siteModel.setStatusTime(new Date());
        siteRepository.save(siteModel);
        try {
            pageIndex(siteModel);
            lemmaIndexing(siteModel);
            parser(siteModel);
        }
        catch(InterruptedException e){
            log.error("WebParser stopped from ".concat(url).concat(". ").concat(e.getMessage()));
            SiteModel sites = new SiteModel();
            sites.setLastError("WebParser stopped");
            sites.setStatus(Status.FAILED);
            sites.setStatusTime(new Date());
            siteRepository.save(siteModel);
        }

    }

    public void pageIndex(SiteModel siteModel) {
            if (!Thread.interrupted()) {
                List<PageDTO> pageDtoList;
                if (!Thread.interrupted()) {
                    String urls = url.concat("/");
                    List<PageDTO> pageList = new CopyOnWriteArrayList<>();
                    List<String> urlList = new CopyOnWriteArrayList<>();
                    ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
                    List<PageDTO> pages = forkJoinPool.invoke(new PageIndexing(urls, pageList, urlList, sitesList));
                    pageDtoList = new CopyOnWriteArrayList<>(pages);
                    List<PageModel> pageList1 = new CopyOnWriteArrayList<>();
                    for (PageDTO page : pageDtoList) {
                        int start = page.getUrl().indexOf(url) + url.length();
                        String pageFormat = page.getUrl().substring(start);
                        pageList1.add(new PageModel(siteModel, pageFormat, page.getCode(),
                                page.getContent()));
                    }
                    pageRepository.saveAll(pageList1);

                } else {
                    throw new RuntimeException();
                }
            }
        }

    public void lemmaIndexing(SiteModel siteModel) {
        if (!Thread.interrupted()) {
            SiteModel model = siteRepository.findByUrl(url);
            model.setStatusTime(new Date());
            lemmaIndex.startLemmaIndex();
            List<LemmaDTO> lemmaDTOList = lemmaIndex.getLemmaDTOList();
            List<LemmaModel> lemmaModelList = new CopyOnWriteArrayList<>();


            for (LemmaDTO lemmaDTO : lemmaDTOList) {
                lemmaModelList.add(new LemmaModel(lemmaDTO.getLemma(), lemmaDTO.getFrequency(), siteModel));
            }
            lemmaRepository.saveAll(lemmaModelList);
        } else {
            throw new RuntimeException("Invalid lemma written");
        }
    }

        public void parser(SiteModel siteModel) throws InterruptedException {
            if (!Thread.interrupted()) {
                parser.startParsing(siteModel);
                List<IndexDTO> indexDtoList = new CopyOnWriteArrayList<>(parser.getConfig());
                List<IndexModel> indexModels = new CopyOnWriteArrayList<>();
                siteModel.setStatusTime(new Date());
                for (IndexDTO indexDto : indexDtoList) {
                    PageModel page = pageRepository.getById(indexDto.getPageID());
                    LemmaModel lemma = lemmaRepository.getById(indexDto.getLemmaID());
                    indexModels.add(new IndexModel(page, lemma, indexDto.getRank()));
                }
                indexRepository.saveAll(indexModels);
                log.info("Parser stopping ".concat(url));
                siteModel.setStatusTime(new Date());
                siteModel.setStatus(Status.INDEXED);
                siteRepository.save(siteModel);

            } else {
                throw new InterruptedException();
            }
        }

    private String getSiteName () {
        List<Site> sites = sitesList.getSites();
        for (Site site : sites) {
            if (site.getUrl().equals(url)) {
                return site.getName();
            }
        }
        return "";
    }

}

       
        
    

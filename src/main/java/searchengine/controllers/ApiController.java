package searchengine.controllers;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.SearchDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.response.Response;
import searchengine.repository.SiteRepository;
import searchengine.search.Search;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {


    private final IndexingService indexingService;
    private final StatisticsServiceImpl statisticsService;

    private final SiteRepository siteRepository;
    private final Search search;

    public ApiController(StatisticsServiceImpl statisticsService, IndexingService indexingService, SiteRepository siteRepository,
                         Search search) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.search = search;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatisticsResponse());

    }


    @GetMapping("/startIndexing")
    public Response startIndexing() {
        return indexingService.startIndexing();
    }


    @GetMapping("/stopIndexing")
    public Response stopIndexing() {
        log.info("ОСТАНОВКА ИНДЕКСАЦИИ");
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestParam(name = "url", required = false,
            defaultValue = "") String url) {
        if (url.isEmpty()) {
            log.info("Страница не указана");
            return new ResponseEntity<>(new Response(false, "Страница не указана"), HttpStatus.BAD_REQUEST);
        } else {
            if (indexingService.indexPage(url)) {
                log.info("Страница " + url + " добавлнена на переиндексацию");
                return new ResponseEntity<>(new Response(true), HttpStatus.OK);
            } else {
                log.info("Страница " + url + " не может быть проиндексированан");
                return new ResponseEntity<>(new Response(false, "Страница не может быть проиндексированан ")
                        , HttpStatus.BAD_REQUEST);
            }
        }

    }

    @GetMapping("/search")
    public ResponseEntity<Response> search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                                           @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                           @RequestParam(name = "offset", required = false, defaultValue = "0") int offset) {

        List<SearchDTO> searchData;
        if (!site.isEmpty()) {
            if (siteRepository.findByUrl(site) == null) {

                return new ResponseEntity<>(new Response(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле"), HttpStatus.BAD_REQUEST);
            } else {
                searchData = search.getSearchFromSite(query, site, offset, 40);
            }
        } else {
            searchData = search.getFullSearch(query, offset, 40);
        }
        return new ResponseEntity<>(new Response(true, searchData.size(), searchData), HttpStatus.OK);
    }


}

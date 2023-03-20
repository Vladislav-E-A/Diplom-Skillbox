package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;


import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    private TotalStatistics getTotalStatistic() {
        long sites = siteRepository.count();
        long pages  = pageRepository.count();
        long lemmas = lemmaRepository.count();
        return  new TotalStatistics(sites,pages,lemmas,true);
    }

    private DetailedStatisticsItem getDetailedFromDetailedStatistic(SiteModel siteModel){
        String url = siteModel.getUrl();
        String name = siteModel.getName();
        String status = siteModel.getStatus().toString();
        Date statusTime = siteModel.getStatusTime();
        String error = siteModel.getLastError();
        long pages = pageRepository.countBySiteId(siteModel);
        long lemmas = lemmaRepository.countBySiteModelId(siteModel);
        return new DetailedStatisticsItem(url,name,status,statusTime,error,pages,lemmas);
    }


    private List<DetailedStatisticsItem> getDetailedStatisticsItems(){
        List<SiteModel> siteModelList = (List<SiteModel>) siteRepository.findAll();
        return siteModelList.stream().map(this::getDetailedFromDetailedStatistic).collect(Collectors.toList());
    }

    public StatisticsResponse getStatisticsResponse(){
        TotalStatistics totalStatistics = getTotalStatistic();
        List<DetailedStatisticsItem> list = getDetailedStatisticsItems();
        return  new StatisticsResponse(true,new StatisticsData(totalStatistics,list));
    }


}

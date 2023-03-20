package searchengine.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchDTO;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;
import searchengine.repository.SiteRepository;
import searchengine.services.seacrhService.SearchService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Search {
    private final SiteRepository siteRepository;
    private final SearchService searchService;

    public List<SearchDTO> getSearchFromSite(String text, String url, int start, int limit) {
        SiteModel siteModel = siteRepository.findByUrl(url);
        List<String> textLemmaList = searchService.getLemmaFromSearchText(text);
        List<LemmaModel> foundList = searchService.getLemmaModelFromSite(textLemmaList, siteModel);
        return searchService.createSearchDtoList(foundList, textLemmaList, start, limit);
    }

    public List<SearchDTO> getFullSearch(String text,
                                         int start,
                                         int limit) {
        List<SiteModel> siteList = (List<SiteModel>) siteRepository.findAll();
        List<SearchDTO> result = new ArrayList<>();
        List<LemmaModel> foundLemmaList = new ArrayList<>();
        List<String> textLemmaList = searchService.getLemmaFromSearchText(text);
        {
            int i = 0;
            while (i < siteList.size()) {
                SiteModel site = siteList.get(i);
                foundLemmaList.addAll(searchService.getLemmaModelFromSite(textLemmaList, site));
                i++;
            }
        }
        List<SearchDTO> searchData = new ArrayList<>();
        for (LemmaModel l : foundLemmaList) {
            if (l.getLemma().equals(text)) {
                searchData = (searchService.createSearchDtoList(foundLemmaList, textLemmaList, start, limit));
                searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
                if (searchData.size() > limit) {
                    var i = start;
                    while (i < limit) {
                        result.add(searchData.get(i));
                        i++;
                    }
                    return result;
                }
            }
        }
        return searchData;
    }

}

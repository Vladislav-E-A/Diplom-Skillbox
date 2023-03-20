package searchengine.services.seacrhService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchDTO;
import searchengine.lemma.Lemma;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private final Lemma lemma;

    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;


    private List<SearchDTO> getSearchDtoList(ConcurrentHashMap<PageModel, Float> pageList,
                                             List<String> textLemmaList) {
        List<SearchDTO> searchDtoList = new ArrayList<>();
        Iterator<PageModel> iterator = pageList.keySet().iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            PageModel page = iterator.next();
            String uri = page.getPath();
            String content = page.getContent();
            SiteModel pageSite = page.getSiteId();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = clearCode(content, "title");
            String body = clearCode(content, "body");
            stringBuilder.append(title).append(body);
            float value = pageList.get(page);
            List<Integer> lemmaIndex = new ArrayList<>();
            StringBuilder snippetBuilder = new StringBuilder();
            {
                int i = 0;
                while (i < textLemmaList.size()) {
                    String lemm = textLemmaList.get(i);
                    try {
                        lemmaIndex.addAll(lemma.findLemmaIndexInText(stringBuilder.toString(), lemm));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    i++;
                }
            }
            Collections.sort(lemmaIndex);
            List<String> wordList = getWordsFromSite(stringBuilder.toString(), lemmaIndex);
            int i = 0;
            while (i < wordList.size()) {
                snippetBuilder.append(wordList.get(i)).append(".");
                if (i > 3) {
                    break;
                }
                i++;
            }

            searchDtoList.add(new SearchDTO(site, siteName, uri, title, snippetBuilder.toString(), value));
            System.out.println("добавление");
        }
        return searchDtoList;
    }

    private List<String> getWordsFromSite(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < lemmaIndex.size()) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int next = i + 1;
            while (next < lemmaIndex.size() && 0 < lemmaIndex.get(next) - end && lemmaIndex.get(next) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(next));
                next += 1;
            }
            i = next - 1;
            String word = content.substring(start, end);
            int startIndex;
            int nextIndex;
            if (content.lastIndexOf(" ", start) != -1) {
                startIndex = content.lastIndexOf(" ", start);
            } else startIndex = start;
            if (content.indexOf(" ", end + lemmaIndex.size() / 10) != -1) {
                nextIndex = content.indexOf(" ", end + lemmaIndex.size() / 10);
            } else nextIndex = content.indexOf(" ", end);
            String text = content.substring(startIndex, nextIndex).replaceAll(word, "<b>".concat(word).concat("</b>"));
            result.add(text);
            i++;
        }
        result.sort(Comparator.comparing(String::length).reversed());
        return result;
    }

    public List<SearchDTO> createSearchDtoList(List<LemmaModel> lemmaList,
                                               List<String> textLemmaList,
                                               int start, int limit) {
        List<SearchDTO> result = new ArrayList<>();
        if (lemmaList.size() >= textLemmaList.size()) {
            List<PageModel> pagesList = pageRepository.findByLemmaList(lemmaList);
            List<IndexModel> indexesList = indexRepository.findByPageAndLemmas(lemmaList, pagesList);
            Map<PageModel, Float> relevanceMap = getRelevanceFromPage(pagesList, indexesList);
            List<SearchDTO> searchDtos = getSearchDtoList((ConcurrentHashMap<PageModel, Float>) relevanceMap, textLemmaList);
            if (start > searchDtos.size()) {
                return new ArrayList<>();
            }
            if (searchDtos.size() > limit) {
                int i = start;
                while (i < limit) {
                    result.add(searchDtos.get(i));
                    i++;
                }
                return result;
            } else return searchDtos;

        } else return result;
    }

    private Map<PageModel, Float> getRelevanceFromPage(List<PageModel> pageList,
                                                       List<IndexModel> indexList) {
        Map<PageModel, Float> relevanceMap = new HashMap<>();
        {
            int i = 0;
            while (i < pageList.size()) {
                PageModel page = pageList.get(i);
                float relevance = 0;
                int j = 0;
                while (j < indexList.size()) {
                    IndexModel index = indexList.get(j);
                    if (index.getPage() == page) {
                        relevance += index.getRank();
                    }
                    j++;
                }
                relevanceMap.put(page, relevance);
                i++;
            }
        }
        Map<PageModel, Float> allRelevanceMap = new HashMap<>();

        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });

        List<Map.Entry<PageModel, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<PageModel, Float> map = new ConcurrentHashMap<>();
        int i = 0;
        while (i < sortList.size()) {
            Map.Entry<PageModel, Float> pageModelFloatEntry = sortList.get(i);
            map.putIfAbsent(pageModelFloatEntry.getKey(), pageModelFloatEntry.getValue());
            i++;
        }
        return map;
    }


    public List<String> getLemmaFromSearchText(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        int i = 0;
        while (i < words.length) {
            String lemmas = words[i];
            try {
                List<String> list = lemma.getLemma(lemmas);
                lemmaList.addAll(list);
            } catch (IOException e) {
                e.getMessage();
            }
            i++;
        }
        return lemmaList;
    }

    public List<LemmaModel> getLemmaModelFromSite(List<String> lemmas, SiteModel site) {
        List<LemmaModel> lemmaModels = lemmaRepository.findLemmaListBySite(lemmas, site);
        List<LemmaModel> result = new ArrayList<>(lemmaModels);
        result.sort(Comparator.comparingInt(LemmaModel::getFrequency));
        return result;
    }


    public String clearCode(String text, String element) {
        String stringBuilder;
        Document doc = Jsoup.parse(text);
        Elements elements = doc.select(element);
        stringBuilder = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(stringBuilder).text();
    }
}


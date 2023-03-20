package searchengine.services.index;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.IndexDTO;
import searchengine.lemma.Lemma;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Slf4j
@Getter
@Component
public class  Parser {

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final Lemma lemma;

    private List<IndexDTO> config = new ArrayList<>();

    public void startParsing(SiteModel siteModel) {
        List<LemmaModel> lemmaModelList = lemmaRepository.findBySiteModelId(siteModel);
        List<PageModel> pageModels = pageRepository.findBySiteId(siteModel);
        for (PageModel pageModel : pageModels) {
            if (pageModel.getCode() < 400) {
                long pageId = pageModel.getId();
                String content = pageModel.getContent();
                String title = clearHTML(content, "title");
                String body = clearHTML(content, "body");
                Map<String, Integer> titleList = lemma.getLemmaMap(title);
                Map<String, Integer> bodyList = lemma.getLemmaMap(body);


                for (LemmaModel lemmaModel : lemmaModelList) {
                    long lemmaId = lemmaModel.getId();
                    String word = lemmaModel.getLemma();
                    if (titleList.containsKey(word) || bodyList.containsKey(word)) {
                        float totalRank = 0.0f;
                        if (titleList.get(word) != null) {
                            float titleRank = titleList.get(word);
                            totalRank += titleRank;
                        }
                        if (bodyList.get(word) != null) {
                            float bodyRank = (float) (bodyList.get(word) * 0.8);
                            totalRank += bodyRank;
                        }
                        config.add(new IndexDTO(pageId, lemmaId, totalRank));
                    } else {
                        log.debug("lemma not found");
                    }
                }
            } else {
                log.debug("Bad status code " + pageModel.getCode());
            }
        }
    }

    public String clearHTML(String content, String s) {
        String sting;
        Document document = Jsoup.parse(content);
        Elements elements = document.select(s);
        sting = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(sting).text();
    }
}

package searchengine.services.lemma;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.LemmaDTO;
import searchengine.lemma.Lemma;
import searchengine.model.PageModel;
import searchengine.repository.PageRepository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Getter
@Slf4j

public class LemmaIndex {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private Lemma lemma;

    private List<LemmaDTO> lemmaDTOList;

    public void startLemmaIndex() {
        lemmaDTOList = new CopyOnWriteArrayList<>();
        Iterable<PageModel> pageModels = pageRepository.findAll();
        Map<String, Integer> lemmaList = new TreeMap<>();
        for (PageModel pageModel : pageModels) {
            String content = pageModel.getContent();
            String title = Html(content, "title");
            String body = Html(content, "body");
            Map<String, Integer> titleList = lemma.getLemmaMap(title);
            Map<String, Integer> bodyList = lemma.getLemmaMap(body);
            Set<String> words = new HashSet<>();
            words.addAll(titleList.keySet());
            words.addAll(bodyList.keySet());
            for (String word : words) {
                int frequency = lemmaList.getOrDefault(word, 0) + 1;
                lemmaList.put(word, frequency);
            }
        }

        for (String lemma : lemmaList.keySet()) {
            Integer frequency = lemmaList.get(lemma);
            lemmaDTOList.add(new LemmaDTO(lemma, frequency));
        }
    }


    public String Html(String content, String tag) {
        String html;
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select(tag);
        html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }
}

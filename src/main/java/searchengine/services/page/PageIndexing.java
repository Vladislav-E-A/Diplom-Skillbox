package searchengine.services.page;


import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.PageDTO;
import searchengine.model.SiteModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class PageIndexing extends RecursiveTask<List<PageDTO>> {
    private final String url;
    private final List<String> urlList;
    private final List<PageDTO> pageDtoList;
    private final SitesList config;

    public PageIndexing(String url,
                        List<PageDTO> pageDtoList,
                        List<String> urlList,
                        SitesList config) {
        this.url = url;
        this.pageDtoList = pageDtoList;
        this.urlList = urlList;
        this.config = config;
    }

    @Override
    protected List<PageDTO> compute() {
        try {
            Thread.sleep(100);
            Document doc = null;
            try {
                Thread.sleep(100);
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows; U;" +
                                " WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .get();
            } catch (IOException | InterruptedException e) {
                e.getMessage();
            }

            assert doc != null;
            String html = doc.outerHtml();
            Connection.Response response = doc.connection().response();
            int status = response.statusCode();
            PageDTO pageDto = new PageDTO(url, html, status);
            pageDtoList.add(pageDto);
            Elements elements = doc.select("body").select("a");
            List<PageIndexing> taskList = new CopyOnWriteArrayList<>();
            for (Element el : elements) {
                String link = el.absUrl("href");
                if (link.contains("/")) {
                    String[] fragment = el.baseUri().split("/");
                    String http = fragment[0].concat("//");
                    String trueUrl = http.concat(fragment[2]);
                    if (!link.contains("#")
                            && link.startsWith(trueUrl)
                            && ElementsType(link)
                            && !urlList.contains(link)
                            && !link.contains("?")) {
                        urlList.add(link);
                        System.out.println(link);
                        PageIndexing task = new PageIndexing(link, pageDtoList, urlList, config);
                        task.fork();
                        taskList.add(task);
                    }

                }
            }
            for (PageIndexing pageIndexing : taskList) {
                pageIndexing.join();
            }

        } catch (InterruptedException e) {
            log.debug("Error parsing from ".concat(url));
            PageDTO pageDto = new PageDTO(url, "", 500);
            pageDtoList.add(pageDto);
        }

        return pageDtoList;
    }

    private boolean ElementsType(String pathPage) {
        List<String> WRONG_TYPES = Arrays.asList("JPG", "gif", "gz", "jar", "jpeg", "jpg", "pdf", "png",
                "ppt", "pptx", "svg", "svg", "tar", "zip", "xlsx","doc");
        return !WRONG_TYPES.contains(pathPage.substring(pathPage.lastIndexOf(".") + 1));
    }


}


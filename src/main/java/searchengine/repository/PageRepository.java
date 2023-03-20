package searchengine.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<PageModel, Long> {
    long countBySiteId(SiteModel siteId);

    List<PageModel> findBySiteId(SiteModel path);

    @Query(value = "SELECT * FROM index_word JOIN Page  ON Page.id = index_word.page_id WHERE index_word.lemma_id IN :lemmas", nativeQuery = true)
    List<PageModel> findByLemmaList(@Param("lemmas") Collection<LemmaModel> lemmas);

    PageModel getById(long pageID);
}

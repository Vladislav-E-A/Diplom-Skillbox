package searchengine.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<IndexModel, Long> {

    @Query(value = "select * from index_word where index_word.lemma_id in :lemmas and index_word.page_id in :pages", nativeQuery = true)
    List<IndexModel> findByPageAndLemmas(@Param("lemmas") List<LemmaModel> lemmaList,
                                         @Param("pages") List<PageModel> pages);
}

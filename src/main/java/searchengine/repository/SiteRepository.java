package searchengine.repository;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

@Repository
public interface SiteRepository extends CrudRepository<SiteModel, Long> {
    SiteModel findByUrl(String url);
}

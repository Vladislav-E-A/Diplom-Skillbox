package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LemmaDTO {
    private String lemma;
    private int frequency;
}

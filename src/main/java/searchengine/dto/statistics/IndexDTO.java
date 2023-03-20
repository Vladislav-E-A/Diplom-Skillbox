package searchengine.dto.statistics;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndexDTO {
    private long pageID;
    private long lemmaID;
    private float rank;
}

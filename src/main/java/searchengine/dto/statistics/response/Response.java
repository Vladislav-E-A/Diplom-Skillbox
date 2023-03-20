package searchengine.dto.statistics.response;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.statistics.SearchDTO;

import java.util.List;

@Getter
@Setter
public class Response {
    private boolean result;
    private String error;
    private int count;

    private List<SearchDTO> data;


    public Response(boolean result) {
        this.result = result;
    }

    public Response(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public Response(boolean result, int count) {
        this.result = result;
        this.count = count;


    }

    public Response(boolean result, int count, List<SearchDTO> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }


}

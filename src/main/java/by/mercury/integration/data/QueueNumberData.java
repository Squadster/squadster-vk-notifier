package by.mercury.integration.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueNumberData {
    
    @JsonProperty(value = "queue_number")
    private Integer queueNumber;
}

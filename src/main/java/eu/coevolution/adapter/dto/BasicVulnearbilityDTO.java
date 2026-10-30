package eu.coevolution.adapter.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BasicVulnearbilityDTO {

    private String id;

    public BasicVulnearbilityDTO(String id) {
        this.id = id;
    }
}

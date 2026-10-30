package eu.coevolution.adapter.service.util;

import eu.coevolution.adapter.response.PagedApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtilService {

    public PagedApiResponse<?> constructPageDataResponse(Page<?> pagedUsers, List<?> dto) {
        return new PagedApiResponse<>(dto, pagedUsers.getNumber(), pagedUsers.getSize(), pagedUsers.getTotalElements(),
                pagedUsers.getTotalPages(), pagedUsers.isLast());
    }

}

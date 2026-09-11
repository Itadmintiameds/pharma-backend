package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.GstDto;
import tiameds.pharmabackend.entity.master.Gst;
import tiameds.pharmabackend.repository.master.GstRepository;
import tiameds.pharmabackend.service.master.GstService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GstServiceImpl implements GstService {

    private final GstRepository gstRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GstDto> getAllGst() {
        return gstRepository
                .findAll(Sort.by("gstId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private GstDto toDto(Gst gst) {
        GstDto dto = new GstDto();

        dto.setGstId(gst.getGstId());
        dto.setGstPercentage(gst.getGstPercentage());
        dto.setIsActive(gst.getIsActive());

        return dto;
    }
}

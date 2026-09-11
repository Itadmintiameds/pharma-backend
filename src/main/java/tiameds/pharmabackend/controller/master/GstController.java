package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.GstDto;
import tiameds.pharmabackend.service.master.GstService;

import java.util.List;

@RestController
@RequestMapping("/gst")
@RequiredArgsConstructor
public class GstController {

    private final GstService gstService;

    @GetMapping("/getAll")
    public ResponseEntity<List<GstDto>> getAllGst() {
        return ResponseEntity.ok(gstService.getAllGst());
    }
}

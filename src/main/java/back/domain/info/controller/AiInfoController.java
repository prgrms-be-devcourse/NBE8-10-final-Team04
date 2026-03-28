package back.domain.info.controller;

import back.domain.info.service.AiInfoServiceImpl;
import back.domain.info.service.StatServiceImpl;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/info/")
public class AiInfoController {
    private final AiInfoServiceImpl inforService;
    private final StatServiceImpl statService;

    @PostMapping("/run")
    public ResponseEntity<RsData<Void>> run() throws InterruptedException {
        inforService.run();
        statService.run();

        return ResponseEntity.ok(new RsData<>("데이터 적재 완료"));
    }
}

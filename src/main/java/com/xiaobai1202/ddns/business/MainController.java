package com.xiaobai1202.ddns.business;

import com.aliyuncs.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @Autowired
    private MainService mainService;

    @RequestMapping("/")
    public ResponseEntity<String> index() {
        return new ResponseEntity<>("服务运行正常！", HttpStatus.OK);
    }

    @RequestMapping("/update")
    public ResponseEntity<?> update() throws ClientException {
        return mainService.updateIpDNS();
    }
}

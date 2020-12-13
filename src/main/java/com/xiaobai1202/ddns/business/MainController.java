package com.xiaobai1202.ddns.business;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MainController {

    @Autowired
    private IAcsClient client;

    @Autowired
    private MainService mainService;

    @RequestMapping("/")
    public ResponseEntity<String> indedx() throws ClientException, IOException {
        String log = mainService.logs();
        return new ResponseEntity(log, HttpStatus.OK);
    }

    @RequestMapping("/update")
    public ResponseEntity update() throws ClientException {
        return  mainService.updateIpDNS();
    }
}

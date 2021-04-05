package com.xiaobai1202.ddns.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MainController {

    @Autowired
    private MainService mainService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<String> index() {
        return new ResponseEntity<>("服务运行正常！", HttpStatus.OK);
    }

    @RequestMapping(value = "/update", method = RequestMethod.GET)
    public ResponseEntity<String> updateByGet() {
        String s = mainService.updateIpAddress();
        return new ResponseEntity<>(s, HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public ResponseEntity<String> updateByPost() {
        String s = mainService.updateIpAddress();
        return new ResponseEntity<>(s, HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/log", method = RequestMethod.GET, produces = {"text/plain;charset=UTF-8"})
    public ResponseEntity<Resource> getServerLogs() throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        Resource logs = mainService.getLogs(httpHeaders);
        return new ResponseEntity<>(logs, httpHeaders, HttpStatus.OK);
    }
}

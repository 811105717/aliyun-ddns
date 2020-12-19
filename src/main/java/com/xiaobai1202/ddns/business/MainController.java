package com.xiaobai1202.ddns.business;

import com.aliyuncs.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class MainController {

    @Autowired
    private MainService mainService;

    @RequestMapping("/")
    public void index(HttpServletResponse response) throws ClientException, IOException {
        String log = mainService.logs();
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("text/html;charset=utf8");
        response.setContentLength(log.length());
        response.getWriter().println(log);
        response.flushBuffer();
    }

    @RequestMapping("/update")
    public ResponseEntity update() throws ClientException {
        return  mainService.updateIpDNS();
    }
}

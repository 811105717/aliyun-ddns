package com.xiaobai1202.ddns.business;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.exceptions.ClientException;
import com.xiaobai1202.ddns.config.ConfigProperties;
import com.xiaobai1202.email.generated.client.template.EmailApi;
import com.xiaobai1202.email.generated.model.EmailTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class MainService {

    private RestTemplate restTemplate;
    private IAcsClient alClient;
    private ConfigProperties properties;
    private EmailApi emailService;


    public MainService(@Qualifier("restTemplate") RestTemplate restTemplate,
                       IAcsClient client,
                       ConfigProperties properties,
                       EmailApi emailService) {
        this.restTemplate = restTemplate;
        this.alClient = client;
        this.properties = properties;
        this.emailService = emailService;
    }

    @Value("${ddns.client.getIpURL:https://api.ip.sb/ip}")
    private String getIpURL;

    public String getLocalIp() {
        String ipAddress = restTemplate.getForObject(getIpURL, String.class);
        return ipAddress.trim().toLowerCase();
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public ResponseEntity<String> updateIpDNS() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            responseEntity = getStringResponseEntity();
        } catch (Exception e) {
            EmailTo emailTo = new EmailTo();
            emailTo.setContent(String.format("ddns 执行发生错误！ \n %s", e.toString()));
            emailTo.setTittle("DDNS 服务发生不可挽回错误！");
            emailTo.setTo(Arrays.asList("811105717@qq.com"));
            emailService.sendNewEmail("zh-CN", emailTo);
        }
        return responseEntity;
    }

    @Async
    ResponseEntity<String> getStringResponseEntity() {
        String s = updateIpAddress();
        return new ResponseEntity<>(s, HttpStatus.ACCEPTED);
    }

    @Async
    void sendEmail(String oldIp, String newIp) {
        EmailTo emailTo = new EmailTo();
        StringBuilder sb = new StringBuilder();
        sb.append("您好！\n ")
                .append("您的本地AliYunDDNS服务对您的服务器ip进行了更新！\n")
                .append("原ip为: ")
                .append(oldIp)
                .append(" ,更新后的IP为： ")
                .append(newIp)
                .append("\n")
                .append("IP地址解析的TTL为10分钟，解析可能稍有延迟！\n")
                .append("如果您急需访问服务器，可以通过IP地址[")
                .append(newIp)
                .append("]进行访问！\n")
                .append("谢谢！");
        emailTo.setContent(sb.toString());
        emailTo.setTo(Collections.singletonList("811105717@qq.com"));
        emailTo.setBcc(Collections.singletonList("xiaobai@xiaobai1202.com"));
        emailTo.setTittle("AliYun DDNS Service 地址变更通知！");
        emailService.sendNewEmail("zh-CN", emailTo);
    }

    String updateIpAddress() {
        String currentIpAddress = null;
        try {
            String ipAddress = restTemplate.getForObject(getIpURL, String.class);
            if (StringUtils.isNotEmpty(ipAddress)) {
                currentIpAddress = ipAddress.trim().toLowerCase(Locale.ROOT);
            }
        } catch (RestClientException exception) {
            log.error("error occurs when get current ip address, the message -> {}", exception.getMessage());
            return "error occurs when get current ip address";
        }

        DescribeDomainRecordsRequest recordsRequest = new DescribeDomainRecordsRequest();
        recordsRequest.setDomainName(properties.getDomain());
        recordsRequest.setRRKeyWord(properties.getRr());
        recordsRequest.setType(properties.getRecordType());
        DescribeDomainRecordsResponse listResponse = null;
        try {
            listResponse = alClient.getAcsResponse(recordsRequest);
        } catch (ClientException e) {
            log.error("error occurs when call alibaba ddns service => {} " + e.getMessage());
            return "error occurs when call alibaba ddns service";
        }

        if (Objects.nonNull(listResponse) && CollectionUtils.isNotEmpty(listResponse.getDomainRecords())) {
            DescribeDomainRecordsResponse.Record record = listResponse.getDomainRecords().get(0);
            if (record.getValue().equals(currentIpAddress)) {
                String info = "aliyun record value:[" + record.getValue() + "] ,current local record value:[" + currentIpAddress + "] " +
                        ", no update action.";
                log.warn(info);
                return info;
            } else {
                UpdateDomainRecordRequest updateDomainRequest = new UpdateDomainRecordRequest();
                updateDomainRequest.setRR(properties.getRr());
                updateDomainRequest.setRecordId(record.getRecordId());
                updateDomainRequest.setValue(currentIpAddress);
                updateDomainRequest.setType(properties.getRecordType());
                try {
                    alClient.getAcsResponse(updateDomainRequest);
                    String info = "***aliyun record value:[" + record.getValue() + "] current local resolve value:["
                            + currentIpAddress + "] do update action!！！！！***";
                    log.info(info);
                    sendEmail(record.getValue(), currentIpAddress);
                    return info;
                } catch (ClientException e) {
                    log.error("error occurs when call alibaba ddns service: " + e);
                    return "error occurs when call alibaba ddns service";
                }
            }
        } else {
            return "can not found target record! does not change.";
        }
    }

    Resource getLogs(HttpHeaders httpHeaders) throws IOException {
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        httpHeaders.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
        httpHeaders.setContentLanguage(Locale.SIMPLIFIED_CHINESE);
        File file = new File("./ip-refresh.log");
        if (file.exists()) {
            Resource resource = new FileSystemResource(file);
            httpHeaders.setContentLength(resource.contentLength());
            return resource;
        } else {
            Resource resource = new ByteArrayResource("can not found log file!".getBytes());
            httpHeaders.setContentLength(resource.contentLength());
            return resource;
        }
    }

}

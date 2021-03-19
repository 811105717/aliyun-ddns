package com.xiaobai1202.ddns.business;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.exceptions.ClientException;
import com.xiaobai1202.ddns.config.ConfigProperties;
import com.xiaobai1202.ddns.model.IpAddress;
import com.xiaobai1202.email.generated.client.template.EmailApi;
import com.xiaobai1202.email.generated.model.EmailTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

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

    @Value("${ddns.client.getIpURL:http://ip.taobao.com/outGetIpInfo?ip=myip&accessKey=alibaba-inc}")
    private String getIpURL;

    public String getLocalIp() {
        IpAddress ipAddress = restTemplate.getForObject(getIpURL, IpAddress.class);
        return ipAddress.getData().getIp();
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public ResponseEntity<String> updateIpDNS() {
        String localIp = this.getLocalIp();
        if (StringUtils.isEmpty(localIp)) {
            throw new RuntimeException("无法获取正确的本地IP");
        }
        DescribeDomainRecordsRequest recordsRequest = new DescribeDomainRecordsRequest();
        recordsRequest.setDomainName(properties.getDomain());
        recordsRequest.setRRKeyWord(properties.getRr());
        recordsRequest.setType(properties.getRecordType());
        DescribeDomainRecordsResponse listResponse = null;
        try {
            listResponse = alClient.getAcsResponse(recordsRequest);
        } catch (ClientException e) {
            log.error("call client error: " + e);
        }
        if (listResponse.getDomainRecords().size() > 0) {
            DescribeDomainRecordsResponse.Record record = listResponse.getDomainRecords().get(0);
            if (record.getValue().equals(localIp)) {
                String info = "域名解析值:[" + record.getValue() + "] 当前本地IP地址为:[" + localIp + "] 不进行操作！";
                log.warn(info);
                return new ResponseEntity<>(info, HttpStatus.ACCEPTED);
            } else {
                UpdateDomainRecordRequest updateDomainRequest = new UpdateDomainRecordRequest();
                updateDomainRequest.setRR(properties.getRr());
                updateDomainRequest.setRecordId(record.getRecordId());
                updateDomainRequest.setValue(localIp);
                updateDomainRequest.setType(properties.getRecordType());
                try {
                    alClient.getAcsResponse(updateDomainRequest);
                } catch (ClientException e) {
                    log.error("update ip dns error: " + e);
                }
                String info = "***域名解析值:[" + record.getValue() + "] 当前本地IP地址为:[" + localIp + "] 进行更新！！！！***";
                log.info(info);
                sendEmail(record.getValue(), localIp);
                return new ResponseEntity<>(info, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>("未找到解析记录->不进行操作", HttpStatus.OK);
        }

    }

    @Async
    protected void sendEmail(String oldIp, String newIp) {
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

}

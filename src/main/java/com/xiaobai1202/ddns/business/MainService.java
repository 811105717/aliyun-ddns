package com.xiaobai1202.ddns.business;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.xiaobai1202.ddns.config.ConfigProperties;
import com.xiaobai1202.ddns.model.IpAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class MainService {

    private RestTemplate restTemplate;
    private IAcsClient alClient;
    private ConfigProperties properties;


    public MainService(RestTemplate template, IAcsClient client, ConfigProperties properties) {
        this.restTemplate = template;
        this.alClient = client;
        this.properties = properties;
    }

    @Value("${ddns.client.getIpURL:http://ip.taobao.com/outGetIpInfo?ip=myip&accessKey=alibaba-inc}")
    private String getIpURL;

    public String getLocalIp() {
        IpAddress ipAddress = restTemplate.getForObject(getIpURL, IpAddress.class);
        return ipAddress.getData().getIp();
    }

    @Async
    @Scheduled(cron = "0 0/10 * * * ?")
    public ResponseEntity updateIpDNS() throws ClientException {
        String localIp = this.getLocalIp();
        if (StringUtils.isEmpty(localIp)) {
            throw new RuntimeException("无法获取正确的本地IP");
        }
        DescribeDomainRecordsRequest recordsRequest = new DescribeDomainRecordsRequest();
        recordsRequest.setDomainName(properties.getDomain());
        recordsRequest.setRRKeyWord(properties.getRr());
        recordsRequest.setType(properties.getRecordType());
        DescribeDomainRecordsResponse listResponse = alClient.getAcsResponse(recordsRequest);
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
                UpdateDomainRecordResponse updateResponse = alClient.getAcsResponse(updateDomainRequest);
                String info = "***域名解析值:[" + record.getValue() + "] 当前本地IP地址为:[" + localIp + "] 进行更新！！！！***";
                log.info(info);
                return new ResponseEntity<>(updateResponse, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>("未找到解析记录->不进行操作", HttpStatus.OK);
        }

    }

}

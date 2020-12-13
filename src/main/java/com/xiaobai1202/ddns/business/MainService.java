package com.xiaobai1202.ddns.business;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.google.common.base.Charsets;
import com.xiaobai1202.ddns.config.ConfigProperties;
import com.xiaobai1202.ddns.model.IpAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;

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
                String info = "域名解析值:[" + record.getValue() + "]\n"
                        + "当前本地IP地址为:[" + localIp + "]\n" +
                        "不进行操作！";
                log.warn(info);
                try {
                    writeChangeLog(info);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return new ResponseEntity(info, HttpStatus.ACCEPTED);
            } else {
                UpdateDomainRecordRequest updateDomainRequest = new UpdateDomainRecordRequest();
                updateDomainRequest.setRR(properties.getRr());
                updateDomainRequest.setRecordId(record.getRecordId());
                updateDomainRequest.setValue(localIp);
                updateDomainRequest.setType(properties.getRecordType());
                UpdateDomainRecordResponse updateResponse = alClient.getAcsResponse(updateDomainRequest);
                String info = "***域名解析值:[" + record.getValue() + "]\n"
                        + "当前本地IP地址为:[" + localIp + "]\n" +
                        "进行更新！！！！***";
                try {
                    writeChangeLog(info);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info(info);
                return new ResponseEntity(updateResponse, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity("未找到解析记录->不进行操作", HttpStatus.OK);
        }

    }

    public String logs() throws IOException {
        File file = new File("change-log.log");
        InputStream inputStream = new FileInputStream(file);
        byte[] bytes = new byte[1024 * 4];
        StringBuffer buffer = new StringBuffer();
        while (inputStream.read(bytes) > 0) {
            buffer.append(new String(bytes, Charsets.UTF_8));
        }
        String str = buffer.toString().replace("\n", "<br>");
        return str;
    }

    private void writeChangeLog(String info) throws IOException {
        File file = new File("change-log.log");
        FileOutputStream outputStream = new FileOutputStream(file, true);
        OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter writer = new BufferedWriter(streamWriter);
        writer.write("===================" + new Date().toString() + "========================\n");
        writer.write(info);
        writer.newLine();
        writer.write("==================================================\n\n");
        writer.close();
        streamWriter.close();
        outputStream.close();
    }

}

package com.atguigu.gmall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;

public class ElasticSearchConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient(){
            return  new RestHighLevelClient(RestClient.builder(new HttpHost("192.168.71.129",9200,"http")));
    }
}

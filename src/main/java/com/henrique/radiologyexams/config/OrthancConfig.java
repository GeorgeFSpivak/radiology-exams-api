package com.henrique.radiologyexams.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OrthancProperties.class)
public class OrthancConfig {

    @Bean
    public RestClient orthancRestClient(
            RestClient.Builder builder,
            OrthancProperties properties
    ){
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());

        var requestFactory = ClientHttpRequestFactoryBuilder.jdk().build(settings);

        return builder.baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(
                        properties.username(),
                        properties.password()
                ))
                .requestFactory(requestFactory)
                .build();
    }
}

package com.asg.rocketflyiii;

import com.asg.rocketflyiii.config.FireflyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FireflyProperties.class)
public class RocketflyiiiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RocketflyiiiApplication.class, args);
    }
}

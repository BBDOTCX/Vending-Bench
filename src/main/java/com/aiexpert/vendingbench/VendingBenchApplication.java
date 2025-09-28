package com.aiexpert.vendingbench;

import com.aiexpert.vendingbench.config.SimulationDefaults;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SimulationDefaults.class)
public class VendingBenchApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendingBenchApplication.class, args);
    }
}
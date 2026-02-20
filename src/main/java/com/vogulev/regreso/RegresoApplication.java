package com.vogulev.regreso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RegresoApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(RegresoApplication.class, args);
    }

}

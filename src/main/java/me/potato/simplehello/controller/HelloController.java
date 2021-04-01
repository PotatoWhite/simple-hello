package me.potato.simplehello.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class HelloController {

    @Value("${app.resources.greeting}")
    private String greetingMessage;

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hello() throws UnknownHostException {
        String ip   = InetAddress.getLocalHost().getHostAddress();
        String name = InetAddress.getLocalHost().getHostName();

        return greetingMessage + " {" + name + " " + ip + "}";
    }
}

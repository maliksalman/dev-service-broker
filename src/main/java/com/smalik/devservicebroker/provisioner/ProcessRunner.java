package com.smalik.devservicebroker.provisioner;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.awt.desktop.AboutEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@Service
public class ProcessRunner {
 
  public int runProcess(String... args) throws Exception {

    log.debug("Running: [[" + StringUtils.join(args, ' ') + "]]");

    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);
    builder.directory(new File(System.getProperty("user.home")));
    
    Process process = builder.start();
    StreamGobbler streamGobbler = 
      new StreamGobbler(process.getInputStream(), System.out::println);
    Executors.newSingleThreadExecutor().submit(streamGobbler);
    return process.waitFor();
  }

  private static class StreamGobbler implements Runnable {
    private InputStream inputStream;
    private Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        new BufferedReader(new InputStreamReader(inputStream))
          .lines()
          .forEach(consumer);
    }
  }
}
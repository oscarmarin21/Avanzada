package com.avanzada.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;


/**
 * When the application is started with {@code --init-data}, runs the data initializer and then exits.
 * Use: {@code mvn spring-boot:run -Dspring-boot.run.arguments=--init-data} or
 * {@code java -jar backend.jar --init-data}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitDataRunner implements ApplicationRunner, Ordered {

    private static final String INIT_DATA_ARG = "init-data";

    private final DataInitializer dataInitializer;

    @Override
    public void run(ApplicationArguments args) {
        if (args.getOptionNames().contains(INIT_DATA_ARG)
                || (!ObjectUtils.isEmpty(args.getNonOptionArgs()) && args.getNonOptionArgs().contains(INIT_DATA_ARG))) {
            runInitAndExit();
        }
    }

    private void runInitAndExit() {
        log.info("Running data initialization (--init-data)...");
        dataInitializer.run();
        log.info("Exiting after init-data.");
        System.exit(0);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

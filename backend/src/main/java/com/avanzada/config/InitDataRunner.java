package com.avanzada.config;

import com.avanzada.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;


/**
 * Runs the data initializer in two modes:
 * <ul>
 *   <li>{@code --init-data} argument: seeds data then exits the process.</li>
 *   <li>Auto-init: if the state table is empty (fresh database), seeds data and continues normally.
 *       Skipped during tests to avoid conflicts with test fixtures.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitDataRunner implements ApplicationRunner, Ordered {

    private static final String INIT_DATA_ARG = "init-data";

    private final DataInitializer dataInitializer;
    private final StateRepository stateRepository;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        boolean explicit = args.getOptionNames().contains(INIT_DATA_ARG)
                || (!ObjectUtils.isEmpty(args.getNonOptionArgs()) && args.getNonOptionArgs().contains(INIT_DATA_ARG));

        if (explicit) {
            log.info("Running data initialization (--init-data)...");
            dataInitializer.run();
            log.info("Exiting after init-data.");
            System.exit(0);
        } else if (isTestEnvironment()) {
            log.debug("Skipping auto-init in test environment.");
        } else if (stateRepository.count() == 0) {
            log.info("Empty database detected, running auto-initialization...");
            dataInitializer.run();
        }
    }

    private boolean isTestEnvironment() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

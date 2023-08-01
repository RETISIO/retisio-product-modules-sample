package com.retisio.arc.module;

import com.google.inject.AbstractModule;
import com.retisio.arc.execution.ServiceExecutionContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonModule extends AbstractModule {

    @Override
    protected void configure() {
        log.info(" .... configure");
        bind(ServiceExecutionContext.class).asEagerSingleton();
        log.info("Depence injection is configured ....");
    }

}

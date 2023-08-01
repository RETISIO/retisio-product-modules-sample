package com.retisio.arc.ext.module;

import com.google.inject.AbstractModule;
import com.retisio.arc.ext.controller.ExtCatalogServiceController;
import com.retisio.arc.ext.service.ExtCatalogService;
import com.retisio.arc.ext.service.ExtCatalogServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExtCatalogModule extends AbstractModule {

    @Override
    protected void configure() {
        log.info(" .... configure");
        bind(ExtCatalogServiceController.class).asEagerSingleton();
        bind(ExtCatalogService.class).to(ExtCatalogServiceImpl.class).asEagerSingleton();
    }

}

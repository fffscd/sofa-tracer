/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.tracer.boot.compatible;

import com.alipay.common.tracer.core.configuration.SofaTracerConfiguration;
import com.alipay.common.tracer.core.utils.StringUtils;
import com.alipay.sofa.tracer.boot.properties.SofaTracerProperties;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2020/2/21 12:25 AM
 * @since:
 **/
public class SpringBootV1Config {

    public static void configSpringBootOnV1(ConfigurableEnvironment environment) {
        // set loggingPath
        String loggingPath = environment.getProperty("logging.path");
        if (StringUtils.isNotBlank(loggingPath)) {
            System.setProperty("logging.path", loggingPath);
        }

        // check spring.application.name
        String applicationName = environment
            .getProperty(SofaTracerConfiguration.TRACER_APPNAME_KEY);
        Assert.isTrue(!StringUtils.isBlank(applicationName),
            SofaTracerConfiguration.TRACER_APPNAME_KEY + " must be configured!");
        SofaTracerConfiguration.setProperty(SofaTracerConfiguration.TRACER_APPNAME_KEY,
            applicationName);

        SofaTracerProperties tempTarget = new SofaTracerProperties();
        PropertiesConfigurationFactory<SofaTracerProperties> binder = new PropertiesConfigurationFactory<SofaTracerProperties>(
            tempTarget);
        ConfigurationProperties configurationPropertiesAnnotation = getConfigurationPropertiesAnnotation(tempTarget);
        if (configurationPropertiesAnnotation != null
            && StringUtils.isNotBlank(configurationPropertiesAnnotation.prefix())) {
            //consider compatible Spring Boot 1.5.X and 2.x
            binder.setIgnoreInvalidFields(configurationPropertiesAnnotation.ignoreInvalidFields());
            binder.setIgnoreUnknownFields(configurationPropertiesAnnotation.ignoreUnknownFields());
            binder.setTargetName(configurationPropertiesAnnotation.prefix());
        } else {
            binder.setTargetName(SofaTracerProperties.SOFA_TRACER_CONFIGURATION_PREFIX);
        }
        binder.setConversionService(new DefaultConversionService());
        binder.setPropertySources(environment.getPropertySources());
        try {
            binder.bindPropertiesToTarget();
        } catch (BindException ex) {
            throw new IllegalStateException("Cannot bind to SofaTracerProperties", ex);
        }

        //properties convert to tracer
        SofaTracerConfiguration.setProperty(
            SofaTracerConfiguration.DISABLE_MIDDLEWARE_DIGEST_LOG_KEY,
            tempTarget.getDisableDigestLog());
        SofaTracerConfiguration.setProperty(SofaTracerConfiguration.DISABLE_DIGEST_LOG_KEY,
            tempTarget.getDisableConfiguration());
        SofaTracerConfiguration.setProperty(SofaTracerConfiguration.TRACER_GLOBAL_ROLLING_KEY,
            tempTarget.getTracerGlobalRollingPolicy());
        SofaTracerConfiguration.setProperty(SofaTracerConfiguration.TRACER_GLOBAL_LOG_RESERVE_DAY,
            tempTarget.getTracerGlobalLogReserveDay());
        //stat log interval
        SofaTracerConfiguration.setProperty(SofaTracerConfiguration.STAT_LOG_INTERVAL,
            tempTarget.getStatLogInterval());
        //baggage length
        SofaTracerConfiguration.setProperty(
            SofaTracerConfiguration.TRACER_PENETRATE_ATTRIBUTE_MAX_LENGTH,
            tempTarget.getBaggageMaxLength());
        SofaTracerConfiguration.setProperty(
            SofaTracerConfiguration.TRACER_SYSTEM_PENETRATE_ATTRIBUTE_MAX_LENGTH,
            tempTarget.getBaggageMaxLength());

        //sampler config
        if (tempTarget.getSamplerName() != null) {
            SofaTracerConfiguration.setProperty(SofaTracerConfiguration.SAMPLER_STRATEGY_NAME_KEY,
                tempTarget.getSamplerName());
        }
        if (StringUtils.isNotBlank(tempTarget.getSamplerCustomRuleClassName())) {
            SofaTracerConfiguration.setProperty(
                SofaTracerConfiguration.SAMPLER_STRATEGY_CUSTOM_RULE_CLASS_NAME,
                tempTarget.getSamplerCustomRuleClassName());
        }
        SofaTracerConfiguration.setProperty(
            SofaTracerConfiguration.SAMPLER_STRATEGY_PERCENTAGE_KEY,
            String.valueOf(tempTarget.getSamplerPercentage()));

        SofaTracerConfiguration.setProperty(SofaTracerConfiguration.JSON_FORMAT_OUTPUT,
            String.valueOf(tempTarget.isJsonOutput()));

    }

    private static ConfigurationProperties getConfigurationPropertiesAnnotation(Object targetObject) {
        return AnnotationUtils.findAnnotation(targetObject.getClass(),
            ConfigurationProperties.class);
    }
}
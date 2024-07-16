package io.github.nickid2018.atribot.core.communicate;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CommunicateFilters.class)
public @interface CommunicateFilter {
    String key();

    String value();
}

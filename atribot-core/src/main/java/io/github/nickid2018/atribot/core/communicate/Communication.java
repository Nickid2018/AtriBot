package io.github.nickid2018.atribot.core.communicate;

import com.google.common.base.Predicates;
import io.github.nickid2018.atribot.core.plugin.PluginContainer;
import io.github.nickid2018.atribot.core.plugin.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Slf4j
public class Communication {

    private static Set<Triple<CommunicateReceiver, Method, Communicate>> getAvailableCallables(
        String key, Predicate<String> pluginFilter, Predicate<CommunicateFilter[]> methodFilter, Object... obj
    ) {
        Set<Triple<CommunicateReceiver, Method, Communicate>> availableCallables = new TreeSet<>(
            Comparator.comparingInt(o -> o.getRight().priority())
        );
        PluginManager.forEachPluginNames(plugin -> {
            if (!pluginFilter.test(plugin))
                return;
            PluginContainer container = PluginManager.getPlugin(plugin);
            CommunicateReceiver receiver = container.pluginInstance().getCommunicateReceiver();
            Arrays
                .stream(receiver.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Communicate.class))
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> {
                    if (obj == null)
                        return method.getParameterCount() == 0;
                    Class<?>[] clazzRequired = Arrays
                        .stream(obj)
                        .map(o -> o == null ? Object.class : o.getClass())
                        .toArray(Class[]::new);
                    Class<?>[] clazzMethod = method.getParameterTypes();
                    if (clazzRequired.length != clazzMethod.length)
                        return false;
                    for (int i = 0; i < clazzRequired.length; i++) {
                        if (!clazzMethod[i].isAssignableFrom(clazzRequired[i]))
                            return false;
                    }
                    return true;
                })
                .filter(method -> {
                    if (method.isAnnotationPresent(CommunicateFilters.class)) {
                        return methodFilter.test(method.getAnnotation(CommunicateFilters.class).value());
                    } else if (method.isAnnotationPresent(CommunicateFilter.class)) {
                        return methodFilter.test(new CommunicateFilter[]{
                            method.getAnnotation(CommunicateFilter.class)
                        });
                    } else {
                        return methodFilter.test(new CommunicateFilter[0]);
                    }
                })
                .forEach(method -> {
                    Communicate communicate = method.getAnnotation(Communicate.class);
                    if (communicate.value().equals(key))
                        availableCallables.add(Triple.of(receiver, method, communicate));
                });
        });
        return availableCallables;
    }

    public static void communicate(String key, Object... obj) {
        communicate(key, Predicates.alwaysTrue(), Predicates.alwaysTrue(), obj);
    }

    public static void communicate(String key, Predicate<String> pluginFilter, Predicate<CommunicateFilter[]> methodFilter, Object... obj) {
        Set<Triple<CommunicateReceiver, Method, Communicate>> availableCallables = getAvailableCallables(
            key,
            pluginFilter,
            methodFilter,
            obj
        );
        Iterator<Triple<CommunicateReceiver, Method, Communicate>> iterator = availableCallables.iterator();
        DiscardableCommunicateData[] discardables = obj == null ? new DiscardableCommunicateData[0] : Arrays
            .stream(obj)
            .filter(DiscardableCommunicateData.class::isInstance)
            .map(DiscardableCommunicateData.class::cast)
            .toArray(DiscardableCommunicateData[]::new);
        while (iterator.hasNext()) {
            for (DiscardableCommunicateData discardable : discardables)
                if (discardable.isDiscarded())
                    break;
            Triple<CommunicateReceiver, Method, Communicate> triple = iterator.next();
            try {
                triple.getMiddle().invoke(triple.getLeft(), obj);
            } catch (Throwable e) {
                log.error("Failed to call method: {}", triple.getMiddle().getName(), e);
            }
            iterator.remove();
        }
        availableCallables.clear();
    }

    public static <D> CompletableFuture<D> communicateWithResult(String plugin, String key, Object... obj) {
        return communicateWithResult(plugin, key, Predicates.alwaysTrue(), obj);
    }

    @SuppressWarnings("unchecked")
    public static <D> CompletableFuture<D> communicateWithResult(String plugin, String key, Predicate<CommunicateFilter[]> methodFilter, Object... obj) {
        Set<Triple<CommunicateReceiver, Method, Communicate>> availableCallables = getAvailableCallables(
            key,
            Predicate.isEqual(plugin),
            methodFilter,
            obj
        );
        Triple<CommunicateReceiver, Method, Communicate> triple = availableCallables
            .stream()
            .findFirst()
            .orElse(null);
        if (triple == null)
            return CompletableFuture.failedFuture(new IllegalStateException("No method found"));
        try {
            Object result = triple.getMiddle().invoke(triple.getLeft(), obj);
            if (result instanceof CompletableFuture)
                return (CompletableFuture<D>) result;
            else
                return CompletableFuture.completedFuture((D) result);
        } catch (Throwable e) {
            log.error("Failed to call method: {}", triple.getMiddle().getName(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

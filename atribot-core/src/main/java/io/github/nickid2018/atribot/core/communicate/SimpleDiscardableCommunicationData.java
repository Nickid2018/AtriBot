package io.github.nickid2018.atribot.core.communicate;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SimpleDiscardableCommunicationData<T> extends DiscardableCommunicateData {

    public T data;

    public static <T> SimpleDiscardableCommunicationData<T> of(T data) {
        return new SimpleDiscardableCommunicationData<>(data);
    }
}

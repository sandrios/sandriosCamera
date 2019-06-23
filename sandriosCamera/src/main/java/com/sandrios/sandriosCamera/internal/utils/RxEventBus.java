package com.sandrios.sandriosCamera.internal.utils;

import androidx.annotation.RestrictTo;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * @author Arpit Gandhi
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RxEventBus {

    private PublishSubject<Object> bus = PublishSubject.create();

    RxEventBus() {
    }

    public void complete() {
        bus.onComplete();
    }

    public void send(Object o) {
        bus.onNext(o);
    }

    public Observable<Object> toObserverable() {
        return bus;
    }

    public boolean hasObservers() {
        return bus.hasObservers();
    }
}

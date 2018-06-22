package com.intact.rx.templates.api;

public interface ObservableFluent<Subject, Observer> {
    Subject connect(Observer observer);

    Subject disconnect(Observer observer);

    Subject disconnectAll();
}

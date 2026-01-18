package ru.intelinfo.inczilla.mvc.service;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}